package io.github.biglv666.webcommon.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ResultMatchers 测试工具包的集成测试：工具本身也需要被验证，
 * 同时以真实用例展示使用方视角的用法（正确断言通过、错误断言失败）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResultMatchersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * success()：code=0 断言通过。
     */
    @Test
    void successMatchesCodeZero() throws Exception {
        mockMvc.perform(get("/demo/success"))
                .andExpect(status().isOk())
                .andDo(ResultMatchers.success());
    }

    /**
     * success(data)：data 相等断言通过（简单类型）。
     */
    @Test
    void successMatchesDataValue() throws Exception {
        mockMvc.perform(get("/demo/wrap-string").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andDo(ResultMatchers.success("raw-str"));
    }

    /**
     * error(code, fragment)：错误码与 message 片段断言通过。
     */
    @Test
    void errorMatchesCodeAndMessageFragment() throws Exception {
        mockMvc.perform(get("/demo/biz"))
                .andExpect(status().isOk())
                .andDo(ResultMatchers.error(40900, "已被注册"));
    }

    /**
     * paramError(field, fragment)：校验明细按字段断言通过。
     */
    @Test
    void paramErrorMatchesFieldEntry() throws Exception {
        mockMvc.perform(get("/demo/param").param("page", "0"))
                .andExpect(status().isOk())
                .andDo(ResultMatchers.paramError("page", "正数"));
    }

    /**
     * 反向验证：断言器真的在断言——错误码不符时 success()/error() 必须失败。
     */
    @Test
    void matchersFailOnWrongEnvelope() throws Exception {
        assertThrows(AssertionError.class, () ->
                mockMvc.perform(get("/demo/biz")).andDo(ResultMatchers.success()));
        assertThrows(AssertionError.class, () ->
                mockMvc.perform(get("/demo/biz")).andDo(ResultMatchers.error(500)));
    }
}
