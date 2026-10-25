package io.github.biglv666.webcommon.test;

import io.github.biglv666.webcommon.config.WebCommonAutoConfiguration;
import io.github.biglv666.webcommon.result.PageResultNormalizer;
import io.github.biglv666.webcommon.web.ResultWrapAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分页依赖类路径隔离测试：使用方未引入 Spring Data / MyBatis-Plus 时，
 * 自动装配必须正常启动，不能因归一实现引用缺失类型而抛
 * {@code NoClassDefFoundError}。
 */
class PaginationClasspathIsolationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebCommonAutoConfiguration.class));

    /**
     * 隐藏 spring-data 后上下文正常启动，Spring Data 归一实现不注册。
     */
    @Test
    void startsWithoutSpringData() {
        runner.withClassLoader(new FilteredClassLoader("org.springframework.data.domain"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ResultWrapAdvice.class);
                    assertThat(context).doesNotHaveBean("springDataPageResultNormalizer");
                });
    }

    /**
     * 隐藏 mybatis-plus 后上下文正常启动，MyBatis-Plus 归一实现不注册。
     */
    @Test
    void startsWithoutMybatisPlus() {
        runner.withClassLoader(new FilteredClassLoader("com.baomidou.mybatisplus.core.metadata"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ResultWrapAdvice.class);
                    assertThat(context).doesNotHaveBean("mybatisPlusPageResultNormalizer");
                });
    }

    /**
     * 总开关关闭时既不注册包装通知，也不注册归一实现。
     */
    @Test
    void noPaginationBeansWhenMasterSwitchDisabled() {
        runner.withPropertyValues("web-common.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ResultWrapAdvice.class);
                    assertThat(context.getBeanNamesForType(PageResultNormalizer.class)).isEmpty();
                });
    }

    /**
     * 两者同时缺失时仍可启动，手动 PageResult 用法不受影响。
     */
    @Test
    void startsWithoutBothPaginationFrameworks() {
        runner.withClassLoader(new FilteredClassLoader(
                        "org.springframework.data.domain", "com.baomidou.mybatisplus.core.metadata"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ResultWrapAdvice.class);
                });
    }
}
