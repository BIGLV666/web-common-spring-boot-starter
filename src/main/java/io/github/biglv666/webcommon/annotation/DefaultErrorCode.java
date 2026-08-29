package io.github.biglv666.webcommon.annotation;

import io.github.biglv666.webcommon.result.ErrorCode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式错误码注解：标注在自定义异常类上，声明该异常应映射到的错误码。
 *
 * <p>业务方往往已有一批继承 {@code RuntimeException} 的领域异常
 * （如 {@code StockNotEnoughException}），逐个改为继承
 * {@code BusinessException} 成本高。此时直接在异常类上标注本注解，
 * 全局异常处理器就会把它当作业务异常处理：</p>
 *
 * <pre>{@code
 * // 单常量枚举：只写枚举类即可
 * @DefaultErrorCode(OrderErrorCode.class)
 * public class StockNotEnoughException extends RuntimeException { ... }
 *
 * // 多常量枚举：需指定常量名
 * @DefaultErrorCode(value = OrderErrorCode.class, constant = "STOCK_NOT_ENOUGH")
 * public class StockNotEnoughException extends RuntimeException { ... }
 *
 * // Service 中照常抛出，无需感知 starter
 * throw new StockNotEnoughException("库存不足，剩余 3 件");
 * }</pre>
 *
 * <p>映射规则：响应 code 取解析出的错误码；message 取异常自身的
 * {@code getMessage()}，为空时回退到错误码默认文案。
 * 解析失败（枚举无此常量、多常量枚举未指定 constant）会抛出
 * {@link IllegalStateException}，在开发期即可暴露，不会静默降级为系统错误。</p>
 *
 * <p>标注了 {@code @Inherited}，注解在异常的父类上同样生效；
 * 异常类继承链上若有多处标注，取离异常类最近的一处。</p>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultErrorCode {

    /**
     * 错误码枚举类。Java 注解成员不支持接口类型，故持有枚举 Class
     * 而非 {@link ErrorCode} 实例，配合 {@link #constant()} 在运行期解析。
     *
     * @return 实现了 {@link ErrorCode} 的枚举类
     */
    Class<? extends ErrorCode> value();

    /**
     * 枚举常量名。枚举只有一个常量时可省略；
     * 多常量枚举必须指定，否则解析时抛出 {@link IllegalStateException}。
     *
     * @return 枚举常量名，默认空串表示未指定
     */
    String constant() default "";
}
