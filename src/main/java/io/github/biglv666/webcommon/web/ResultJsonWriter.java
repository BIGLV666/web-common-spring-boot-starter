package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.Result;

/**
 * String 返回值自动包装的 JSON 序列化器：将包装后的 {@link Result}
 * 序列化为 JSON 字符串，供 {@link ResultWrapAdvice} 在
 * {@code StringHttpMessageConverter} 场景下交出。
 *
 * <p>抽象出独立接口是为了适配 Jackson 2 与 Jackson 3（Spring Boot 4 起默认）
 * 两代 databind API：具体实现分别隔离在
 * {@link Jackson2ResultJsonWriter} 与 {@link Jackson3ResultJsonWriter} 中，
 * 由 {@link ResultJsonWriterFactory} 按类路径探测选择，保证两代运行时均可加载。</p>
 */
public interface ResultJsonWriter {

    /**
     * 将包装结果序列化为 JSON 字符串。
     *
     * @param result 包装后的 Result
     * @return JSON 文本
     * @throws IllegalStateException 序列化失败或运行时缺失 Jackson 库时抛出
     */
    String toJson(Result<?> result);
}
