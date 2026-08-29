package io.github.biglv666.webcommon.result;

/**
 * 内置通用错误码枚举，覆盖统一返回与全局异常处理的默认场景。
 *
 * <p>各错误码按分段约定划分（见 {@link ErrorCode}），每个常量的 Javadoc
 * 标注了它的适用场景；业务方选择错误码时应按语义对号入座，而不是
 * 一律抛 {@link #BIZ_ERROR}。</p>
 *
 * <p>当内置码无法覆盖业务语义时，在业务模块内新建枚举实现
 * {@link ErrorCode} 自行扩展，本枚举保持只增不改，保证版本兼容。</p>
 */
public enum ResultCode implements ErrorCode {

    /**
     * 使用场景：请求正常处理并成功返回。
     * 是唯一表示成功的码，HTTP 状态恒为 200，code=0，前端以 code 判定成败。
     */
    SUCCESS(0, "操作成功"),

    /**
     * 使用场景：请求参数不合法。
     * 典型来源有三类：{@code @Valid} 注解在请求体 DTO 上校验失败、
     * {@code @Validated} 注解在 {@code @RequestParam}/{@code @PathVariable}
     * 上校验失败、请求体 JSON 反序列化失败（如格式错误、类型不匹配）。
     * 响应 message 会携带「字段名: 原因」明细，前端据此定位到具体字段。
     */
    PARAM_ERROR(40000, "参数错误"),

    /**
     * 使用场景：未登录或登录凭证已失效。
     * 由鉴权层（如拦截器、过滤器解析 token 失败）抛出，
     * 与 {@link #FORBIDDEN} 的区别是「还没证明你是谁」。
     */
    UNAUTHORIZED(40100, "未登录或登录已过期"),

    /**
     * 使用场景：已登录但权限不足，或访问了不属于当前用户的资源。
     * 与 {@link #UNAUTHORIZED} 的区别是「知道你是谁，但你没资格」。
     * 资源归属校验必须以服务端当前登录态为准，不得信任客户端提交的用户标识。
     */
    FORBIDDEN(40300, "无访问权限"),

    /**
     * 使用场景：按查询条件找不到目标资源。
     * 如根据 id 查询记录不存在、操作已被删除的数据。
     */
    NOT_FOUND(40400, "资源不存在"),

    /**
     * 使用场景：资源状态冲突。
     * 如重复提交创建请求、唯一性约束冲突（用户名/手机号已注册）、
     * 状态机不允许的迁移（对已支付订单重复支付）。
     */
    CONFLICT(40900, "资源状态冲突"),

    /**
     * 使用场景：业务规则校验失败、业务流程无法继续的通用兜底码。
     * 如库存不足、余额不够、活动已结束等「请求合法但业务上做不了」的情况。
     * 内置码无法精确表达业务语义时应新建业务枚举，而不是复用本码。
     */
    BIZ_ERROR(50000, "业务处理失败"),

    /**
     * 使用场景：下游服务或中间件不可用。
     * 如调用第三方接口超时、消息队列/缓存连接失败后的降级返回。
     */
    SERVICE_UNAVAILABLE(50300, "下游服务不可用"),

    /**
     * 使用场景：未被识别的运行时异常兜底。
     * 由全局异常处理器的兜底分支统一返回，code 固定为 500。
     * 响应 message 固定为「系统繁忙」，不透出任何内部异常细节，
     * 堆栈只记录在服务端日志中，防止泄露内部实现信息。
     */
    SYSTEM_ERROR(500, "系统繁忙，请稍后重试");

    private final int code;

    private final String message;

    ResultCode(int code, String message) {
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
