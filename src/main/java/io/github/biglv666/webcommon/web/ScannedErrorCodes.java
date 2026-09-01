package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.ErrorCode;

import java.util.List;

/**
 * {@code @ErrorCodeScan} 的扫描结果载体：持有一次扫描发现的全部错误码枚举类。
 *
 * <p>由 {@link ErrorCodeScanRegistrar} 在配置解析期注册为单个 Bean，
 * 自动装配的 {@code ErrorCodeRegistry} 创建时收集容器内所有本类实例，
 * 与 {@code web-common.error-codes} 配置声明的枚举合并后统一校验。
 * 每个标注 {@code @ErrorCodeScan} 的配置类对应一个本类 Bean，互不干扰。</p>
 */
public class ScannedErrorCodes {

    /** 本次扫描发现的错误码枚举类，可能为空列表 */
    private final List<Class<? extends ErrorCode>> enumClasses;

    /**
     * 构造扫描结果。
     *
     * @param enumClasses 扫描发现的错误码枚举类
     */
    public ScannedErrorCodes(List<Class<? extends ErrorCode>> enumClasses) {
        this.enumClasses = enumClasses == null ? List.of() : List.copyOf(enumClasses);
    }

    /**
     * 读取扫描发现的错误码枚举类。
     *
     * @return 不可变的枚举类列表
     */
    public List<Class<? extends ErrorCode>> getEnumClasses() {
        return enumClasses;
    }
}
