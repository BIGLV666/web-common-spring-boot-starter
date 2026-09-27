package io.github.biglv666.webcommon.annotation;

import io.github.biglv666.webcommon.web.ErrorCodeEndpointController;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启错误码字典查询端点：在任意 {@code @Configuration} 配置类上标注后，
 * 应用提供 {@code GET /web-common/error-codes} 接口，返回启动期校验通过的
 * 全量错误码字典（内置码 + 业务码，含默认文案与来源枚举，按 code 升序），
 * 供前端、测试与网关对齐错误码。
 *
 * <p>端点默认关闭，标注本注解即开启，无需任何配置；响应同样使用统一
 * {@code Result} 结构。若固定路径与业务路由冲突，或需要自定义输出形态，
 * 可改为自己声明 Controller 注入 {@code ErrorCodeRegistry} 调用
 * {@code descriptors()} 读取字典。</p>
 *
 * <p>使用前提：starter 处于启用状态（{@code web-common.enabled} 未设为
 * {@code false}），否则缺少 {@code ErrorCodeRegistry} Bean，应用启动即失败。</p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ErrorCodeEndpointController.class)
public @interface EnableErrorCodeEndpoint {
}
