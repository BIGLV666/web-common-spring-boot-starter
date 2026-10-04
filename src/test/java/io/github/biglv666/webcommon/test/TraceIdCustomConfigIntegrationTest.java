package io.github.biglv666.webcommon.test;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * traceId 配置项绑定测试：自定义 MDC key 与响应头名生效，
 * 且默认响应头 X-Trace-Id 不再写出。
 */
@SpringBootTest(properties = {
        "web-common.trace-id.enabled=true",
        "web-common.trace-id.mdc-key=request-id",
        "web-common.trace-id.response-header=X-Request-Id"})
@AutoConfigureMockMvc
class TraceIdCustomConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 自定义配置：从 MDC 的 request-id 取值，写 X-Request-Id 头与响应 traceId 字段。
     */
    @Test
    void customMdcKeyAndHeaderTakeEffect() throws Exception {
        MDC.put("request-id", "custom-42");
        try {
            mockMvc.perform(get("/demo/success"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Request-Id", "custom-42"))
                    .andExpect(header().doesNotExist("X-Trace-Id"))
                    .andExpect(jsonPath("$.traceId").value("custom-42"));
        } finally {
            MDC.remove("request-id");
        }
    }
}
