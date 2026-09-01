package io.github.biglv666.webcommon.scantest.badtype;

import io.github.biglv666.webcommon.annotation.ErrorCodeScan;
import org.springframework.context.annotation.Configuration;

/**
 * 非枚举场景测试配置：扫描包内存在普通类实现的 ErrorCode，扫描必须拒绝。
 */
@Configuration(proxyBeanMethods = false)
@ErrorCodeScan
public class BadTypeScanConfig {
}
