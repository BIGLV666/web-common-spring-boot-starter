package io.github.biglv666.webcommon.scantest.badtype;

import io.github.biglv666.webcommon.result.ErrorCode;

/**
 * 非枚举的 ErrorCode 实现：本 starter 约定错误码必须是枚举，扫描发现时启动失败。
 */
public class NotAnEnumErrorCode implements ErrorCode {

    @Override
    public int getCode() {
        return 52100;
    }

    @Override
    public String getMessage() {
        return "不是枚举";
    }
}
