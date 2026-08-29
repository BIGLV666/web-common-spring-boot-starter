package io.github.biglv666.webcommon.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一返回体，所有对外接口的响应均使用本结构。
 *
 * <p>响应结构固定为三段：</p>
 * <pre>{@code
 * {
 *     "code": 0,          // 错误码，0 表示成功，其余见 ErrorCode
 *     "message": "操作成功", // 提示信息，成功时为固定文案，失败时为可读错误描述
 *     "data": {...}        // 业务数据，失败时为 null（序列化时省略）
 * }
 * }</pre>
 *
 * <p>HTTP 状态码恒为 200，成败以 {@link #code} 是否为 0 判定，
 * 前端只需按统一结构解析一种失败路径。</p>
 *
 * <p>Controller 中通常无需手动构造：开启自动包装（默认开启）时直接返回业务对象即可；
 * 显式构造应通过静态工厂方法 {@code Result.ok(...)} / {@code Result.fail(...)}。
 * 启用自动包装后本类字段上的 {@code @Schema} 描述会出现在 springdoc/knife4j 生成的文档中。</p>
 *
 * @param <T> 业务数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "统一返回体：HTTP 状态码恒为 200，成败以 code 是否为 0 判定")
public class Result<T> {

    /** 成功文案默认值，可通过 {@code web-common.success-message} 或 {@link #setDefaultSuccessMessage} 调整 */
    public static final String SUCCESS_MESSAGE_DEFAULT = "操作成功";

    /**
     * 成功响应的静态默认文案。静态字段为 JVM 级全局状态，
     * 由自动装配按 {@code web-common.success-message} 在启动时写入；
     * 同 JVM 部署多个使用不同配置的应用时（罕见，如老式 war 同宿主部署）不要依赖该默认值，
     * 应逐实例 {@link #setMessage} 指定。
     */
    private static volatile String defaultSuccessMessage = SUCCESS_MESSAGE_DEFAULT;

    /** 错误码：0 表示成功，其余取值见 {@link ErrorCode} 及其各实现 */
    @Schema(description = "错误码：0 成功；4xxxx 客户端侧问题；5xxxx 业务侧问题；500 系统兜底",
            example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private int code;

    /** 提示信息，面向调用方可读，不得包含堆栈、内部路径等敏感信息 */
    @Schema(description = "提示信息，成功时为固定文案，失败时为可读错误描述",
            example = "操作成功", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    /** 业务数据，失败时为 null 且不参与 JSON 序列化 */
    @Schema(description = "业务数据，失败时省略该字段")
    private T data;

    public Result() {
    }

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 构造无数据的成功返回，message 取静态默认成功文案。
     *
     * @param <T> 业务数据类型
     * @return code=0 的成功结果，data 为 null
     */
    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.SUCCESS.getCode(), defaultSuccessMessage, null);
    }

    /**
     * 构造携带业务数据的成功返回，message 取静态默认成功文案。
     *
     * @param data 业务数据，允许为 null（此时与 {@link #ok()} 等价）
     * @param <T>  业务数据类型
     * @return code=0 的成功结果
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), defaultSuccessMessage, data);
    }

    /**
     * 调整成功响应的静态默认文案。
     * 自动装配会按 {@code web-common.success-message} 在启动时调用本方法；
     * 业务代码一般无需直接调用，改单个实例用 {@link #setMessage}。
     *
     * @param message 新的成功默认文案，null 视为恢复内置默认值
     */
    public static void setDefaultSuccessMessage(String message) {
        defaultSuccessMessage = message != null ? message : SUCCESS_MESSAGE_DEFAULT;
    }

    /**
     * 读取当前成功响应静态默认文案。
     *
     * @return 当前默认文案
     */
    public static String getDefaultSuccessMessage() {
        return defaultSuccessMessage;
    }

    /**
     * 构造失败返回，提示信息取错误码的默认文案。
     *
     * @param errorCode 错误码，不允许为 null
     * @param <T>       业务数据类型
     * @return 携带错误码与默认提示的失败结果
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 构造失败返回，使用自定义提示覆盖错误码默认文案。
     *
     * <p>适用于业务方希望给出更具体提示的场景，如
     * {@code fail(ResultCode.CONFLICT, "用户名已被注册")}。</p>
     *
     * @param errorCode 错误码，不允许为 null
     * @param message   自定义提示信息，覆盖错误码默认文案
     * @param <T>       业务数据类型
     * @return 携带错误码与自定义提示的失败结果
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    /**
     * 构造失败返回，直接指定错误码数值与提示。
     *
     * <p>主要供全局异常处理器在错误码语义明确时使用，
     * 业务代码应优先使用 {@link #fail(ErrorCode)} 系列方法。</p>
     *
     * @param code    数字错误码
     * @param message 提示信息
     * @param <T>     业务数据类型
     * @return 携带指定错误码与提示的失败结果
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
