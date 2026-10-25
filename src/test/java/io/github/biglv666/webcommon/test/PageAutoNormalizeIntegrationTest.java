package io.github.biglv666.webcommon.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 分页自动归一集成测试：Spring Data Page / MyBatis-Plus IPage 直接返回、
 * Result 包裹、ResponseEntity、异步返回均输出统一分页结构。
 */
@SpringBootTest
@AutoConfigureMockMvc
class PageAutoNormalizeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 直接返回 Spring Data Page：归一为 {total, records, current, size, pages}，
     * 不再输出 pageable/sort 等 Spring Data 内部结构。
     */
    @Test
    void springDataPageIsNormalized() throws Exception {
        mockMvc.perform(get("/demo/page-spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].name").value("s1"))
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.pages").value(8))
                .andExpect(jsonPath("$.data.pageable").doesNotExist())
                .andExpect(jsonPath("$.data.content").doesNotExist());
    }

    /**
     * 直接返回 MyBatis-Plus IPage：保留五个核心字段，丢弃 orders 等内部状态。
     */
    @Test
    void mybatisPlusPageIsNormalizedWithoutInternalState() throws Exception {
        mockMvc.perform(get("/demo/page-mp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].name").value("m1"))
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.pages").value(8))
                .andExpect(jsonPath("$.data.orders").doesNotExist())
                .andExpect(jsonPath("$.data.optimizeCountSql").doesNotExist())
                .andExpect(jsonPath("$.data.searchCount").doesNotExist());
    }

    /**
     * 显式 Result.ok(page)：data 内的分页对象同样归一，不能手动包一层就绕过契约。
     */
    @Test
    void pageInsideResultIsNormalized() throws Exception {
        mockMvc.perform(get("/demo/result-page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("r1"))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andExpect(jsonPath("$.data.content").doesNotExist());
    }

    /**
     * ResponseEntity 包裹 Page：body 解析后同样归一。
     */
    @Test
    void pageInsideResponseEntityIsNormalized() throws Exception {
        mockMvc.perform(get("/demo/response-entity-page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("e1"))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.content").doesNotExist());
    }

    /**
     * 异步返回 Page：异步完成后仍归一，证明包装发生在解析值而非声明类型。
     */
    @Test
    void asyncPageIsNormalized() throws Exception {
        MvcResult async = mockMvc.perform(get("/demo/page-async"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("a1"))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.content").doesNotExist());
    }

    /**
     * @NoWrap 豁免：Page 原样输出，不出现 code/data 包络。
     */
    @Test
    void noWrapPageIsNotNormalized() throws Exception {
        mockMvc.perform(get("/demo/nowrap-page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.content[0].name").value("n1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    /**
     * 手动 PageResult.of 不携带元数据，保持 0.4.0 的 JSON 形状。
     */
    @Test
    void manualPageResultKeepsLegacyShape() throws Exception {
        mockMvc.perform(get("/demo/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.current").doesNotExist())
                .andExpect(jsonPath("$.data.size").doesNotExist())
                .andExpect(jsonPath("$.data.pages").doesNotExist());
    }

    /**
     * 游标分页：records + nextCursor + 计算属性 hasNext，不出现 total/pages。
     */
    @Test
    void cursorResultIsWrapped() throws Exception {
        mockMvc.perform(get("/demo/cursor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.nextCursor").value("cursor-2"))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.total").doesNotExist())
                .andExpect(jsonPath("$.data.pages").doesNotExist());
    }

    /**
     * 游标末页：nextCursor 输出 null，hasNext=false。
     */
    @Test
    void cursorLastPageHasNullCursor() throws Exception {
        mockMvc.perform(get("/demo/cursor-last"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.nextCursor").value(nullValue()))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    /**
     * 显式 Result 包裹 CursorResult：非框架分页对象，原样作为 data 输出。
     */
    @Test
    void cursorInsideResultIsWrapped() throws Exception {
        mockMvc.perform(get("/demo/result-cursor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextCursor").value("cursor-3"))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }
}
