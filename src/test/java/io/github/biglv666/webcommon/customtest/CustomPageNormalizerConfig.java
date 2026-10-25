package io.github.biglv666.webcommon.customtest;

import io.github.biglv666.webcommon.result.PageResult;
import io.github.biglv666.webcommon.result.PageResultNormalizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义分页归一化扩展点配置，仅由扩展点测试显式导入。
 */
@Configuration(proxyBeanMethods = false)
public class CustomPageNormalizerConfig {

    /**
     * 注册第三方分页对象的归一实现。
     *
     * @return 自定义归一实现
     */
    @Bean
    public PageResultNormalizer customPageResultNormalizer() {
        return new PageResultNormalizer() {
            @Override
            public boolean supports(Object body) {
                return body instanceof CustomPage;
            }

            @Override
            public PageResult<?> normalize(Object body) {
                CustomPage page = (CustomPage) body;
                return PageResult.of(page.items(), page.total());
            }
        };
    }
}
