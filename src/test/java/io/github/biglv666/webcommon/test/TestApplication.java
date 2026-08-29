package io.github.biglv666.webcommon.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 集成测试用启动类。
 *
 * <p>故意放在 {@code io.github.biglv666.webcommon.test} 包下而非 starter 主包，
 * 组件扫描不会扫到主包代码，从而验证 starter 的
 * {@code AutoConfiguration.imports} 自动装配机制真实生效。</p>
 */
@SpringBootApplication
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
