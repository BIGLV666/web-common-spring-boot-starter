package io.github.biglv666.webcommon.scantest.conflict;

import io.github.biglv666.webcommon.annotation.ErrorCodeScan;
import io.github.biglv666.webcommon.result.ErrorCode;
import org.springframework.context.annotation.Configuration;

/**
 * 冲突场景测试配置：扫描包内枚举与内置 ResultCode.CONFLICT 的 code 重复，
 * 应用启动必须失败。
 */
@Configuration(proxyBeanMethods = false)
@ErrorCodeScan
public class ConflictScanConfig {
}
