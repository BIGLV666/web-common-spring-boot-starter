package io.github.biglv666.webcommon.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 错误码字典 OpenAPI 扩展（x-error-codes）的集成测试：
 * springdoc 在类路径时，/v3/api-docs 根节点携带全量错误码字典
 * （内置码 + 业务声明码），与字典端点同源。
 */
@SpringBootTest(properties = "web-common.error-codes=io.github.biglv666.webcommon.test.TestOrderErrorCode")
@AutoConfigureMockMvc
class ErrorCodeOpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * x-error-codes 为数组且包含内置码与业务码条目。
     */
    @Test
    void openApiRootCarriesErrorCodesExtension() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$['x-error-codes']").isArray())
                .andExpect(jsonPath("$['x-error-codes'][?(@.code == 40400)].constant").value("NOT_FOUND"))
                .andExpect(jsonPath("$['x-error-codes'][?(@.code == 51001)].constant").value("STOCK_NOT_ENOUGH"))
                .andExpect(jsonPath("$['x-error-codes'][?(@.code == 51001)].enumClass")
                        .value("io.github.biglv666.webcommon.test.TestOrderErrorCode"));
    }
}
