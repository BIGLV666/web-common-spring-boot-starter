package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.ErrorCode;
import io.github.biglv666.webcommon.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 错误码注册校验的单元测试：重复码、越段码、保留值、非法声明均应在启动期失败。
 */
class ErrorCodeRegistryTest {

    /** 合法业务枚举：51001 在 5xxxx 业务段 */
    enum ValidBizCode implements ErrorCode {
        SOME_BIZ_ERROR;
        public int getCode() { return 51001; }
        public String getMessage() { return "业务失败"; }
    }

    /** 与内置码冲突的业务枚举：占用 PARAM_ERROR 的 40000 */
    enum ConflictWithBuiltIn implements ErrorCode {
        DUPLICATED_PARAM;
        public int getCode() { return 40000; }
        public String getMessage() { return "重复的参数错误"; }
    }

    /** 越段业务枚举：1234 不在 0/500/40000~59999 */
    enum OutOfSegment implements ErrorCode {
        WEIRD_CODE;
        public int getCode() { return 1234; }
        public String getMessage() { return "越段错误码"; }
    }

    /** 占用系统保留值 0 的业务枚举 */
    enum ReservedZero implements ErrorCode {
        BAD_ZERO;
        public int getCode() { return 0; }
        public String getMessage() { return "占用成功码"; }
    }

    /** 业务枚举之间互相冲突：两个枚举都用 51001 */
    enum AnotherEnum implements ErrorCode {
        CONFLICTING;
        public int getCode() { return 51001; }
        public String getMessage() { return "与 ValidBizCode 冲突"; }
    }

    @Test
    void validDeclarationPasses() {
        ErrorCodeRegistry registry = new ErrorCodeRegistry(List.of(ValidBizCode.class));
        assertThatCode(registry::validate).doesNotThrowAnyException();
    }

    @Test
    void builtInOnlyPasses() {
        ErrorCodeRegistry registry = new ErrorCodeRegistry(List.of());
        assertThatCode(registry::validate).doesNotThrowAnyException();
    }

    @Test
    void conflictWithBuiltInFails() {
        ErrorCodeRegistry registry = new ErrorCodeRegistry(List.of(ConflictWithBuiltIn.class));
        assertThatThrownBy(registry::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("错误码冲突")
                .hasMessageContaining("40000");
    }

    @Test
    void conflictBetweenBizEnumsFails() {
        ErrorCodeRegistry registry = new ErrorCodeRegistry(List.of(ValidBizCode.class, AnotherEnum.class));
        assertThatThrownBy(registry::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("51001");
    }

    @Test
    void outOfSegmentFails() {
        ErrorCodeRegistry registry = new ErrorCodeRegistry(List.of(OutOfSegment.class));
        assertThatThrownBy(registry::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("越段");
    }

    @Test
    void reservedValueFails() {
        ErrorCodeRegistry registry = new ErrorCodeRegistry(List.of(ReservedZero.class));
        assertThatThrownBy(registry::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("保留");
    }

    /** 非枚举的错误码实现类：声明路径要求必须是枚举 */
    static class NotAnEnum implements ErrorCode {
        public int getCode() { return 51999; }
        public String getMessage() { return "非枚举错误码"; }
    }

    @Test
    void nonEnumDeclarationFails() {
        ErrorCodeRegistry registry = new ErrorCodeRegistry(List.of(NotAnEnum.class));
        assertThatThrownBy(registry::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不是枚举类");
    }
}
