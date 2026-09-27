package io.github.biglv666.webcommon.demo;

import io.github.biglv666.webcommon.annotation.EnableErrorCodeEndpoint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 示例应用启动类：演示 web-common-spring-boot-starter 的全部特性。
 *
 * <p>标注 {@code @EnableErrorCodeEndpoint} 开启错误码字典端点
 * {@code GET /web-common/error-codes}，输出启动校验通过的全量错误码。</p>
 */
@SpringBootApplication
@EnableErrorCodeEndpoint
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
