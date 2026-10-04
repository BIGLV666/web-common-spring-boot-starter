package io.github.biglv666.webcommon.test;

import io.github.biglv666.webcommon.fixture.I18nTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * traceId 与 i18n 同时开启的组合测试：两个特性分别作用于
 * Result 的 traceId 与 message 字段，组合开启时互不干扰。
 */
@SpringBootTest(properties = {
        "web-common.trace-id.enabled=true",
        "web-common.error-codes=io.github.biglv666.webcommon.test.TestOrderErrorCode"})
@AutoConfigureMockMvc
@Import(I18nTestConfig.class)
class TraceIdI18nCombinedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 成功响应：i18n 成功文案与 traceId 字段同时存在。
     */
    @Test
    void successCarriesLocalizedMessageAndTraceId() throws Exception {
        mockMvc.perform(get("/demo/wrap").locale(Locale.US))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success (en)"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andDo(ResultMatchers.traceId());
    }

    /**
     * 失败响应：i18n 错误文案与 traceId 字段同时存在。
     */
    @Test
    void failureCarriesLocalizedMessageAndTraceId() throws Exception {
        mockMvc.perform(get("/demo/biz-default").locale(Locale.US))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("Resource not found (en)"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }
}
