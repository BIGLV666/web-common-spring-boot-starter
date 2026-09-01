package io.github.biglv666.webcommon.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.biglv666.webcommon.result.Result;

/**
 * Jackson 2 实现的 Result 序列化器：优先复用容器定制的
 * {@code ObjectMapper}（命名策略、日期格式等与业务序列化保持一致）。
 * 本类仅在类路径存在 Jackson 2 databind 时才会被
 * {@link ResultJsonWriterFactory} 加载，Boot 4 纯 Jackson 3 环境不会触达。
 */
public class Jackson2ResultJsonWriter implements ResultJsonWriter {

    private final ObjectMapper objectMapper;

    /**
     * 构造 Jackson 2 序列化器。
     *
     * @param objectMapper 容器定制的 ObjectMapper，非 null
     */
    public Jackson2ResultJsonWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 用 Jackson 2 序列化包装结果。
     *
     * @param result 包装后的 Result
     * @return JSON 文本
     * @throws IllegalStateException Jackson 序列化失败时抛出，携带原因
     */
    @Override
    public String toJson(Result<?> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Result 包装 String 序列化失败", e);
        }
    }
}
