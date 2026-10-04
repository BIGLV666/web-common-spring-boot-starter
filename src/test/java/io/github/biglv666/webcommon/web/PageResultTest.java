package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PageResult 的单元测试：of/from 工厂、null 规整与不可变性。
 */
class PageResultTest {

    /**
     * of 工厂：records 与 total 原样承载。
     */
    @Test
    void ofCarriesRecordsAndTotal() {
        PageResult<String> page = PageResult.of(List.of("a", "b"), 15);
        assertEquals(15, page.total());
        assertIterableEquals(List.of("a", "b"), page.records());
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
     * from(Page)：取 Page 的总条数与当前页内容。
     */
    @Test
    void fromPageExtractsTotalAndContent() {
        PageImpl<String> spring = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 15);
        PageResult<String> page = PageResult.from(spring);
        assertEquals(15, page.total());
        assertIterableEquals(List.of("a", "b"), page.records());
    }

    /**
     * records 不可变：外部修改拷贝来源或修改返回列表都会失败或无效。
     */
    @Test
    void recordsAreImmutable() {
        PageResult<String> page = PageResult.of(List.of("a"), 1);
        assertThrows(UnsupportedOperationException.class, () -> page.records().add("b"));
    }
}
