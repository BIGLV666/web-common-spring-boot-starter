package io.github.biglv666.webcommon.web;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link RequestBodyErrorDetail} 单元测试：分别用真实的 Jackson 2 与
 * Jackson 3 {@code InvalidFormatException} 验证字段路径提取——两代
 * 路径访问器不同名（2.x {@code getFieldName()} / 3.x {@code getPropertyName()}），
 * 反射兜底逻辑需各跑一次。测试类路径同时含两代库。
 */
class RequestBodyErrorDetailTest {

    /**
     * 纯字段路径：根字段类型不匹配应输出 {@code age}。
     */
    @Test
    void extractsPlainFieldPath() {
        InvalidFormatException exception = buildException();
        exception.prependPath(new Object(), "age");
        assertEquals("age", RequestBodyErrorDetail.fieldPathOf(exception));
    }

    /**
     * 嵌套 + 数组下标路径：应输出 {@code items[0].count} 形式。
     */
    @Test
    void extractsNestedPathWithArrayIndex() {
        InvalidFormatException exception = buildException();
        exception.prependPath(new Object(), "count");
        exception.prependPath(new Object(), 0);
        exception.prependPath(new Object(), "items");
        assertEquals("items[0].count", RequestBodyErrorDetail.fieldPathOf(exception));
    }

    /**
     * cause 链包装：被业务异常包裹的反序列化异常同样能提取。
     */
    @Test
    void walksCauseChainToFindInvalidFormat() {
        InvalidFormatException exception = buildException();
        exception.prependPath(new Object(), "age");
        IllegalStateException wrapped = new IllegalStateException("service failed", exception);
        assertEquals("age", RequestBodyErrorDetail.fieldPathOf(wrapped));
    }

    /**
     * 无路径信息（如空列表）返回 null，交由调用方回退固定文案。
     */
    @Test
    void returnsNullWhenNoPathAvailable() {
        InvalidFormatException exception = buildException();
        assertNull(RequestBodyErrorDetail.fieldPathOf(exception));
    }

    /**
     * 非 InvalidFormatException 异常返回 null。
     */
    @Test
    void returnsNullForOtherExceptionTypes() {
        assertNull(RequestBodyErrorDetail.fieldPathOf(new IllegalStateException("boom")));
    }

    /**
     * 构造无路径信息的 InvalidFormatException 实例。
     */
    private InvalidFormatException buildException() {
        return InvalidFormatException.from(null, "类型不匹配", "abc", Integer.class);
    }

    /**
     * Jackson 3（tools.jackson）异常的路径访问器是 getPropertyName()，
     * 与 2.x 的 getFieldName() 不同名，反射兜底必须命中。
     */
    @Test
    void extractsFieldPathFromJackson3Exception() {
        // Jackson 3 prependPath 按调用顺序逐段前置，先字段后下标再父字段
        tools.jackson.databind.exc.InvalidFormatException root =
                tools.jackson.databind.exc.InvalidFormatException.from(null, "类型不匹配", "abc", Integer.class);
        root.prependPath(new Object(), "count");
        root.prependPath(new Object(), 0);
        root.prependPath(new Object(), "items");
        assertEquals("items[0].count", RequestBodyErrorDetail.fieldPathOf(root));
    }
}
