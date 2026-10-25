package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.CursorResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CursorResult 的单元测试：工厂、游标规整、hasNext 一致性与不可变性。
 */
class CursorResultTest {

    /**
     * of 工厂：records 与游标原样承载，hasNext 由游标推导。
     */
    @Test
    void ofCarriesRecordsAndCursor() {
        CursorResult<String> result = CursorResult.of(List.of("a", "b"), "cursor-2");
        assertIterableEquals(List.of("a", "b"), result.records());
        assertEquals("cursor-2", result.nextCursor());
        assertTrue(result.hasNext());
    }

    /**
     * 空白游标规整为 null，避免 hasNext=true 但客户端拿着空游标无法继续翻页。
     */
    @Test
    void blankCursorNormalizesToNull() {
        assertNull(CursorResult.of(List.of("a"), null).nextCursor());
        assertNull(CursorResult.of(List.of("a"), "").nextCursor());
        assertNull(CursorResult.of(List.of("a"), "   ").nextCursor());
        assertFalse(CursorResult.of(List.of("a"), " ").hasNext());
    }

    /**
     * last 工厂：末页游标恒为 null，hasNext=false。
     */
    @Test
    void lastHasNoNextCursor() {
        CursorResult<String> result = CursorResult.last(List.of("a"));
        assertNull(result.nextCursor());
        assertFalse(result.hasNext());
    }

    /**
     * null records 规整为空列表，不抛异常。
     */
    @Test
    void ofNormalizesNullRecords() {
        CursorResult<String> result = CursorResult.of(null, "c1");
        assertIterableEquals(List.of(), result.records());
        assertTrue(result.hasNext());
    }

    /**
     * records 不可变：外部修改返回列表会失败。
     */
    @Test
    void recordsAreImmutable() {
        CursorResult<String> result = CursorResult.of(List.of("a"), "c1");
        assertThrows(UnsupportedOperationException.class, () -> result.records().add("b"));
    }

    /**
     * hasNext 是计算属性，不参与 equals：游标相同即相等，
     * 不会因为额外状态出现两个结构相等但 hasNext 不同的实例。
     */
    @Test
    void hasNextIsDerivedAndNotPartOfEquality() {
        CursorResult<String> a = CursorResult.of(List.of("x"), "c1");
        CursorResult<String> b = CursorResult.of(List.of("x"), "c1");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.hasNext());
    }
}
