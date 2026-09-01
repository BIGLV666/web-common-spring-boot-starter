package io.github.biglv666.webcommon.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * semantic 模式集成测试：{@code web-common.http-status-mode=SEMANTIC} 时，
 * HTTP 状态码按错误码段映射（4xxxx→400、40400→404、500→500、5xxxx 业务码→200），
 * 且响应体统一 Result 结构保持不变。
 */
@SpringBootTest(properties = {
        "web-common.error-codes=io.github.biglv666.webcommon.test.TestOrderErrorCode",
        "web-common.http-status-mode=SEMANTIC"
})
@AutoConfigureMockMvc
class StatusModeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 请求体校验失败（40000）应映射为 HTTP 400。
     */
    @Test
    void paramErrorMapsToHttpStatus400() throws Exception {
        mockMvc.perform(post("/demo/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"age\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    /**
     * 40900 属于 4xxxx 客户端侧码段，应映射为 HTTP 400。
     */
    @Test
    void conflictCodeMapsToHttpStatus400() throws Exception {
        mockMvc.perform(get("/demo/biz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40900));
    }

    /**
     * 50000 属于 5xxxx 业务码段，业务失败不是服务端故障，HTTP 状态保持 200。
     */
    @Test
    void businessCodeKeepsHttpStatus200() throws Exception {
        mockMvc.perform(get("/demo/template"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50000));
    }

    /**
     * 系统兜底（500）应映射为 HTTP 500。
     */
    @Test
    void systemErrorMapsToHttpStatus500() throws Exception {
        mockMvc.perform(get("/demo/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }

    /**
     * 路径不存在应映射为 HTTP 404（Spring 6.1+ 默认抛 NoResourceFoundException）。
     */
    @Test
    void notFoundMapsToHttpStatus404() throws Exception {
        mockMvc.perform(get("/demo/not-exist-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    /**
     * 方法参数内置校验（HandlerMethodValidationException）返回 40000，
     * semantic 模式下应为 HTTP 400，明细格式与单参数校验一致。
     */
    @Test
    void handlerMethodValidationMapsToHttpStatus400() throws Exception {
        mockMvc.perform(get("/demo/method-validation").param("page", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message", containsString("page: 页码必须为正数")));
    }

    /**
     * 上传超限分支返回 40000 固定文案，semantic 模式下应为 HTTP 400，
     * 且不透出异常内部细节。
     */
    @Test
    void maxUploadSizeMapsToHttpStatus400() throws Exception {
        mockMvc.perform(post("/demo/upload-oversize"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("上传文件过大"));
    }

    /**
     * 成功路径两种模式均为 HTTP 200。
     */
    @Test
    void successKeepsHttpStatus200() throws Exception {
        mockMvc.perform(get("/demo/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
