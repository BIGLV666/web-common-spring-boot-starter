package io.github.biglv666.webcommon.test;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.biglv666.webcommon.annotation.DefaultErrorCode;
import io.github.biglv666.webcommon.annotation.NoWrap;
import io.github.biglv666.webcommon.exception.BusinessException;
import io.github.biglv666.webcommon.result.CursorResult;
import io.github.biglv666.webcommon.result.PageResult;
import io.github.biglv666.webcommon.result.Result;
import io.github.biglv666.webcommon.result.ResultCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.concurrent.Callable;

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
     * 路径二补充：业务异常使用错误码默认文案（不传自定义文案），
     * 供 i18n 测试验证按 locale 解析 {@code web-common.error.{code}}。
     */
    @GetMapping("/demo/biz-default")
    public Result<Void> bizDefault() {
        throw new BusinessException(ResultCode.NOT_FOUND);
    }

    /**
     * 分页路径：PageResult 结构演示，供分页响应形状测试。
     */
    @GetMapping("/demo/page")
    public PageResult<DemoController.UserVO> page() {
        return PageResult.of(List.of(new DemoController.UserVO("p1", 1), new DemoController.UserVO("p2", 2)), 15);
    }

    /**
     * 分页自动归一：直接返回 Spring Data Page，应被包成统一分页结构
     * （页码 1-based，总页数 ceil(total / size)）。
     */
    @GetMapping("/demo/page-spring")
    public Page<UserVO> pageSpring() {
        return new PageImpl<>(List.of(new UserVO("s1", 1), new UserVO("s2", 2)), PageRequest.of(1, 2), 15);
    }

    /**
     * 分页自动归一：直接返回 MyBatis-Plus IPage，只保留前端需要的五个字段。
     */
    @GetMapping("/demo/page-mp")
    public IPage<UserVO> pageMp() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserVO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(2, 2, 15);
        page.setRecords(List.of(new UserVO("m1", 1), new UserVO("m2", 2)));
        return page;
    }

    /**
     * 显式 Result 包裹 Page：data 内的分页对象同样应被归一。
     */
    @GetMapping("/demo/result-page")
    public Result<Page<UserVO>> resultPage() {
        return Result.ok(new PageImpl<>(List.of(new UserVO("r1", 1)), PageRequest.of(0, 10), 1));
    }

    /**
     * 分页自动归一：ResponseEntity 包裹 Page。
     */
    @GetMapping("/demo/response-entity-page")
    public ResponseEntity<Page<UserVO>> responseEntityPage() {
        return ResponseEntity.ok(new PageImpl<>(List.of(new UserVO("e1", 1)), PageRequest.of(0, 10), 1));
    }

    /**
     * 分页自动归一：异步返回 Page，异步完成后仍应归一。
     */
    @GetMapping("/demo/page-async")
    public Callable<Page<UserVO>> pageAsync() {
        return () -> new PageImpl<>(List.of(new UserVO("a1", 1)), PageRequest.of(0, 10), 1);
    }

    /**
     * @NoWrap 豁免：Page 原样输出，不被归一。
     */
    @NoWrap
    @GetMapping("/demo/nowrap-page")
    public Page<UserVO> nowrapPage() {
        return new PageImpl<>(List.of(new UserVO("n1", 1)), PageRequest.of(0, 10), 1);
    }

    /**
     * 游标分页：显式构造 CursorResult，有下一页。
     */
    @GetMapping("/demo/cursor")
    public CursorResult<UserVO> cursor() {
        return CursorResult.of(List.of(new UserVO("c1", 1), new UserVO("c2", 2)), "cursor-2");
    }

    /**
     * 游标分页：末页，nextCursor 为 null。
     */
    @GetMapping("/demo/cursor-last")
    public CursorResult<UserVO> cursorLast() {
        return CursorResult.last(List.of(new UserVO("c3", 3)));
    }

    /**
     * 显式 Result 包裹 CursorResult：非框架分页对象，原样作为 data。
     */
    @GetMapping("/demo/result-cursor")
    public Result<CursorResult<UserVO>> resultCursor() {
        return Result.ok(CursorResult.of(List.of(new UserVO("c4", 4)), "cursor-3"));
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
     * 请求体反序列化路径：不加校验注解，用于覆盖 JSON 反序列化失败分支
     * （类型不匹配透出字段路径、格式错误返回固定文案）。
     */
    @PostMapping("/demo/deserialize")
    public Result<CreateUserRequest> deserialize(@RequestBody CreateUserRequest request) {
        return Result.ok(request);
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
     * 上传超限路径：MockMvc 不经过容器真实的 multipart 解析，无法自然触发
     * MaxUploadSizeExceededException，这里直接抛出以覆盖异常映射分支本身。
     */
    @PostMapping("/demo/upload-oversize")
    public Result<Void> uploadOversize() {
        throw new MaxUploadSizeExceededException(1024);
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
