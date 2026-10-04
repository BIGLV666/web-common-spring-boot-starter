package io.github.biglv666.webcommon.fixture;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * i18n 集成测试的语言资源配置。
 *
 * <p>注意：本类必须放在 {@code io.github.biglv666.webcommon.test} 包之外
 * （TestApplication 组件扫描该包及其子包），否则会被所有测试上下文扫描到，
 * 测试语言资源将污染全量用例的文案断言；同样不可作为测试类的内嵌
 * {@code @Configuration}（那会使 TestContext 跳过 {@code @SpringBootConfiguration}）。
 * 通过 {@code @Import(I18nTestConfig.class)} 仅引入到 i18n 相关测试。</p>
 */
@Configuration(proxyBeanMethods = false)
public class I18nTestConfig {

    /**
     * 提供测试语言资源；Boot 的 MessageSource 自动配置对已存在的 MessageSource Bean 让位。
     *
     * @return 指向 i18n/messages 资源束的 MessageSource
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }
}
