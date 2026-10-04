package io.github.biglv666.webcommon.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 基于 Micrometer 的异常指标实现：计数器 {@code web.common.exceptions}，
 * tag 为 {@code code}（错误码数值）与 {@code category}（错误码分段归类，
 * 见 {@link ExceptionMetrics#categoryOf}）。不携带 uri 等**高基数**标签，
 * 避免指标体系基数爆炸。registry 缺失（micrometer 存在但无 registry Bean，
 * 如未引入 actuator）时静默跳过。
 */
public class MicrometerExceptionMetrics implements ExceptionMetrics {

    /** 计数器名 */
    public static final String COUNTER_NAME = "web.common.exceptions";

    private final MeterRegistry registry;

    /**
     * 构造 Micrometer 异常指标。
     *
     * @param registry Micrometer 注册器，可为 null（此时全部跳过）
     */
    public MicrometerExceptionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordFailure(int code) {
        if (registry == null) {
            return;
        }
        Counter.builder(COUNTER_NAME)
                .tag("code", String.valueOf(code))
                .tag("category", ExceptionMetrics.categoryOf(code))
                .register(registry)
                .increment();
    }
}
