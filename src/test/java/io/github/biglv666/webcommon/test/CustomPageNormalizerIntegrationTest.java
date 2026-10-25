package io.github.biglv666.webcommon.test;

import io.github.biglv666.webcommon.customtest.CustomPageController;
import io.github.biglv666.webcommon.customtest.CustomPageNormalizerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 自定义 PageResultNormalizer 扩展点测试：未内置的第三方分页对象
 * 通过声明 Bean 即可接入自动归一。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({CustomPageController.class, CustomPageNormalizerConfig.class})
class CustomPageNormalizerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 自定义 Normalizer 生效：第三方分页对象被转换为统一 PageResult。
     */
    @Test
    void customNormalizerIsApplied() throws Exception {
        mockMvc.perform(get("/demo/custom-page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(7))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0]").value("x"))
                .andExpect(jsonPath("$.data.current").doesNotExist());
    }
}
