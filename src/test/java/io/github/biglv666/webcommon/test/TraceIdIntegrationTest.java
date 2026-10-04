package io.github.biglv666.webcommon.test;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * traceId 功能（web-common.trace-id.enabled=true）的集成测试。
 *
 * <p>覆盖三条路径：无链路追踪时自动生成（响应头与响应字段一致、请求间互不相同）、
 * 已有链路追踪写入 MDC 时原样透传、失败响应同样携带 traceId。</p>
 */
@SpringBootTest(properties = "web-common.trace-id.enabled=true")
@AutoConfigureMockMvc
class TraceIdIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 自动生成路径：traceId 同时出现在响应头与响应字段且取值一致；
     * 多次请求各自生成新值，不复用。
     */
    @Test
    void generatesTraceIdConsistentBetweenHeaderAndBody() throws Exception {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/demo/success"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Trace-Id", not(emptyOrNullString())))
                    .andDo(result -> seen.add(result.getResponse().getHeader("X-Trace-Id")))
                    .andDo(ResultMatchers.traceId());
        }
        org.junit.jupiter.api.Assertions.assertEquals(2, seen.size(), "两次请求应生成不同的 traceId");
    }

    /**
     * 透传路径：MDC 已有 traceId（模拟使用方接入链路追踪组件）时原样使用，
     * 不覆盖不重生；MockMvc 与测试用例同线程，可直接预置 MDC。
     */
    @Test
    void reusesTraceIdAlreadyPresentInMdc() throws Exception {
        MDC.put("traceId", "preset-trace-001");
        try {
            mockMvc.perform(get("/demo/success"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Trace-Id", "preset-trace-001"))
                    .andExpect(jsonPath("$.traceId").value("preset-trace-001"));
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * 生命周期：过滤器自行生成的 traceId 在请求结束后必须清理，
     * 防容器线程池复用导致下一个请求串到上一个 traceId。
     */
    @Test
    void generatedTraceIdIsCleanedUpAfterRequest() throws Exception {
        mockMvc.perform(get("/demo/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        org.junit.jupiter.api.Assertions.assertNull(MDC.get("traceId"),
                "自行生成的 traceId 必须在请求结束后清理，防线程池复用串号");
    }

    /**
     * 失败路径：业务异常响应同样携带 traceId，前后端可按同一标识对账。
     */
    @Test
    void failureResponseCarriesTraceId() throws Exception {
        mockMvc.perform(get("/demo/biz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40900))
                .andDo(ResultMatchers.traceId());
    }
}
