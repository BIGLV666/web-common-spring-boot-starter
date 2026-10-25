package io.github.biglv666.webcommon.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.biglv666.webcommon.result.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MyBatis-Plus IPage 归一实现测试：支持判断、元数据映射与空边界。
 */
class MybatisPlusPageResultNormalizerTest {

    private final MybatisPlusPageResultNormalizer normalizer = new MybatisPlusPageResultNormalizer();

    /**
     * 只识别 IPage，普通 List 不参与归一。
     */
    @Test
    void supportsOnlyIPage() {
        assertTrue(normalizer.supports(new Page<String>()));
        assertFalse(normalizer.supports(List.of("a")));
        assertFalse(normalizer.supports(null));
    }

    /**
     * 元数据原样映射，总页数按 ceil(total / size) 计算。
     */
    @Test
    void normalizesPageMetadata() {
        Page<String> page = new Page<>(2, 2, 15);
        page.setRecords(List.of("a", "b"));

        PageResult<?> result = normalizer.normalize(page);

        assertEquals(15, result.total());
        assertIterableEquals(List.of("a", "b"), result.records());
        assertEquals(2, result.current());
        assertEquals(2, result.size());
        assertEquals(8, result.pages());
    }

    /**
     * records 为 null 时归一为空列表，不抛异常。
     */
    @Test
    void normalizesNullRecords() {
        Page<String> page = new Page<>(1, 10, 0);
        page.setRecords(null);

        PageResult<?> result = normalizer.normalize(page);

        assertTrue(result.records().isEmpty());
        assertEquals(0, result.total());
        assertEquals(0, result.pages());
    }
}
