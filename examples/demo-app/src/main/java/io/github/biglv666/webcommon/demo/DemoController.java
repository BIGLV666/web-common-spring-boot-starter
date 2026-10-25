package io.github.biglv666.webcommon.demo;

import io.github.biglv666.webcommon.annotation.DefaultErrorCode;
import io.github.biglv666.webcommon.annotation.NoWrap;
import io.github.biglv666.webcommon.exception.BusinessException;
import io.github.biglv666.webcommon.result.CursorResult;
import io.github.biglv666.webcommon.result.ErrorCode;
import io.github.biglv666.webcommon.result.PageResult;
import io.github.biglv666.webcommon.result.Result;
import io.github.biglv666.webcommon.result.ResultCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 示例控制器：覆盖统一返回、自动包装、声明式抛错、自建错误码等特性。
 *
 * <p>注意所有方法都没有手动构造失败返回——业务失败一律抛异常，
 * 由 starter 的全局异常处理器统一包装。</p>
 */
@RestController
@Validated
public class DemoController {

    /**
     * 显式返回 Result：与自动包装两种风格都支持。
     */
    @GetMapping("/explicit")
    public Result<String> explicit() {
        return Result.ok("显式包装");
    }

    /**
     * 自动包装：直接返回业务对象，响应为 {"code":0,"message":"操作成功","data":{...}}。
     */
    @GetMapping("/user/{no}")
    public UserVO user(@PathVariable long no) {
        return new UserVO("张三", no);
    }

    /**
     * 偏移量分页：手动构造 PageResult，data 为 {"total":...,"records":[...]}。
     */
    @GetMapping("/users")
    public PageResult<UserVO> users() {
        return PageResult.of(List.of(new UserVO("张三", 1), new UserVO("李四", 2)), 2);
    }

    /**
     * 偏移量分页自动归一（0.5.0 新增）：直接返回 Spring Data Page，
     * data 自动变成 {total, records, current, size, pages}。
     */
    @GetMapping("/users/spring-page")
    public Page<UserVO> springPage() {
        return new PageImpl<>(List.of(new UserVO("王五", 3), new UserVO("赵六", 4)),
                PageRequest.of(1, 2), 15);
    }

    /**
     * 大分页自动归一：默认 1000 条，可用 size 调到 10000，用于压测归一化
     * （列表拷贝）与序列化开销。
     *
     * @param size 每页条数，限制在 1~10000
     * @return 大分页结果
     */
    @GetMapping("/users/spring-page-large")
    public Page<UserVO> springPageLarge(@RequestParam(defaultValue = "1000") int size) {
        int limit = Math.min(Math.max(size, 1), 10_000);
        List<UserVO> records = java.util.stream.IntStream.range(0, limit)
                .mapToObj(i -> new UserVO("user-" + i, i))
                .toList();
        return new PageImpl<>(records, PageRequest.of(0, limit), 100_000);
    }

    /**
     * 游标分页（0.5.0 新增）：data 为 {records, nextCursor, hasNext}。
     */
    @GetMapping("/users/cursor")
    public CursorResult<UserVO> cursor() {
        return CursorResult.of(List.of(new UserVO("钱七", 5), new UserVO("孙八", 6)), "cursor-2");
    }

    /**
     * 游标分页末页：nextCursor=null、hasNext=false。
     */
    @GetMapping("/users/cursor-last")
    public CursorResult<UserVO> cursorLast() {
        return CursorResult.last(List.of(new UserVO("周九", 7)));
    }

    /**
     * 豁免包装：文件下载、健康检查等接口标注 @NoWrap 原样输出。
     */
    @NoWrap
    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    /**
     * 模板抛错：{} 占位符按顺序填充实参。
     */
    @GetMapping("/stock")
    public Result<Void> stock(@RequestParam int remain) {
        if (remain <= 0) {
            throw BusinessException.of(OrderErrorCode.STOCK_NOT_ENOUGH, "库存不足，剩余 {} 件", remain);
        }
        return Result.ok();
    }

    /**
     * 声明式抛错：领域异常类标注 @DefaultErrorCode，无需继承 BusinessException。
     */
    @GetMapping("/pay")
    public Result<Void> pay() {
        throw new OrderAlreadyPaidException("订单 20260829001 已支付");
    }

    /**
     * 参数校验：校验失败返回 40000 与「字段名: 原因」明细，data 携带结构化明细列表。
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok();
    }

    /**
     * 请求体反序列化：不加校验注解，类型不匹配时 40000 的 message
     * 会附带出错字段路径（如「字段 count 类型不匹配」）。
     */
    @PostMapping("/orders")
    public Result<OrderVO> createOrder(@RequestBody OrderRequest request) {
        return Result.ok(new OrderVO("20260904001", request.count()));
    }

    /**
     * 业务自建错误码枚举：实现 ErrorCode 接口，声明到 web-common.error-codes 参与启动校验。
     */
    public enum OrderErrorCode implements ErrorCode {
        STOCK_NOT_ENOUGH(51001, "库存不足"),
        ORDER_NOT_FOUND(51002, "订单不存在");

        private final int code;
        private final String message;

        OrderErrorCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

    /**
     * 声明式领域异常：多常量枚举需指定 constant。
     */
    @DefaultErrorCode(value = OrderErrorCode.class, constant = "ORDER_NOT_FOUND")
    public static class OrderAlreadyPaidException extends RuntimeException {
        public OrderAlreadyPaidException(String message) {
            super(message);
        }
    }

    /**
     * 业务对象示例。
     */
    public record UserVO(String name, long no) {
    }

    /**
     * 注册请求体。
     */
    public static class RegisterRequest {

        /** 用户名，不能为空 */
        @NotBlank(message = "用户名不能为空")
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    /**
     * 下单请求体：count 为数值字段，用于反序列化类型不匹配演示。
     */
    public record OrderRequest(int count) {
    }

    /**
     * 下单响应业务对象。
     */
    public record OrderVO(String orderNo, int count) {
    }
}
