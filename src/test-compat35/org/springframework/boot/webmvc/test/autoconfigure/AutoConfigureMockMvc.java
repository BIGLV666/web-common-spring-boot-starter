package org.springframework.boot.webmvc.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 双代 Boot 兼容桥（仅测试类路径，不进产物 jar）。
 *
 * <p>测试代码的 {@code @AutoConfigureMockMvc} import 统一取 Boot 4 的包名
 * {@code org.springframework.boot.webmvc.test.autoconfigure}；Boot 3.5 无该包，
 * 同名注解在 {@code org.springframework.boot.test.autoconfigure.web.servlet}。
 * 本类是 Boot 3.5 下的同名注解壳，元注解指向 3.5 的真实注解，使测试在
 * 3.5 依赖树下编译、运行均生效；Boot 4 依赖树（CI 的 boot4 job）中
 * spring-boot-starter-webmvc-test 提供真正的同名注解，按类路径顺序覆盖本类。</p>
 *
 * <p>为什么测试要 import Boot 4 包名而不是 3.5 的：Boot 4 已彻底删除
 * {@code test.autoconfigure.web.servlet} 包（无兼容类保留），反向桥不可行；
 * 而本桥在 3.5 下是仅有的同名类、在 4.x 下被同名真类遮蔽，两端都成立。</p>
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
public @interface AutoConfigureMockMvc {

    /**
     * 透传 print 选项，保持与真实注解同形。
     *
     * @return MockMvc 结果打印选项
     */
    org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint print()
            default org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.DEFAULT;
}
