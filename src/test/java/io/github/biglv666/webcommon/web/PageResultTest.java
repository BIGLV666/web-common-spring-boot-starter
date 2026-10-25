package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PageResult 的单元测试：of/from 工厂、分页元数据、null 规整与不可变性。
 */
class PageResultTest {

    /**
     * of 工厂：records 与 total 原样承载，不携带分页元数据。
     */
    @Test
    void ofCarriesRecordsAndTotal() {
        PageResult<String> page = PageResult.of(List.of("a", "b"), 15);
        assertEquals(15, page.total());
        assertIterableEquals(List.of("a", "b"), page.records());
        assertNull(page.current());
        assertNull(page.size());
        assertNull(page.pages());
    }

    /**
     * 兼容 0.4.0 的两参构造器：源码与二进制行为都保持不变。
     */
    @Test
    void twoArgConstructorKeepsLegacyShape() {
        PageResult<String> page = new PageResult<>(15, List.of("a"));
        assertEquals(15, page.total());
        assertIterableEquals(List.of("a"), page.records());
        assertNull(page.current());
        assertNull(page.size());
        assertNull(page.pages());
    }

    /**
     * of 工厂对 null records 规整为空列表，不抛异常。
     */
    @Test
    void ofNormalizesNullRecords() {
        PageResult<String> page = PageResult.of(null, 0);
        assertEquals(0, page.total());
        assertIterableEquals(List.of(), page.records());
    }

    /**
     * from(Page)：Spring Data 页码从 0 开始，转换后从 1 开始；
     * 总页数按 ceil(total / size) 计算。
     */
    @Test
    void fromPageExtractsTotalAndMetadata() {
        PageImpl<String> spring = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 15);
        PageResult<String> page = PageResult.from(spring);
        assertEquals(15, page.total());
        assertIterableEquals(List.of("a", "b"), page.records());
        assertEquals(2, page.current());
        assertEquals(2, page.size());
        assertEquals(8, page.pages());
    }

    /**
     * 元数据工厂：总页数向上取整；size 非正时总页数为 0，避免除零。
     */
    @Test
    void ofWithMetadataComputesPages() {
        PageResult<String> page = PageResult.of(List.of("a"), 15, 2, 2);
        assertEquals(8, page.pages());

        assertEquals(0, PageResult.of(List.of(), 0, 1, 10).pages());
        assertEquals(0, PageResult.of(List.of(), 10, 1, 0).pages());
        assertEquals(1, PageResult.of(List.of("a"), 1, 1, 10).pages());
    }

    /**
     * records 不可变：外部修改返回列表会失败。
     */
    @Test
    void recordsAreImmutable() {
        PageResult<String> page = PageResult.of(List.of("a"), 1);
        assertThrows(UnsupportedOperationException.class, () -> page.records().add("b"));
    }
}
