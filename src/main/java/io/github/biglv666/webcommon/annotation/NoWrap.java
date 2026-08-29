package io.github.biglv666.webcommon.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 豁免自动包装注解：标注在 Controller 方法或类上后，
 * 该接口的返回体不再被 starter 的自动包装（{@code ResultWrapAdvice}）处理，
 * 原样写出。
 *
 * <p>典型适用场景：文件下载、健康检查、需要返回原始 JSON 结构给第三方回调的接口。</p>
 *
 * <p>注意：本注解只影响自动包装，不影响全局异常处理——即使接口豁免了包装，
 * 抛出的异常仍会被 {@code GlobalExceptionHandler} 转换为统一返回体。
 * 若需要彻底自定义错误结构，请自行声明异常处理器 Bean。</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface NoWrap {
}
