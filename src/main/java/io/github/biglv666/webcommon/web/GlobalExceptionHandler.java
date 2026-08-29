package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.annotation.DefaultErrorCode;
import io.github.biglv666.webcommon.config.WebCommonProperties;
import io.github.biglv666.webcommon.exception.BusinessException;
import io.github.biglv666.webcommon.result.ErrorCode;
import io.github.biglv666.webcommon.result.Result;
import io.github.biglv666.webcommon.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 全局异常处理器，将各类异常统一转换为 {@link Result} 失败结构。
 *
 * <p>处理分支按优先级覆盖四类异常：</p>
 * <ol>
 *     <li>{@link BusinessException}：业务代码主动抛出，code 取异常携带的错误码；</li>
 *     <li>参数类异常：{@code @Valid}/{@code @Validated} 校验失败、JSON 反序列化失败、
 *         请求方法/参数/类型错误等，统一返回 {@link ResultCode#PARAM_ERROR}，
 *         message 拼接「字段名: 原因」明细；</li>
 *     <li>{@link NoHandlerFoundException}：路径不存在，返回 {@link ResultCode#NOT_FOUND}；</li>
 *     <li>{@link Exception} 兜底：先识别异常类上的 {@link DefaultErrorCode} 注解
 *         （声明式业务异常），命中则按业务异常处理；未命中则统一返回
 *         {@link ResultCode#SYSTEM_ERROR}，堆栈只记录在服务端日志，
 *         响应体不透出任何内部细节，防止信息泄露。</li>
 * </ol>
 *
 * <p>所有响应的 HTTP 状态码恒为 200，成败由 code 字段区分。
 * 各分支日志级别可通过 {@code web-common.log.*} 配置调整，日志均携带请求 URI。</p>
 *
 * <p>本类由自动装配注册为 Bean（可通过 {@code web-common.enabled=false} 关闭），
 * 业务方也可自行声明 {@code GlobalExceptionHandler} 类型的 Bean 覆盖默认实现。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final WebCommonProperties properties;

    /**
     * 构造全局异常处理器。
     *
     * @param properties starter 配置项，决定日志级别与兜底消息透出开关
     */
    public GlobalExceptionHandler(WebCommonProperties properties) {
        this.properties = properties;
    }

    /**
     * 业务异常分支：使用业务代码抛出时携带的错误码与提示构造失败返回。
     *
     * @param e       业务异常
     * @param request 当前请求，用于日志定位
     * @return code 取 {@code e.getErrorCode().getCode()} 的失败结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log(properties.getLog().getBusinessLevel(), false,
                "业务异常: uri={}, code={}, message={}", request.getRequestURI(), errorCode.getCode(), e.getMessage());
        return Result.fail(errorCode.getCode(), e.getMessage());
    }

    /**
     * 请求体校验失败分支：{@code @Valid} 注解在 {@code @RequestBody} 上校验失败时触发。
     * 响应 message 拼接所有未通过字段的「字段名: 原因」，便于前端定位。
     *
     * @param e       校验异常
     * @param request 当前请求
     * @return code=PARAM_ERROR 的失败结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log(properties.getLog().getParamLevel(), false,
                "请求体校验失败: uri={}, detail={}", request.getRequestURI(), detail);
        return Result.fail(ResultCode.PARAM_ERROR, detail);
    }

    /**
     * 表单绑定校验失败分支：{@code @Valid} 注解在非请求体（如表单提交）上校验失败时触发。
     *
     * @param e       绑定异常
     * @param request 当前请求
     * @return code=PARAM_ERROR 的失败结果
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String detail = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log(properties.getLog().getParamLevel(), false,
                "参数绑定校验失败: uri={}, detail={}", request.getRequestURI(), detail);
        return Result.fail(ResultCode.PARAM_ERROR, detail);
    }

    /**
     * 单参数校验失败分支：类上标注 {@code @Validated} 时，
     * {@code @RequestParam}/{@code @PathVariable} 上的约束注解校验失败触发。
     *
     * @param e       约束校验异常
     * @param request 当前请求
     * @return code=PARAM_ERROR 的失败结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        String detail = e.getConstraintViolations().stream()
                .map(violation -> lastPathNode(violation) + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        log(properties.getLog().getParamLevel(), false,
                "单参数校验失败: uri={}, detail={}", request.getRequestURI(), detail);
        return Result.fail(ResultCode.PARAM_ERROR, detail);
    }

    /**
     * 请求体不可读分支：JSON 格式错误、请求体类型不匹配等反序列化失败时触发。
     *
     * @param e       不可读异常
     * @param request 当前请求
     * @return code=PARAM_ERROR 的失败结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log(properties.getLog().getParamLevel(), true, "请求体解析失败: uri={}", request.getRequestURI(), e);
        return Result.fail(ResultCode.PARAM_ERROR, "请求体格式错误");
    }

    /**
     * 请求基础设施类错误分支：请求方法不支持、参数缺失、类型转换失败、媒体类型不支持、
     * 异步请求超时等由 Spring MVC 判定的客户端侧错误，统一归入参数错误。
     *
     * @param e       Spring MVC 请求处理异常
     * @param request 当前请求
     * @return code=PARAM_ERROR 的失败结果
     */
    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            ServletRequestBindingException.class,
            TypeMismatchException.class,
            ConversionNotSupportedException.class,
            MissingPathVariableException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class,
            AsyncRequestTimeoutException.class
    })
    public Result<Void> handleMvcClientError(Exception e, HttpServletRequest request) {
        log(properties.getLog().getParamLevel(), false,
                "请求参数或方式错误: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR, e.getMessage());
    }

    /**
     * 路径不存在分支：未匹配到任何处理器时触发。
     *
     * @param e       无处理器异常
     * @param request 当前请求
     * @return code=NOT_FOUND 的失败结果
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e, HttpServletRequest request) {
        log(properties.getLog().getParamLevel(), false,
                "请求路径不存在: uri={}", request.getRequestURI());
        return Result.fail(ResultCode.NOT_FOUND, ResultCode.NOT_FOUND.getMessage());
    }

    /**
     * 兜底分支：未被上述分支捕获的一切异常。
     * 先沿异常类层级向上查找 {@link DefaultErrorCode} 注解，命中则按业务异常处理；
     * 未命中则堆栈完整记录在服务端 error 日志用于排查，响应体固定返回
     * {@link ResultCode#SYSTEM_ERROR}，不泄露堆栈、类名、内部路径等信息。
     * 仅当 {@code web-common.expose-exception-message=true}（联调开关）时
     * 才将原始异常消息透出，生产环境必须保持关闭。
     *
     * @param e       未识别异常
     * @param request 当前请求
     * @return code=SYSTEM_ERROR 或注解声明错误码的失败结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        DefaultErrorCode declared = findDeclaredErrorCode(e.getClass());
        if (declared != null) {
            // 声明式业务异常：message 取异常自身文案，为空回退错误码默认文案
            ErrorCode errorCode = resolveDeclaredErrorCode(declared);
            String message = e.getMessage() != null ? e.getMessage() : errorCode.getMessage();
            log(properties.getLog().getBusinessLevel(), false,
                    "声明式业务异常: uri={}, code={}, message={}",
                    request.getRequestURI(), errorCode.getCode(), message);
            return Result.fail(errorCode.getCode(), message);
        }
        log(properties.getLog().getSystemLevel(), true, "未捕获系统异常: uri={}", request.getRequestURI(), e);
        String message = properties.isExposeExceptionMessage()
                ? e.getMessage()
                : ResultCode.SYSTEM_ERROR.getMessage();
        return Result.fail(ResultCode.SYSTEM_ERROR.getCode(), message);
    }

    /**
     * 解析 {@link DefaultErrorCode} 注解声明的错误码实例：
     * 指定 constant 时按常量名取值；未指定且枚举仅有一个常量时直接使用；
     * 多常量枚举未指定 constant 视为声明错误，抛出异常在开发期暴露。
     *
     * @param annotation 命中的注解
     * @return 解析出的错误码实例
     */
    @SuppressWarnings("unchecked")
    private ErrorCode resolveDeclaredErrorCode(DefaultErrorCode annotation) {
        Class<? extends ErrorCode> enumClass = annotation.value();
        if (!enumClass.isEnum()) {
            throw new IllegalStateException(
                    "@DefaultErrorCode.value 必须是枚举类: " + enumClass.getName());
        }
        if (!annotation.constant().isEmpty()) {
            try {
                // Enum.valueOf 泛型签名要求 Class<T extends Enum<T>>，此处经 raw 类型中转后强转回 ErrorCode
                @SuppressWarnings({"unchecked", "rawtypes"})
                Enum<?> constant = Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), annotation.constant());
                return (ErrorCode) constant;
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException(String.format(
                        "@DefaultErrorCode 常量不存在: %s 中没有常量 %s",
                        enumClass.getName(), annotation.constant()), ex);
            }
        }
        ErrorCode[] constants = enumClass.getEnumConstants();
        if (constants.length == 1) {
            return constants[0];
        }
        throw new IllegalStateException(String.format(
                "@DefaultErrorCode 用于多常量枚举 %s 时必须指定 constant", enumClass.getName()));
    }

    /**
     * 沿异常类层级向上查找最近的 {@link DefaultErrorCode} 注解。
     *
     * @param exceptionClass 异常具体类型
     * @return 命中的注解；整个层级未标注时返回 null
     */
    private DefaultErrorCode findDeclaredErrorCode(Class<?> exceptionClass) {
        Class<?> current = exceptionClass;
        while (current != null && current != Object.class) {
            DefaultErrorCode annotation = current.getAnnotation(DefaultErrorCode.class);
            if (annotation != null) {
                return annotation;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * 按配置级别写异常日志：模板参数中的最后一个元素若为 Throwable 且 withStack=true，
     * 将作为异常堆栈交给 slf4j 输出；OFF/FATAL 级别静默跳过。
     *
     * @param level     配置的日志级别
     * @param withStack 是否要求附带异常堆栈（调用方需将异常作为最后一个参数传入）
     * @param message   日志模板
     * @param args      模板参数，withStack=true 时最后一个元素为异常
     */
    private void log(LogLevel level, boolean withStack, String message, Object... args) {
        if (level == LogLevel.OFF || level == LogLevel.FATAL) {
            return;
        }
        // 从参数尾部提取异常，slf4j 会把 varargs 末尾的 Throwable 当堆栈输出
        Throwable throwable = withStack && args.length > 0 && args[args.length - 1] instanceof Throwable t
                ? t
                : null;
        Object[] logArgs = throwable != null ? Arrays.copyOf(args, args.length - 1) : args;
        switch (level) {
            case TRACE -> { if (log.isTraceEnabled()) log.trace(message, logArgs); }
            case DEBUG -> { if (log.isDebugEnabled()) log.debug(message, logArgs); }
            case INFO -> { if (log.isInfoEnabled()) log.info(message, logArgs); }
            case WARN -> { if (log.isWarnEnabled()) log.warn(message, logArgs); }
            case ERROR -> { if (log.isErrorEnabled()) log.error(message, logArgs); }
            default -> { /* 其余级别不输出 */ }
        }
    }

    /**
     * 取约束违规路径的最后一段（通常是参数名），用于单参数校验失败明细。
     *
     * @param violation 约束违规项
     * @return 参数名
     */
    private String lastPathNode(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int index = path.lastIndexOf('.');
        return index >= 0 ? path.substring(index + 1) : path;
    }
}
