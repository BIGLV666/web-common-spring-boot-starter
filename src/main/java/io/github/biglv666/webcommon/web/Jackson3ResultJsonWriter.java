package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.Result;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3（{@code tools.jackson}，Spring Boot 4 起默认 JSON 库）实现的
 * Result 序列化器。优先复用容器定制的 Mapper，保证命名策略等全局配置
 * 与业务序列化一致；无容器实例时回退默认 {@link JsonMapper}。
 *
 * <p>本类仅在类路径存在 Jackson 3 databind 时才会被
 * {@link ResultJsonWriterFactory} 加载，Boot 3.x 纯 Jackson 2 环境不会触达。</p>
 */
public class Jackson3ResultJsonWriter implements ResultJsonWriter {

    private final ObjectMapper objectMapper;

    /**
     * 构造 Jackson 3 序列化器。
     *
     * @param containerMapper 容器中的 tools.jackson ObjectMapper；null 或类型
     *                        不匹配时回退默认 JsonMapper（反射探测场景以 Object 传入，
     *                        避免装配层签名依赖 Jackson 3 类型）
     */
    public Jackson3ResultJsonWriter(Object containerMapper) {
        this.objectMapper = containerMapper instanceof ObjectMapper mapper
                ? mapper
                : JsonMapper.builder().build();
    }

    /**
     * 用 Jackson 3 序列化包装结果。Jackson 3 的序列化异常为非受检，
     * 无需显式包装。
     *
     * @param result 包装后的 Result
     * @return JSON 文本
     */
    @Override
    public String toJson(Result<?> result) {
        return objectMapper.writeValueAsString(result);
    }
}
