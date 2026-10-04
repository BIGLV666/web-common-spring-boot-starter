package io.github.biglv666.webcommon.fixture;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 指标集成测试配置：提供 SimpleMeterRegistry。
 *
 * <p>与 {@link I18nTestConfig} 同理，必须放在 TestApplication 组件扫描包之外，
 * 且通过 {@code @Import} 仅引入到指标相关测试，避免污染其他上下文。</p>
 */
@Configuration(proxyBeanMethods = false)
public class MetricsTestConfig {

    /**
     * 提供内存态 MeterRegistry 供断言。
     *
     * @return SimpleMeterRegistry
     */
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
