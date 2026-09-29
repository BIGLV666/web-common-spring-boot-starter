package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.Result;

/**
 * Jackson 2 实现的 Result 序列化器：优先复用容器定制的
 * {@code ObjectMapper}（命名策略、日期格式等与业务序列化保持一致）。
 *
 * <p>本类仅在类路径存在 Jackson 2 databind 时才会被
 * {@link ResultJsonWriterFactory} 加载，Boot 4 纯 Jackson 3 环境不会触达。
 * 为满足「装配层签名零 Jackson 类型」的约束，构造参数以 Object 传入、
 * 内部校验强转，Jackson 2 类型仅出现在方法体与私有字段中——
 * 类加载只解析常量池引用，不触发这些符号的解析，因此纯 Jackson 3
 * 环境加载本类不会抛 {@code NoClassDefFoundError}。</p>
 */
public class Jackson2ResultJsonWriter implements ResultJsonWriter {

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * 构造 Jackson 2 序列化器。
     *
     * @param objectMapper 容器定制的 Jackson 2 ObjectMapper；类型不符时
     *                     抛出 {@link ClassCastException}（工厂仅在探测
     *                     确认 Jackson 2 存在后调用，正常不会发生）
     */
    public Jackson2ResultJsonWriter(Object objectMapper) {
        this.objectMapper = (com.fasterxml.jackson.databind.ObjectMapper) objectMapper;
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
