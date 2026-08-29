package io.github.biglv666.webcommon.test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 模拟第三方 starter（如 api-governance）自带的全局异常处理器：
 * 返回自定义结构 + 非 200 HTTP 状态码，验证自动包装不会破坏它。
 */
public class ExternalAdviceFixture {

    /** 模拟治理类异常 */
    public static class GovernanceException extends RuntimeException {
        public GovernanceException(String message) {
            super(message);
        }
    }

    /** 第三方异常处理器：返回自己的结构，HTTP 429 */
    @RestControllerAdvice
    public static class GovernanceLikeAdvice {
        @ExceptionHandler(GovernanceException.class)
        public ResponseEntity<Map<String, Object>> handle(GovernanceException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("success", false, "code", "RATE_LIMITED", "message", e.getMessage()));
        }
    }

    /** 触发第三方异常的端点 */
    @RestController
    public static class ThrowingController {
        @GetMapping("/demo/governance")
        public String trigger() {
            throw new GovernanceException("请求过于频繁");
        }
    }
}
