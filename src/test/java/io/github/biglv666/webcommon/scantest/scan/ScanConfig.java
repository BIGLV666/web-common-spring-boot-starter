package io.github.biglv666.webcommon.scantest.scan;

import io.github.biglv666.webcommon.annotation.ErrorCodeScan;
import io.github.biglv666.webcommon.result.ErrorCode;
import org.springframework.context.annotation.Configuration;

/**
 * @ErrorCodeScan 集成测试配置：未指定 basePackages，验证默认回退到标注类所在包。
 */
@Configuration(proxyBeanMethods = false)
@ErrorCodeScan
public class ScanConfig {
}
