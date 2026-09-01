package io.github.biglv666.webcommon.scantest.scan;

import io.github.biglv666.webcommon.result.ErrorCode;

/**
 * @ErrorCodeScan 扫描目标枚举：与内置码、yaml 声明码均不冲突。
 */
public enum ScanDemoErrorCode implements ErrorCode {

    /** 使用场景：功能需要付费后才能使用 */
    PAY_REQUIRED(52001, "需要付费");

    private final int code;

    private final String message;

    ScanDemoErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
