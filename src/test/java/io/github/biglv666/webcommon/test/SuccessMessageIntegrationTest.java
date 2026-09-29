package io.github.biglv666.webcommon.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 成功文案可配置验证：{@code web-common.success-message} 应同时作用于
 * 自动包装响应与显式 {@code Result.ok(...)} 响应。
 */
@SpringBootTest(properties = {
        "web-common.error-codes=io.github.biglv666.webcommon.test.TestOrderErrorCode",
        "web-common.success-message=载入成功"
})
@AutoConfigureMockMvc
class SuccessMessageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 自动包装路径的成功文案应取配置值。
     */
    @Test
    void wrappedResponseUsesConfiguredSuccessMessage() throws Exception {
        mockMvc.perform(get("/demo/wrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("载入成功"))
                .andExpect(jsonPath("$.data.name").value("张三"));
    }

    /**
     * 显式 Result.ok 路径应使用启动时应用过的静态默认文案。
     */
    @Test
    void explicitOkUsesConfiguredSuccessMessage() throws Exception {
        mockMvc.perform(get("/demo/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("载入成功"))
                .andExpect(jsonPath("$.data").value("hello learncard"));
    }

    /**
     * 静态默认值是 JVM 级状态，测试结束后恢复内置默认，避免污染同 JVM 的其他上下文。
     */
    @AfterAll
    static void restoreDefaultMessage() {
        io.github.biglv666.webcommon.result.Result.setDefaultSuccessMessage(
                io.github.biglv666.webcommon.result.Result.SUCCESS_MESSAGE_DEFAULT);
    }
}
