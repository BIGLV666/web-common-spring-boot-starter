package io.github.biglv666.webcommon.config;

import io.github.biglv666.webcommon.result.ErrorCode;
import io.github.biglv666.webcommon.result.Result;
import io.github.biglv666.webcommon.web.ErrorCodeRegistry;
import io.github.biglv666.webcommon.web.GlobalExceptionHandler;
import io.github.biglv666.webcommon.web.ResultJsonWriterFactory;
import io.github.biglv666.webcommon.web.ResultWrapAdvice;
import io.github.biglv666.webcommon.web.ScannedErrorCodes;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(WebCommonProperties.class)
public class WebCommonAutoConfiguration {

    /**
     * 注册全局异常处理器。
     *
     * @param properties starter 配置项
     * @return 按 {@code expose-exception-message} 与日志级别配置构造的处理器实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "web-common", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GlobalExceptionHandler globalExceptionHandler(WebCommonProperties properties) {
        return new GlobalExceptionHandler(properties);
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
                                             WebCommonProperties properties) {
        return new ResultWrapAdvice(
                ResultJsonWriterFactory.create(beanFactory),
                properties.getSuccessMessage());
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
}
