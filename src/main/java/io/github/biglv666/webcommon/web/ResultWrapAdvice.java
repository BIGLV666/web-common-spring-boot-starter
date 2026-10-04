package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.annotation.NoWrap;
import io.github.biglv666.webcommon.config.WebCommonProperties;
import io.github.biglv666.webcommon.result.Result;
import io.github.biglv666.webcommon.result.ResultCode;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 响应自动包装通知：Controller 直接返回业务对象，本通知统一包装为
 * {@link Result} 成功结构，接入方无需每个方法手写 {@code Result.ok(...)}。
 *
 * <p>包装规则：</p>
 * <ul>
 *     <li>返回类型已是 {@code Result}（或 {@code ResponseEntity<Result>}）的不重复包装；</li>
 *     <li>方法或类上标注 {@link NoWrap} 的豁免（文件下载、健康检查等）；</li>
 *     <li>返回类型为 void（如某些写操作只回 200 空体）的跳过，不改变空响应语义；</li>
 *     <li>返回 String 时因 Spring 走 {@link StringHttpMessageConverter}，
 *         直接包装对象会被当成字符串内容，需先序列化为 JSON 字符串再交出；
 *         序列化由 {@link ResultJsonWriter} 完成，自动适配 Jackson 2 / Jackson 3
 *         （Spring Boot 4 起默认），本类不直接依赖任何 Jackson 类型。</li>
 * </ul>
 *
 * <p>本通知只包装成功路径；异常统一由 {@link GlobalExceptionHandler} 处理。</p>
 */
@RestControllerAdvice
public class ResultWrapAdvice implements ResponseBodyAdvice<Object> {

    private final ResultJsonWriter jsonWriter;

    private final String successMessage;

    private final WebCommonProperties.TraceId traceId;

    private final LocalizedMessageResolver messageResolver;

    /**
     * 构造自动包装通知。
     *
     * @param jsonWriter      String 返回的 JSON 序列化器，按类路径适配 Jackson 2/3
     * @param successMessage  成功响应文案，取自 {@code web-common.success-message}，作 i18n 回退默认值
     * @param traceId         traceId 配置，启用时成功响应填充 {@code Result.traceId}
     * @param messageResolver 文案解析器，成功文案按 locale 走 i18n
     */
    public ResultWrapAdvice(ResultJsonWriter jsonWriter, String successMessage,
                            WebCommonProperties.TraceId traceId, LocalizedMessageResolver messageResolver) {
        this.jsonWriter = jsonWriter;
        this.successMessage = successMessage;
        this.traceId = traceId;
        this.messageResolver = messageResolver;
    }

