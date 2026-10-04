package io.github.biglv666.webcommon.test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.github.biglv666.webcommon.fixture.MetricsTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Micrometer 异常指标的集成测试：失败响应按错误码打点，
 * counter 带 code 与 category（分段归类）tag。
 */
@SpringBootTest(properties = "web-common.error-codes=io.github.biglv666.webcommon.test.TestOrderErrorCode")
@AutoConfigureMockMvc
@Import(MetricsTestConfig.class)
class ExceptionMetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry registry;

    /**
     * 业务异常（5xxxx）归入 business 分类。
     */
    @Test
    void businessFailureCountedWithBusinessCategory() throws Exception {
        mockMvc.perform(get("/demo/custom-code")).andExpect(status().isOk());
        assertEquals(1.0, registry.get("web.common.exceptions")
                .tag("code", "51001").tag("category", "business").counter().count());
    }

    /**
     * 客户端侧错误（4xxxx）归入 param 分类，500 归入 system 分类。
     */
    @Test
    void paramAndSystemFailuresCountedWithCategories() throws Exception {
        mockMvc.perform(get("/demo/no-such-path")).andExpect(status().isOk());
        mockMvc.perform(get("/demo/unknown")).andExpect(status().isOk());
        assertNotNull(registry.get("web.common.exceptions")
                .tag("code", "40400").tag("category", "param").counter());
        assertNotNull(registry.get("web.common.exceptions")
                .tag("code", "500").tag("category", "system").counter());
    }

    /**
     * 成功请求不打点。
     */
    @Test
    void successIsNotCounted() throws Exception {
        mockMvc.perform(get("/demo/success")).andExpect(status().isOk());
        assertNull(registry.find("web.common.exceptions").counter());
    }
}
