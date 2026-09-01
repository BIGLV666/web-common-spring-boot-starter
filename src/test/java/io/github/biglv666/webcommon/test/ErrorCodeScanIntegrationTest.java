package io.github.biglv666.webcommon.test;

import io.github.biglv666.webcommon.config.WebCommonAutoConfiguration;
import io.github.biglv666.webcommon.scantest.badtype.BadTypeScanConfig;
import io.github.biglv666.webcommon.scantest.conflict.ConflictScanConfig;
import io.github.biglv666.webcommon.scantest.scan.ScanConfig;
import io.github.biglv666.webcommon.scantest.scan.ScanDemoController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @ErrorCodeScan 扫描注册集成测试：扫描枚举真实参与错误码处理，
 * 冲突与非枚举实现在启动期即失败。
 */
@SpringBootTest(properties = "web-common.error-codes=io.github.biglv666.webcommon.test.TestOrderErrorCode")
@AutoConfigureMockMvc
@Import({ScanConfig.class, ScanDemoController.class})
class ErrorCodeScanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 扫描注册的 ScanDemoErrorCode 应通过启动校验并参与业务异常处理。
     */
    @Test
    void scannedErrorCodeEnumWorks() throws Exception {
        mockMvc.perform(get("/demo/scan-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(52001))
                .andExpect(jsonPath("$.message").value("需要付费"));
    }

    /**
     * 扫描到的枚举与内置码冲突时，启动必须失败并指明冲突来源。
     */
    @Test
    void conflictingScannedEnumFailsStartup() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebCommonAutoConfiguration.class))
                .withUserConfiguration(ConflictScanConfig.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("错误码冲突");
                });
    }

    /**
     * 扫描到非枚举的 ErrorCode 实现时，启动必须失败并明确说明。
     */
    @Test
    void nonEnumErrorCodeImplementationFailsStartup() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebCommonAutoConfiguration.class))
                .withUserConfiguration(BadTypeScanConfig.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("非枚举");
                });
    }
}