    /**
     * 判断该返回值是否需要包装。
     *
     * @param returnType    Controller 方法返回类型
     * @param converterType 将使用的消息转换器
     * @return true 表示进入 {@link #beforeBodyWrite} 包装
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends org.springframework.http.converter.HttpMessageConverter<?>> converterType) {
        if (returnType.getMethod() == null) {
            return false;
        }
        // 异常处理器（本 starter 的或其他库的 @RestControllerAdvice）的返回值不包装：
        // ResponseBodyAdvice 会作用于异常处理方法的响应，若包装会破坏其自定义结构
        // （如 api-governance 的限流 429 响应），异常响应的形态由各自的处理器全权决定
        if (returnType.hasMethodAnnotation(org.springframework.web.bind.annotation.ExceptionHandler.class)) {
            return false;
        }
        // API 文档端点（springdoc / knife4j）不包装：其响应体是文档 JSON 原文，
        // 包装会破坏 swagger-ui 消费，byte[] 形态更会因消息转换器类型校验直接报错
        if (isDocumentationEndpoint(returnType)) {
            return false;
        }
        // 方法或类上声明豁免的接口原样写出
        if (returnType.hasMethodAnnotation(NoWrap.class)
                || returnType.getContainingClass().isAnnotationPresent(NoWrap.class)) {
            return false;
        }
        // void 返回保持空响应语义
        if (returnType.getParameterType() == void.class) {
            return false;
        }
        // 已是 Result 或 ResponseEntity<Result> 的不重复包装
        Type type = unwrapResponseEntity(returnType.getGenericParameterType());
        return !(type instanceof Class<?> clazz && Result.class.isAssignableFrom(clazz));
    }

    /**
     * 将业务对象包装为 Result 成功结构。
     *
     * @param body        Controller 返回的业务对象
     * @param returnType  方法返回类型
     * @param contentType 响应内容类型
     * @param converterType 消息转换器
     * @return 包装后的 Result；String 返回返回 JSON 字符串
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends org.springframework.http.converter.HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 透传时 body 仍可能是 Result（如直接 return Result.ok()），统一兜住
        if (body instanceof Result<?> existing) {
            applyTraceId(existing);
            // 显式 Result.ok() 的默认成功文案同样按 locale 解析，与自动包装路径一致；
            // 业务方显式指定的 message 不动
            applySuccessMessage(existing);
            return body;
        }
        Result<Object> wrapped = Result.ok(body);
        // 自动包装路径按配置取成功文案（i18n 按当前 locale 解析，缺失回退配置值），
        // 不依赖静态默认值，避免多应用同 JVM 场景串配置
        wrapped.setMessage(messageResolver.resolveSuccess(successMessage));
        applyTraceId(wrapped);
        // String 走 StringHttpMessageConverter：包装对象再交出会被 toString 成 {"code":...} 字面量，
        // 必须先序列化成 JSON 字符串；具体实现按类路径适配 Jackson 2/3。
        // 声明类型与解析值类型都要看：异步返回（Callable<String> 等）解析值是 String
        // 而声明类型不是，Accept: text/plain 时选中的正是 String 转换器，漏判会 ClassCastException
        if (StringHttpMessageConverter.class.isAssignableFrom(converterType)
                && (returnType.getParameterType() == String.class || body instanceof String)) {
            return jsonWriter.toJson(wrapped);
        }
        return wrapped;
    }

    /**
     * 判断返回值所属控制器是否为 API 文档端点（springdoc / knife4j）。
     * 这类端点输出文档 JSON 原文供 swagger-ui 消费，不属于业务统一包络的范围。
     *
     * @param returnType Controller 方法返回参数
     * @return true 表示文档端点，跳过包装
     */
    private boolean isDocumentationEndpoint(MethodParameter returnType) {
        String className = returnType.getContainingClass().getName();
        return className.startsWith("org.springdoc.")
                || className.startsWith("com.github.xiaoymin.knife4j.");
    }

    /**
     * 按配置填充 traceId：功能关闭或 MDC 无值时不填充，字段保持 null 不参与序列化；
     * 已有值（显式 {@code setTraceId}）不覆盖。
     *
     * @param result 待写出的结果
     */
    private void applyTraceId(Result<?> result) {
        if (traceId.isEnabled() && result.getTraceId() == null) {
            result.setTraceId(TraceIds.current(traceId.getMdcKey()));
        }
    }

    /**
     * 显式 {@code Result.ok()} 的默认成功文案按 locale 解析（i18n）：
     * 仅当成功码且 message 恰为静态默认文案时替换，业务方显式指定的文案不动。
     *
     * @param result 待写出的成功结果
     */
    private void applySuccessMessage(Result<?> result) {
        if (result.getCode() == ResultCode.SUCCESS.getCode()
                && Result.getDefaultSuccessMessage().equals(result.getMessage())) {
            result.setMessage(messageResolver.resolveSuccess(successMessage));
        }
    }

    /**
     * 解开 ResponseEntity 的泛型壳，取出真实业务类型用于重复包装判断。
     *
     * @param type Controller 声明的返回泛型
     * @return ResponseEntity 内的真实类型；非 ResponseEntity 原样返回
     */
    private Type unwrapResponseEntity(Type type) {
        if (type instanceof ParameterizedType parameterized
                && parameterized.getRawType() instanceof Class<?> raw
                && org.springframework.http.ResponseEntity.class.isAssignableFrom(raw)
                && parameterized.getActualTypeArguments().length == 1) {
            return parameterized.getActualTypeArguments()[0];
        }
        return type;
    }
}
