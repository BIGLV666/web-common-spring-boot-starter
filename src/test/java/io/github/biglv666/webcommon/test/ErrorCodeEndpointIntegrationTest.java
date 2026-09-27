package io.github.biglv666.webcommon.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.biglv666.webcommon.annotation.EnableErrorCodeEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 错误码字典端点集成测试：在配置类上标注 {@code @EnableErrorCodeEndpoint}
 * 后，{@code GET /web-common/error-codes} 输出启动期校验通过的全量错误码字典。
 */
@SpringBootTest(properties = "web-common.error-codes=io.github.biglv666.webcommon.test.TestOrderErrorCode")
@AutoConfigureMockMvc
class ErrorCodeEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 端点开启配置：标注注解即注册端点，无需任何 yml 配置。
     * 嵌套 {@code @TestConfiguration} 会叠加到 TestApplication 主配置上，
     * 不会替换自动装配。
     */
    @TestConfiguration
    @EnableErrorCodeEndpoint
    static class EndpointConfig {
    }

    /**
     * 字典内容：响应为统一 Result 成功结构，包含内置码与业务声明码，
     * 条目携带默认文案、来源枚举与常量名。
     */
    @Test
    void endpointReturnsValidatedDictionary() throws Exception {
        mockMvc.perform(get("/web-common/error-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.code == 40900)].constant").value(hasItem("CONFLICT")))
                .andExpect(jsonPath("$.data[?(@.code == 40000)].message").value(hasItem("参数错误")))
                .andExpect(jsonPath("$.data[?(@.code == 51001)].message").value(hasItem("库存不足")))
                .andExpect(jsonPath("$.data[?(@.code == 51001)].enumClass")
                        .value(hasItem("io.github.biglv666.webcommon.test.TestOrderErrorCode")))
                .andExpect(jsonPath("$.data[?(@.code == 51001)].constant").value(hasItem("STOCK_NOT_ENOUGH")));
    }

    /**
     * 字典按 code 升序输出，便于文档查阅与前后端 diff 对齐。
     */
    @Test
    void entriesAreSortedByCode() throws Exception {
        MvcResult result = mockMvc.perform(get("/web-common/error-codes")).andReturn();
        JsonNode data = new ObjectMapper().readTree(result.getResponse().getContentAsString()).path("data");
        assertTrue(data.isArray(), "data 应为错误码条目数组");
        int previous = -1;
        for (JsonNode entry : data) {
            int code = entry.path("code").asInt();
            assertTrue(code >= previous, "条目应按 code 升序: " + previous + " -> " + code);
            previous = code;
        }
    }
}
