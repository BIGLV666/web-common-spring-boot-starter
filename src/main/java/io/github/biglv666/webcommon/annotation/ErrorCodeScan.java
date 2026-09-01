package io.github.biglv666.webcommon.annotation;

import io.github.biglv666.webcommon.web.ErrorCodeScanRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 错误码扫描注册注解：标注在任意 {@code @Configuration} 配置类上，
 * 按包扫描实现 {@link io.github.biglv666.webcommon.result.ErrorCode} 的枚举并自动注册，
 * 替代逐个填写 {@code web-common.error-codes} 字符串类名的方式。
 *
 * <pre>{@code
 * @Configuration
 * @ErrorCodeScan("com.example.order")   // 默认扫描本包时可省略 basePackages
 * public class WebConfig { }
 * }</pre>
 *
 * <p>扫描到的枚举与 {@code web-common.error-codes} 配置声明的枚举合并后
 * 走同一套启动期校验（重复码、越段码、保留值），冲突仍然启动失败，
 * 两种注册方式可并存。扫描结果为空不报错，便于多模块按需开启。</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(ErrorCodeScanRegistrar.class)
public @interface ErrorCodeScan {

    /**
     * 扫描的基础包名列表，不得与 {@link #basePackageClasses()} 同时为空
     * （两者均为空时回退到标注类的所在包）。
     *
     * @return 基础包名，空串元素会被忽略
     */
    String[] basePackages() default {};

    /**
     * 以类为锚点指定扫描的基础包（取类所在包），替代硬编码包名字符串。
     *
     * @return 锚点类
     */
    Class<?>[] basePackageClasses() default {};
}
