package io.github.biglv666.webcommon.test;

import io.github.biglv666.webcommon.result.PageResultNormalizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 分页归一开关测试：{@code web-common.pagination.enabled=false} 时
 * Page 原样序列化，行为与 0.4.0 一致。
 */
@SpringBootTest(properties = "web-common.pagination.enabled=false")
@AutoConfigureMockMvc
class PageNormalizeDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 关闭后不注册任何内置归一实现，证明开关作用于装配层而非仅跳过调用。
     */
    @Test
    void normalizerBeansAreAbsentWhenDisabled() {
        assertEquals(0, applicationContext.getBeanNamesForType(PageResultNormalizer.class).length,
                "关闭分页归一后不应注册 PageResultNormalizer");
    }

    /**
     * 关闭后 Spring Data Page 保持原始字段：content/totalElements/pageable 等。
     */
    @Test
    void springDataPageIsNotNormalizedWhenDisabled() throws Exception {
        mockMvc.perform(get("/demo/page-spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(15))
                .andExpect(jsonPath("$.data.records").doesNotExist())
                .andExpect(jsonPath("$.data.current").doesNotExist());
    }

    /**
     * 关闭归一不影响手动 PageResult：仍按用户构造的形状输出。
     */
    @Test
    void manualPageResultStillWorksWhenDisabled() throws Exception {
        mockMvc.perform(get("/demo/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.records.length()").value(2));
    }
}
