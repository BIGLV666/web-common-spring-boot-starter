package io.github.biglv666.webcommon.scantest.conflict;

import io.github.biglv666.webcommon.result.ErrorCode;

/**
 * 冲突场景测试枚举：40900 与内置 ResultCode.CONFLICT 重复，注册校验必须拦截。
 */
public enum ConflictingScanErrorCode implements ErrorCode {

    /** 故意与内置 CONFLICT(40900) 冲突的码 */
    USER_NAME_TAKEN(40900, "用户名已被占用");

    private final int code;

    private final String message;

    ConflictingScanErrorCode(int code, String message) {
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
