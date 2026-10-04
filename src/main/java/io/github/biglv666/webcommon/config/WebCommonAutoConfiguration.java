package io.github.biglv666.webcommon.config;

import io.github.biglv666.webcommon.result.ErrorCode;
import io.github.biglv666.webcommon.result.Result;
import io.github.biglv666.webcommon.web.ErrorCodeOpenApiCustomizer;
import io.github.biglv666.webcommon.web.ErrorCodeRegistry;
import io.github.biglv666.webcommon.web.ExceptionMetrics;
import io.github.biglv666.webcommon.web.GlobalExceptionHandler;
import io.github.biglv666.webcommon.web.LocalizedMessageResolver;
import io.github.biglv666.webcommon.web.MicrometerExceptionMetrics;
import io.github.biglv666.webcommon.web.ResultJsonWriterFactory;
import io.github.biglv666.webcommon.web.ResultWrapAdvice;
import io.github.biglv666.webcommon.web.ScannedErrorCodes;
import io.github.biglv666.webcommon.web.TraceIdFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;

/**
 * starter 自动装配入口，由
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 注册，使用方引入依赖即生效，无需任何配置。
 *
 * <p>装配条件：</p>
 * <ul>
 *     <li>仅 Web（Servlet）应用装配，非 Web 环境跳过；</li>
 *     <li>{@code web-common.enabled=false} 时整体关闭，默认开启；</li>
 *     <li>{@code web-common.auto-wrap=false} 可单独关闭响应自动包装。</li>
 * </ul>
 *
 * <p>使用方自行声明同类型 Bean 即可覆盖对应默认实现。</p>
 *
 * <p>{@code after} 以字符串声明 Jackson 自动配置类名：Boot 3.x 的包是
 * {@code autoconfigure.jackson}，Boot 4 起迁移为 {@code jackson.autoconfigure}，
 * 引用 Class 字面量会在 Boot 4 下直接编译失败；字符串名仅影响排序，
 * 两代均通过名称过滤生效，不存在的名字被忽略。</p>
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration",
        "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
        "org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration"})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(WebCommonProperties.class)
public class WebCommonAutoConfiguration {

    /**
     * 注册文案解析器，错误码默认文案与成功文案按 locale 走 MessageSource（i18n）。
     * key 缺失即回退默认文案，容器无 MessageSource 时全部回退，零配置零影响。
     * 装配顺序排在 MessageSource 自动配置之后（Boot 4 中该配置类若已迁移包名，
     * 名称过滤自动忽略，实例化期按需解析不受影响）。
     *
     * @param messageSource 容器中的 MessageSource（Boot 默认提供或使用方自定义），可为空
     * @param properties    starter 配置项
     * @return 文案解析器实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "web-common", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LocalizedMessageResolver messageResolver(ObjectProvider<MessageSource> messageSource,
                                                    WebCommonProperties properties) {
        return new LocalizedMessageResolver(messageSource.getIfAvailable(), properties.getI18n().isEnabled());
    }

    /**
     * Micrometer 异常指标装配：使用方类路径存在 micrometer 时注册
     * {@link MicrometerExceptionMetrics}（计数器 web.common.exceptions），
     * 使用方自定义 {@code ExceptionMetrics} Bean 时让位；无 micrometer 时
     * 本嵌套配置整体跳过，异常处理器的指标回调为空引用、零开销。
     * registry 由 ObjectProvider 惰性解析：未引入 actuator 等提供 registry
     * 的场景同样静默跳过。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnProperty(prefix = "web-common", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class MicrometerMetricsConfiguration {

        /**
         * 注册 Micrometer 异常指标。
         *
         * @param registryProvider Micrometer 注册器（通常来自 actuator），可为空
         * @return 指标实现
         */
        @Bean
        @ConditionalOnMissingBean(ExceptionMetrics.class)
        public ExceptionMetrics exceptionMetrics(ObjectProvider<MeterRegistry> registryProvider) {
            return new MicrometerExceptionMetrics(registryProvider.getIfAvailable());
        }
    }

    /**
     * 注册全局异常处理器。
     *
     * @param properties      starter 配置项
     * @param messageResolver 文案解析器
     * @param metricsProvider 异常指标，容器无实现时为 null（无 micrometer 场景）
     * @return 按 {@code expose-exception-message} 与日志级别配置构造的处理器实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "web-common", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GlobalExceptionHandler globalExceptionHandler(WebCommonProperties properties,
                                                         LocalizedMessageResolver messageResolver,
                                                         ObjectProvider<ExceptionMetrics> metricsProvider) {
        return new GlobalExceptionHandler(properties, messageResolver, metricsProvider.getIfAvailable());
    }

    /**
     * 注册响应自动包装通知，Controller 直接返回业务对象即被包装为 Result。
     * String 返回的 JSON 序列化经 {@link ResultJsonWriterFactory} 按类路径
     * 自动适配 Jackson 2（Boot 3.x 默认）与 Jackson 3（Boot 4 默认），
     * 并尽量复用使用方容器的 Mapper 配置。本装配方法签名不携带任何
     * Jackson 类型（含泛型引用），保证纯 Jackson 3 环境的 Bean 方法
     * 依赖解析不会触发类加载。
     *
     * @param beanFactory Bean 工厂，供按类名延迟解析两代 Jackson Mapper
     * @param properties  starter 配置项
     * @return 自动包装通知实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "web-common", name = {"enabled", "auto-wrap"},
            havingValue = "true", matchIfMissing = true)
    public ResultWrapAdvice resultWrapAdvice(ConfigurableListableBeanFactory beanFactory,
                                             WebCommonProperties properties,
                                             LocalizedMessageResolver messageResolver) {
        return new ResultWrapAdvice(
                ResultJsonWriterFactory.create(beanFactory),
                properties.getSuccessMessage(),
                properties.getTraceId(),
                messageResolver);
    }

    /**
     * 注册 traceId 过滤器，仅在 {@code web-common.trace-id.enabled=true} 时装配。
     * 顺序取最低优先级（最后执行），保证使用方链路追踪组件（通常为最高优先级）
     * 先写入 MDC，本过滤器读到已有值时原样透传；未接入链路追踪时才自行生成。
     *
     * @param properties starter 配置项
     * @return traceId 过滤器注册
     */
    @Bean
    @ConditionalOnProperty(prefix = "web-common", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(prefix = "web-common.trace-id", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter(WebCommonProperties properties) {
        WebCommonProperties.TraceId traceId = properties.getTraceId();
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(
                new TraceIdFilter(traceId.getMdcKey(), traceId.getResponseHeader()));
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        return registration;
    }

    /**
     * 将 {@code web-common.success-message} 应用为 {@code Result} 的静态默认成功文案，
     * 使显式 {@code Result.ok(...)} 调用也使用配置文案。
     * 每个应用上下文启动时各自应用一次，上下文之间互不残留。
     *
     * @param properties starter 配置项
     * @return 启动时应用静态默认值的初始化器
     */
    @Bean
    @ConditionalOnProperty(prefix = "web-common", name = "enabled", havingValue = "true", matchIfMissing = true)
    public InitializingBean resultDefaultsInitializer(WebCommonProperties properties) {
        return () -> Result.setDefaultSuccessMessage(properties.getSuccessMessage());
    }
    /**
     * 注册错误码注册器，对内置码与业务方声明的错误码枚举做启动期冲突校验。
     * 枚举来源有两处：{@code web-common.error-codes} 字符串配置，以及
     * {@code @ErrorCodeScan} 扫描注册的 {@link ScannedErrorCodes} Bean，两者合并后统一校验。
     *
     * @param properties      starter 配置项
     * @param scannedProvider 容器内所有 {@code @ErrorCodeScan} 的扫描结果
     * @return 错误码注册器实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "web-common", name = "enabled", havingValue = "true", matchIfMissing = true)
    @SuppressWarnings("unchecked")
    public ErrorCodeRegistry errorCodeRegistry(WebCommonProperties properties,
                                               ObjectProvider<ScannedErrorCodes> scannedProvider)
            throws ClassNotFoundException {
        List<Class<? extends ErrorCode>> enumClasses = new ArrayList<>();
        for (String className : properties.getErrorCodes()) {
            // 类名写错立即失败，而不是静默跳过造成「以为校验了其实没有」
            enumClasses.add((Class<? extends ErrorCode>) Class.forName(className));
        }
        scannedProvider.orderedStream()
                .map(ScannedErrorCodes::getEnumClasses)
                .forEach(enumClasses::addAll);
        return new ErrorCodeRegistry(enumClasses);
    }

    /**
     * 错误码字典 OpenAPI 扩展装配：使用方工程存在 springdoc 时，把错误码字典
     * 以 {@code x-error-codes} 扩展挂进 swagger 文档根节点。按类名条件判断
     * （不加载类，避免无 springdoc 时 NoClassDefFoundError），无 springdoc 零影响。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springdoc.core.customizers.OpenApiCustomizer")
    @ConditionalOnProperty(prefix = "web-common", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ErrorCodeOpenApiConfiguration {

        /**
         * 注册错误码字典 OpenAPI 扩展。
         *
         * @param registry 错误码注册器
         * @return 扩展实例，使用方可声明同类型 Bean 覆盖
         */
        @Bean
        @ConditionalOnMissingBean
        public OpenApiCustomizer errorCodeOpenApiCustomizer(ErrorCodeRegistry registry) {
            return new ErrorCodeOpenApiCustomizer(registry);
        }
    }
}
