package io.github.biglv666.webcommon.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * traceId 默认关闭态的契约测试：不启用 trace-id 时，
 * 响应体不出现 traceId 字段、不写 X-Trace-Id 响应头，
 * 0.3.0 的响应包络保持字节级不变。
 */
@SpringBootTest(properties = "web-common.trace-id.enabled=false")
@AutoConfigureMockMvc
class TraceIdDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 关闭态：成功响应无 traceId 字段、无响应头。
     */
    @Test
    void successResponseOmitsTraceIdWhenDisabled() throws Exception {
        mockMvc.perform(get("/demo/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").doesNotExist())
                .andExpect(header().doesNotExist("X-Trace-Id"));
    }

    /**
     * 关闭态：失败响应同样无 traceId 字段。
     */
    @Test
    void failureResponseOmitsTraceIdWhenDisabled() throws Exception {
        mockMvc.perform(get("/demo/biz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40900))
                .andExpect(jsonPath("$.traceId").doesNotExist());
    }
}
