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
 * i18n 文案解析的集成测试：错误码默认文案与成功文案按 key 约定
 * （{@code web-common.error.{code}} / {@code web-common.success}）走 MessageSource，
 * 按请求 locale 取值，缺失回退枚举默认文案；业务自定义文案不参与解析。
 */
@SpringBootTest(properties = "web-common.error-codes=io.github.biglv666.webcommon.test.TestOrderErrorCode")
@AutoConfigureMockMvc
@Import(I18nTestConfig.class)
class I18nIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 英文 locale：默认文案业务异常按 key 解析为英文资源文案。
     */
    @Test
    void defaultBusinessMessageResolvesByLocale() throws Exception {
        mockMvc.perform(get("/demo/biz-default").locale(Locale.US))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("Resource not found (en)"));
    }

    /**
     * 未匹配到语言变体时回退默认资源文件（messages.properties）。
     */
    @Test
    void fallsBackToDefaultBundleWhenLocaleVariantMissing() throws Exception {
        mockMvc.perform(get("/demo/biz-default").locale(Locale.SIMPLIFIED_CHINESE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("默认语言：资源不存在文案"));
    }

    /**
     * 自定义文案不参与 i18n：即使 key 存在，业务异常的自定义 message 原样透出。
     */
    @Test
    void customMessageIsNeverTranslated() throws Exception {
        mockMvc.perform(get("/demo/biz").locale(Locale.US))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40900))
                .andExpect(jsonPath("$.message").value("用户名已被注册"));
    }

    /**
     * 系统兜底文案按 key 解析：500 分支同样参与 i18n。
     */
    @Test
    void systemErrorMessageResolvesByLocale() throws Exception {
        mockMvc.perform(get("/demo/unknown").locale(Locale.US))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("System busy (en)"));
    }

    /**
     * 成功文案按 key 解析：自动包装的 message 随 locale 变化。
     */
    @Test
    void successMessageResolvesByLocale() throws Exception {
        mockMvc.perform(get("/demo/wrap").locale(Locale.US))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Success (en)"));
    }

    /**
     * 显式 Result.ok() 的默认成功文案同样按 locale 解析，与自动包装路径一致。
     */
    @Test
    void explicitResultOkDefaultMessageAlsoResolves() throws Exception {
        mockMvc.perform(get("/demo/success").locale(Locale.US))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Success (en)"));
    }

    /**
     * key 未配置的错误码回退枚举默认文案：语言资源只覆盖 40400/500，
     * TestOrderErrorCode.STOCK_NOT_ENOUGH(51001) 无对应 key，应原样返回枚举文案。
     */
    @Test
    void missingKeyFallsBackToEnumDefault() throws Exception {
        mockMvc.perform(get("/demo/custom-code").locale(Locale.US))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(51001))
                .andExpect(jsonPath("$.message").value("库存不足"));
    }
}
