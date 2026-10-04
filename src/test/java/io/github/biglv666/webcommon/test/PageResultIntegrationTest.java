package io.github.biglv666.webcommon.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PageResult 响应形状的集成测试：自动包装后 data 为 {total, records} 结构。
 */
@SpringBootTest
@AutoConfigureMockMvc
class PageResultIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 分页响应：data.total 为总条数，data.records 为当前页数组，元素结构原样保留。
     */
    @Test
    void pageResultIsWrappedWithTotalAndRecords() throws Exception {
        mockMvc.perform(get("/demo/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].name").value("p1"))
                .andExpect(jsonPath("$.data.records[1].age").value(2));
    }
}
