package io.github.biglv666.webcommon.test;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 针对 starter 统一包络契约的 MockMvc 断言工具，供使用方的测试代码引用
 * （主 artifact 携带，测试期与 spring-test/json-path/hamcrest 一起出现在
 * test classpath 即可，三者均由 Spring Boot BOM 管理版本）：
 *
 * <pre>{@code
 * mockMvc.perform(get("/users/1"))
 *         .andExpect(status().isOk())
 *         .andDo(ResultMatchers.success());                       // code=0
 *
 * mockMvc.perform(get("/users/1").accept(MediaType.TEXT_PLAIN))
 *         .andDo(ResultMatchers.success("hello"));                // code=0 且 data 相等
 *
 * mockMvc.perform(get("/orders/1"))
 *         .andDo(ResultMatchers.error(51001, "库存不足"));          // code 与 message 片段
 *
 * mockMvc.perform(post("/users", badBody))
 *         .andDo(ResultMatchers.paramError("age", "正数"));         // 校验明细按字段断言
 *
 * mockMvc.perform(get("/users/1"))                                // trace-id 开启时
 *         .andDo(ResultMatchers.traceId());                       // 字段与 X-Trace-Id 头一致
 * }</pre>
 *
 * <p>注意本类断言的是响应体包络（code/message/data/traceId），HTTP 状态码
 * 断言（{@code status().isOk()} 等）仍由 MockMvc 原生工具负责。</p>
 */
public final class ResultMatchers {

    private static final int SUCCESS_CODE = 0;

    private static final int PARAM_ERROR_CODE = 40000;

    private ResultMatchers() {
    }

    /**
     * 断言成功包络：code=0。
     *
     * @return 可用于 {@code andDo} 的结果处理器
     */
    public static ResultHandler success() {
        return codeIs(SUCCESS_CODE)::match;
    }

    /**
     * 断言成功包络：code=0 且 data 与期望值相等（适用于简单类型；
     * 复杂对象请用 {@code jsonPath("$.data.xxx")} 细化断言）。
     *
     * @param expectedData 期望的 data 值
     * @return 可用于 {@code andDo} 的结果处理器
     */
    public static ResultHandler success(Object expectedData) {
        return result -> {
            codeIs(SUCCESS_CODE).match(result);
            jsonPath("$.data").value(expectedData).match(result);
        };
    }

    /**
     * 断言失败包络：code 与期望错误码一致。
     *
     * @param expectedCode 期望的错误码
     * @return 可用于 {@code andDo} 的结果处理器
     */
    public static ResultHandler error(int expectedCode) {
        return codeIs(expectedCode)::match;
    }

    /**
     * 断言失败包络：code 一致且 message 包含期望片段（整体文案可含动态内容，
     * 按片段断言更稳）。
     *
     * @param expectedCode    期望的错误码
     * @param messageFragment 期望 message 包含的片段
     * @return 可用于 {@code andDo} 的结果处理器
     */
    public static ResultHandler error(int expectedCode, String messageFragment) {
        return result -> {
            codeIs(expectedCode).match(result);
            jsonPath("$.message", Matchers.containsString(messageFragment)).match(result);
        };
    }

    /**
     * 断言参数校验失败包络：code=40000，且 data 明细列表中存在指定 field
     * 的条目、其 message 包含期望片段。
     *
     * @param field           期望出错的字段名
     * @param messageFragment 期望该字段 message 包含的片段
     * @return 可用于 {@code andDo} 的结果处理器
     */
    public static ResultHandler paramError(String field, String messageFragment) {
        return result -> {
            codeIs(PARAM_ERROR_CODE).match(result);
            jsonPath("$.data[?(@.field=='" + field + "')].message",
                    (Matcher<?>) Matchers.hasItem(Matchers.containsString(messageFragment))).match(result);
        };
    }

    /**
     * 断言响应 traceId 字段与 X-Trace-Id 响应头一致，
     * 需 {@code web-common.trace-id.enabled=true}。
     *
     * @return 可用于 {@code andDo} 的结果处理器
     */
    public static ResultHandler traceId() {
        return result -> {
            String bodyTraceId = JsonPath.read(result.getResponse().getContentAsString(), "$.traceId");
            MatcherAssert.assertThat("响应 traceId 应与 X-Trace-Id 响应头一致",
                    bodyTraceId, Matchers.equalTo(result.getResponse().getHeader("X-Trace-Id")));
        };
    }

    /**
     * 断言 code 字段等于期望值。
     *
     * @param expectedCode 期望的错误码
     * @return code 断言器
     */
    private static ResultMatcher codeIs(int expectedCode) {
        return jsonPath("$.code").value(expectedCode);
    }
}
