package io.github.biglv666.webcommon.test;

import io.github.biglv666.webcommon.result.Result;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring 6.1+ 方法参数内置校验触发端点：类上刻意不标 {@code @Validated}，
 * 参数约束失败由 Spring MVC 内置校验抛出 {@code HandlerMethodValidationException}，
 * 与 {@link DemoController} 的类级 {@code @Validated}（抛 ConstraintViolationException）
 * 形成两条不同分支的覆盖。
 */
@RestController
public class MethodValidationController {

    /**
     * 页码约束校验：page 传非正数应返回 40000 + 「page: 原因」明细。
     */
    @GetMapping("/demo/method-validation")
    public Result<Void> methodValidation(@RequestParam @Positive(message = "页码必须为正数") int page) {
        return Result.ok();
    }
}
