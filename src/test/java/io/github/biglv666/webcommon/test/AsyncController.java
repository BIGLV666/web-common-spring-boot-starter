package io.github.biglv666.webcommon.test;

import io.github.biglv666.webcommon.exception.BusinessException;
import io.github.biglv666.webcommon.result.ResultCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * 异步返回类型测试端点：钉住自动包装对 Servlet 异步返回值的行为。
 *
 * <p>Spring MVC 对异步返回值是「异步完成后携带解析值二次进入消息转换」，
 * 自动包装切面看到的是声明类型而非解析值类型，是包装逻辑的边界场景。</p>
 */
@RestController
public class AsyncController {

    /** CompletableFuture 返回业务对象 */
    @GetMapping("/demo/async/user")
    public CompletableFuture<DemoController.UserVO> asyncUser() {
        return CompletableFuture.completedFuture(new DemoController.UserVO("async", 1));
    }

    /** CompletableFuture 返回 String：声明类型非 String，验证 String 特判的边界 */
    @GetMapping("/demo/async/string")
    public CompletableFuture<String> asyncString() {
        return CompletableFuture.completedFuture("async-hello");
    }

    /** Callable 返回业务对象 */
    @GetMapping("/demo/async/callable")
    public Callable<DemoController.UserVO> callableUser() {
        return () -> new DemoController.UserVO("callable", 2);
    }

    /** Callable 返回 String：与 CompletableFuture<String> 同类的声明类型边界 */
    @GetMapping("/demo/async/callable-string")
    public Callable<String> callableString() {
        return () -> "callable-hello";
    }

    /** DeferredResult 返回业务对象 */
    @GetMapping("/demo/async/deferred")
    public DeferredResult<DemoController.UserVO> deferredUser() {
        DeferredResult<DemoController.UserVO> result = new DeferredResult<>();
        result.setResult(new DemoController.UserVO("deferred", 3));
        return result;
    }

    /** 异步管线中的业务异常：由全局异常处理器在异步派发中接管 */
    @GetMapping("/demo/async/biz")
    public CompletableFuture<DemoController.UserVO> asyncBiz() {
        return CompletableFuture.failedFuture(
                new BusinessException(ResultCode.CONFLICT, "异步业务失败"));
    }

    /** 异步管线中的系统异常：验证兜底分支对异步派发同样生效 */
    @GetMapping("/demo/async/system")
    public CompletableFuture<DemoController.UserVO> asyncSystem() {
        return CompletableFuture.failedFuture(new CompletionException(new IllegalStateException("boom")));
    }
}
