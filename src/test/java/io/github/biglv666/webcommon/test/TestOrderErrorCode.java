package io.github.biglv666.webcommon.test;

import io.github.biglv666.webcommon.result.ErrorCode;

/**
 * 测试用业务自建错误码枚举，模拟业务方通过
 * {@code web-common.error-codes} 声明扩展的场景。
 */
public enum TestOrderErrorCode implements ErrorCode {

    /** 库存不足：订单模块业务错误示例 */
    STOCK_NOT_ENOUGH(51001, "库存不足");

    private final int code;

    private final String message;

    TestOrderErrorCode(int code, String message) {
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
