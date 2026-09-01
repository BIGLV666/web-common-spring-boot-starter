package io.github.biglv666.webcommon.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.biglv666.webcommon.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Supplier;

/**
 * 序列化器选择工厂单测：本测试 JVM 同时存在 Jackson 2 与 Jackson 3
 * （provided 依赖均在测试类路径），应优先选择 Jackson 2 路径，
 * 且 Jackson 3 的 Mapper 提供者不应被触达。
 */
class ResultJsonWriterFactoryTest {

    /**
     * Jackson 2 在类路径时应优先选择 Jackson 2 实现，序列化输出正确。
     */
    @Test
    void prefersJackson2WhenPresent() {
        ResultJsonWriter writer = ResultJsonWriterFactory.create(
                (Supplier<Object>) ObjectMapper::new,
                () -> {
                    fail("Jackson 2 存在时不应触达 Jackson 3 提供者");
                    return null;
                });
        assertThat(writer).isInstanceOf(Jackson2ResultJsonWriter.class);
        assertThat(writer.toJson(Result.ok("j2"))).contains("\"code\":0").contains("\"data\":\"j2\"");
    }

    /**
     * 兜底实现应在调用时抛出带修复指引的明确异常。
     */
    @Test
    void missingJacksonWriterFailsWithGuidance() {
        MissingJacksonResultJsonWriter writer = new MissingJacksonResultJsonWriter();
        assertThatThrownBy(() -> writer.toJson(Result.ok()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tools.jackson");
    }
}
