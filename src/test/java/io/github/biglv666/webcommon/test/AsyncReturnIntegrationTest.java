package io.github.biglv666.webcommon.test;

import io.github.biglv666.webcommon.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 异步返回类型（CompletableFuture/Callable/DeferredResult）自动包装的集成测试，
 * 钉住 Servlet 异步返回值在自动包装下的行为契约。
 *
 * <p>Spring MVC 对异步返回值先启动异步、完成后携带解析值二次进入消息转换，
 * 包装切面看到的声明类型与解析值类型可能不同（如 {@code CompletableFuture<String>}），
 * 本测试同时覆盖这两种形态与异步管线中的异常。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AsyncReturnIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * CompletableFuture 业务对象：正常包装。
     */
    @Test
    void completableFutureOfObjectIsWrapped() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/demo/async/user"))
                .andExpect(request().asyncResult(instanceOf(DemoController.UserVO.class)))
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("async"));
    }

    /**
     * CompletableFuture String：声明类型不是 String，解析值是 String。
     * 钉住契约：仍然按统一包络包装（JSON 输出，不退化为字符串字面量）。
     */
    @Test
    void completableFutureOfStringIsWrapped() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/demo/async/string"))
                .andExpect(request().asyncResult("async-hello"))
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("async-hello"));
    }

    /**
     * Callable 业务对象：正常包装。
     */
    @Test
    void callableOfObjectIsWrapped() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/demo/async/callable"))
                .andExpect(request().asyncResult(instanceOf(DemoController.UserVO.class)))
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("callable"));
    }

    /**
     * Callable String：解析值走 StringHttpMessageConverter 的边界场景。
     * 包装对象若未先序列化会被 toString 成字面量，此用例钉住其正确行为。
     */
    @Test
    void callableOfStringIsWrappedAsJson() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/demo/async/callable-string"))
                .andExpect(request().asyncResult("callable-hello"))
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("callable-hello"));
    }

    /**
     * Callable String + Accept: text/plain：解析值命中的是 StringHttpMessageConverter，
     * 包装对象若未先序列化成 JSON 字符串会 ClassCastException，此用例钉住其正确行为。
     */
    @Test
    void callableOfStringWithTextPlainAcceptIsWrappedAsJson() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/demo/async/callable-string").accept(MediaType.TEXT_PLAIN))
                .andExpect(request().asyncResult("callable-hello"))
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("callable-hello"));
    }

    /**
     * DeferredResult 业务对象：正常包装。
     */
    @Test
    void deferredResultOfObjectIsWrapped() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/demo/async/deferred"))
                .andExpect(request().asyncResult(instanceOf(DemoController.UserVO.class)))
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("deferred"));
    }

    /**
     * 异步管线中的业务异常：全局异常处理器在异步派发中接管，
     * 返回业务错误码与自定义文案，且异常响应不被二次包装。
     */
    @Test
    void businessExceptionInAsyncPipelineIsHandled() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/demo/async/biz"))
                .andExpect(request().asyncResult(instanceOf(BusinessException.class)))
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40900))
                .andExpect(jsonPath("$.message").value("异步业务失败"));
    }

    /**
     * 异步管线中的系统异常：兜底分支返回 500，不泄露内部信息。
     */
    @Test
    void systemExceptionInAsyncPipelineFallsBack() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/demo/async/system"))
                .andExpect(request().asyncResult(instanceOf(Throwable.class)))
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统繁忙，请稍后重试"));
    }
}
