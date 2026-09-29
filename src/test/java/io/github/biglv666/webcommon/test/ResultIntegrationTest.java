package io.github.biglv666.webcommon.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 统一返回与全局异常处理的集成测试，覆盖四条典型路径与 0.2.0 新特性。
 *
 * <p>启动类 {@link TestApplication} 不在 starter 主包内，本测试通过即证明
 * AutoConfiguration 自动装配真实生效，而非依赖同包扫描。</p>
 */
@SpringBootTest(properties = "web-common.error-codes=io.github.biglv666.webcommon.test.TestOrderErrorCode")
@AutoConfigureMockMvc
@Import({ExternalAdviceFixture.GovernanceLikeAdvice.class, ExternalAdviceFixture.ThrowingController.class})
class ResultIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 成功路径：code=0、message 为默认成功文案、data 原样返回。
     */
    @Test
    void successReturnsCodeZeroWithData() throws Exception {
        mockMvc.perform(get("/demo/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").value("hello learncard"));
    }

    /**
     * 业务异常路径：code 取异常携带的错误码，message 为业务自定义文案；
     * 非校验类失败不携带 data（序列化时省略）。
     */
    @Test
    void businessExceptionReturnsConflictCodeWithCustomMessage() throws Exception {
        mockMvc.perform(get("/demo/biz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40900))
                .andExpect(jsonPath("$.message").value("用户名已被注册"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * 请求体类型不匹配路径：message 附带出错字段路径（Jackson InvalidFormatException
     * 提取），便于前端定位到具体字段，但不泄露目标类型与类名。
     */
    @Test
    void deserializeTypeMismatchReturnsFieldPath() throws Exception {
        mockMvc.perform(post("/demo/deserialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"张三\",\"age\":\"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message", containsString("字段 age")))
                .andExpect(jsonPath("$.message", containsString("类型不匹配")));
    }

    /**
     * 请求体格式错误路径：无法定位字段时回退固定文案。
     */
    @Test
    void deserializeMalformedJsonReturnsFixedMessage() throws Exception {
        mockMvc.perform(post("/demo/deserialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }

    /**
     * 请求体校验失败路径：code=40000，message 含字段名与原因明细，
     * 同时 data 携带结构化校验明细（field/message）供程序解析。
     */
    @Test
    void validationFailureReturnsParamErrorWithFieldDetail() throws Exception {
        mockMvc.perform(post("/demo/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"age\":-1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message", containsString("name: 姓名不能为空")))
                .andExpect(jsonPath("$.message", containsString("age: 年龄必须为正数")))
                .andExpect(jsonPath("$.data[?(@.field == 'name')].message", hasItem("姓名不能为空")))
                .andExpect(jsonPath("$.data[?(@.field == 'age')].message", hasItem("年龄必须为正数")));
    }

    /**
     * 单参数校验失败路径：@RequestParam 约束失败同样返回 40000，
     * data 携带参数名与原因的结构化明细。
     */
    @Test
    void singleParamValidationReturnsParamError() throws Exception {
        mockMvc.perform(get("/demo/param").param("page", "-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message", containsString("page: 页码必须为正数")))
                .andExpect(jsonPath("$.data[?(@.field == 'page')].message", hasItem("页码必须为正数")));
    }

    /**
     * 方法参数内置校验失败路径（类上无 @Validated）：默认 always-200 模式下
     * HTTP 状态保持 200，code=40000 且明细格式与单参数校验一致。
     */
    @Test
    void handlerMethodValidationReturnsParamError() throws Exception {
        mockMvc.perform(get("/demo/method-validation").param("page", "-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message", containsString("page: 页码必须为正数")));
    }

    /**
     * 上传超限路径：返回 40000 固定文案，默认 always-200 模式下 HTTP 状态保持 200。
     */
    @Test
    void maxUploadSizeReturnsParamError() throws Exception {
        mockMvc.perform(post("/demo/upload-oversize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("上传文件过大"));
    }

    /**
     * 路径不存在路径：Spring 6.1+ 默认抛 NoResourceFoundException，应返回 40400。
     */
    @Test
    void notFoundReturnsNotFoundCode() throws Exception {
        mockMvc.perform(get("/demo/not-exist-path"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    /**
     * 兜底路径：未识别异常返回 SYSTEM_ERROR，且响应体不泄露内部异常细节。
     */
    @Test
    void unknownExceptionReturnsSystemErrorWithoutLeak() throws Exception {
        mockMvc.perform(get("/demo/unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统繁忙，请稍后重试"))
                .andExpect(jsonPath("$.message", not(containsString("jdbc"))))
                .andExpect(jsonPath("$.message", not(containsString("数据库"))));
    }

    /**
     * 自动包装路径：Controller 返回裸业务对象，应被包装为 code=0 的 Result。
     */
    @Test
    void plainObjectIsAutoWrapped() throws Exception {
        mockMvc.perform(get("/demo/wrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.age").value(25));
    }

    /**
     * 自动包装豁免路径：@NoWrap 接口原样输出业务 JSON，无 code/message 壳。
     */
    @Test
    void noWrapEndpointReturnsRawBody() throws Exception {
        mockMvc.perform(get("/demo/nowrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("李四"))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    /**
     * 自动包装 String 返回路径：应输出合法 JSON 而非字符串字面量。
     */
    @Test
    void stringReturnIsAutoWrappedAsJson() throws Exception {
        mockMvc.perform(get("/demo/wrap-string"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("raw-str"));
    }

    /**
     * 模板抛错路径：BusinessException.of 的 {} 占位符被实参填充。
     */
    @Test
    void templateMessageIsFilled() throws Exception {
        mockMvc.perform(get("/demo/template"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50000))
                .andExpect(jsonPath("$.message").value("库存不足，剩余 3 件"));
    }

    /**
     * 声明式抛错路径：@DefaultErrorCode 注解的自定义异常按业务异常处理。
     */
    @Test
    void annotatedExceptionIsMappedToDeclaredCode() throws Exception {
        mockMvc.perform(get("/demo/annotated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50000))
                .andExpect(jsonPath("$.message").value("商品已售罄"));
    }

    /**
     * 业务自建错误码路径：声明在 web-common.error-codes 中的枚举正常参与处理。
     */
    @Test
    void customErrorCodeEnumWorks() throws Exception {
        mockMvc.perform(get("/demo/custom-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(51001))
                .andExpect(jsonPath("$.message").value("库存不足"));
    }

    /**
     * 第三方异常处理器共存路径：其他 starter 的 @ExceptionHandler 自定义结构
     * （非 200 状态码 + 自有字段）必须原样输出，不被自动包装破坏。
     */
    @Test
    void thirdPartyExceptionHandlerResponseIsNotWrapped() throws Exception {
        mockMvc.perform(get("/demo/governance"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("请求过于频繁"));
    }
}
