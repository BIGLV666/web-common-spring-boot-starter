package io.github.biglv666.webcommon.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import io.github.biglv666.webcommon.fixture.I18nTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * i18n 关闭态（web-common.i18n.enabled=false）的契约测试：
 * 即使容器存在配置了语言资源的 MessageSource，文案也不被解析，
 * 全部保持枚举默认值与配置成功文案，行为与 0.3.0 完全一致。
 */
@SpringBootTest(properties = {
        "web-common.i18n.enabled=false",
        "web-common.error-codes=io.github.biglv666.webcommon.test.TestOrderErrorCode"})
@AutoConfigureMockMvc
@Import(I18nTestConfig.class)
class I18nDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 关闭态：默认文案业务异常不解析，保持枚举默认文案。
     */
    @Test
    void errorMessagesStayDefaultWhenDisabled() throws Exception {
        mockMvc.perform(get("/demo/biz-default").locale(Locale.US))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    /**
     * 关闭态：成功文案保持配置默认值，不随 locale 变化。
     */
    @Test
    void successMessageStaysDefaultWhenDisabled() throws Exception {
        mockMvc.perform(get("/demo/success").locale(Locale.US))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }
}
