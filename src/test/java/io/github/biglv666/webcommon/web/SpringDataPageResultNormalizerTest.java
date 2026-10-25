package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Data Page 归一实现测试：支持判断、页码换算与空页边界。
 */
class SpringDataPageResultNormalizerTest {

    private final SpringDataPageResultNormalizer normalizer = new SpringDataPageResultNormalizer();

    /**
     * 只识别 Page，普通 List 不参与归一。
     */
    @Test
    void supportsOnlyPage() {
        assertTrue(normalizer.supports(new PageImpl<>(List.of("a"))));
        assertFalse(normalizer.supports(List.of("a")));
        assertFalse(normalizer.supports(null));
    }

    /**
     * 页码 0-based 转 1-based，总页数按 ceil(total / size) 计算。
     */
    @Test
    void normalizesPageMetadata() {
        PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(2, 2), 15);
        PageResult<?> result = normalizer.normalize(page);

        assertEquals(15, result.total());
        assertIterableEquals(List.of("a", "b"), result.records());
        assertEquals(3, result.current());
        assertEquals(2, result.size());
        assertEquals(8, result.pages());
    }

    /**
     * 空页：records 为空、total=0、pages=0，不抛异常。
     */
    @Test
    void normalizesEmptyPage() {
        PageImpl<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        PageResult<?> result = normalizer.normalize(page);

        assertEquals(0, result.total());
        assertTrue(result.records().isEmpty());
        assertEquals(1, result.current());
        assertEquals(0, result.pages());
    }
}
