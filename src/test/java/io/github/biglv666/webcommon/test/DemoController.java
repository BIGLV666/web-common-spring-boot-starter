package io.github.biglv666.webcommon.test;

import io.github.biglv666.webcommon.annotation.DefaultErrorCode;
import io.github.biglv666.webcommon.annotation.NoWrap;
import io.github.biglv666.webcommon.exception.BusinessException;
import io.github.biglv666.webcommon.result.Result;
import io.github.biglv666.webcommon.result.ResultCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 集成测试用 demo 控制器，覆盖统一返回与全局异常处理的四条典型路径。
 */
@RestController
@Validated
public class DemoController {

    /**
     * 路径一：正常成功返回。
     */
    @GetMapping("/demo/success")
    public Result<String> success() {
        return Result.ok("hello learncard");
    }

    /**
     * 路径二：业务异常，使用自定义文案覆盖错误码默认文案。
     */
    @GetMapping("/demo/biz")
    public Result<Void> biz() {
        throw new BusinessException(ResultCode.CONFLICT, "用户名已被注册");
    }

    /**
     * 路径三：请求体参数校验失败。
     */
    @PostMapping("/demo/valid")
    public Result<Void> valid(@Valid @RequestBody CreateUserRequest request) {
        return Result.ok();
    }

    /**
     * 路径三补充：单参数校验失败（类级 @Validated + @RequestParam 约束）。
     */
    @GetMapping("/demo/param")
    public Result<Void> param(@RequestParam @Positive(message = "页码必须为正数") int page) {
        return Result.ok();
    }

    /**
     * 路径四：未识别异常，应由兜底分支返回 SYSTEM_ERROR 且不泄露内部细节。
     */
    @GetMapping("/demo/unknown")
    public Result<Void> unknown() {
        throw new IllegalStateException("数据库连接已断开: jdbc:mysql://127.0.0.1:3306/secret");
    }

    /**
     * 自动包装路径：直接返回业务对象，应由 ResultWrapAdvice 包装为 code=0 的 Result。
     */
    @GetMapping("/demo/wrap")
    public UserVO wrap() {
        return new UserVO("张三", 25);
    }

    /**
     * 自动包装豁免路径：标注 @NoWrap，应原样返回业务对象的 JSON，不带 code/message 壳。
     */
    @NoWrap
    @GetMapping("/demo/nowrap")
    public UserVO nowrap() {
        return new UserVO("李四", 30);
    }

    /**
     * 自动包装 String 返回路径：验证 StringHttpMessageConverter 场景序列化正确。
     */
    @GetMapping("/demo/wrap-string")
    public String wrapString() {
        return "raw-str";
    }

    /**
     * 模板抛错路径：BusinessException.of 填充 {} 占位符。
     */
    @GetMapping("/demo/template")
    public Result<Void> template() {
        throw BusinessException.of(ResultCode.BIZ_ERROR, "库存不足，剩余 {} 件", 3);
    }

    /**
     * 声明式抛错路径：自定义异常类标注 @DefaultErrorCode，无继承 BusinessException。
     */
    @GetMapping("/demo/annotated")
    public Result<Void> annotated() {
        throw new StockNotEnoughException("商品已售罄");
    }

    /**
     * 业务自建错误码路径：使用测试枚举 TestOrderErrorCode，验证注册校验与扩展机制。
     */
    @GetMapping("/demo/custom-code")
    public Result<Void> customCode() {
        throw new BusinessException(TestOrderErrorCode.STOCK_NOT_ENOUGH);
    }

    /**
     * 声明式业务异常示例：标注 @DefaultErrorCode 的普通 RuntimeException 子类。
     * ResultCode 是多常量枚举，需指定 constant。
     */
    @DefaultErrorCode(value = ResultCode.class, constant = "BIZ_ERROR")
    public static class StockNotEnoughException extends RuntimeException {
        public StockNotEnoughException(String message) {
            super(message);
        }
    }

    /**
     * 业务对象示例，用于自动包装测试。
     */
    public record UserVO(String name, int age) {
    }

    /**
     * 请求体校验用 DTO。
     */
    public static class CreateUserRequest {

        /** 用户名，不能为空 */
        @NotBlank(message = "姓名不能为空")
        private String name;

        /** 年龄，必须为正数 */
        @NotNull(message = "年龄不能为空")
        @Positive(message = "年龄必须为正数")
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }
}
