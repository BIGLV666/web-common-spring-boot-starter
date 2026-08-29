package io.github.biglv666.webcommon.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.biglv666.webcommon.annotation.NoWrap;
import io.github.biglv666.webcommon.result.Result;
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
 *         直接包装对象会被当成字符串内容，此处手动序列化为 JSON 字符串再交出。</li>
 * </ul>
 *
 * <p>本通知只包装成功路径；异常统一由 {@link GlobalExceptionHandler} 处理。</p>
 */
@RestControllerAdvice
public class ResultWrapAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    private final String successMessage;

    /**
     * 构造自动包装通知。
     *
     * @param objectMapper   Spring MVC 默认注册的 Jackson ObjectMapper，仅用于 String 返回的手动序列化
     * @param successMessage 成功响应文案，取自 {@code web-common.success-message}
     */
    public ResultWrapAdvice(ObjectMapper objectMapper, String successMessage) {
        this.objectMapper = objectMapper;
        this.successMessage = successMessage;
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
        if (body instanceof Result<?>) {
            return body;
        }
        Result<Object> wrapped = Result.ok(body);
        // 自动包装路径按配置取成功文案，不依赖静态默认值，避免多应用同 JVM 场景串配置
        wrapped.setMessage(successMessage);
        // String 走 StringHttpMessageConverter：包装对象再交出会被 toString 成 {"code":...} 字面量，
        // 必须手动序列化成 JSON 字符串
        if (returnType.getParameterType() == String.class
                && StringHttpMessageConverter.class.isAssignableFrom(converterType)) {
            try {
                return objectMapper.writeValueAsString(wrapped);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalStateException("String 返回值包装序列化失败", e);
            }
        }
        return wrapped;
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
