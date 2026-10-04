package io.github.biglv666.webcommon.web;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * traceId 读取与生成的内部工具。
 *
 * <p>traceId 的唯一来源是 MDC（key 由 {@code web-common.trace-id.mdc-key} 配置，
 * 默认 {@code traceId}，与 Micrometer Tracing / Spring Cloud 的默认 MDC key 一致）：
 * 使用方接入链路追踪时无需任何适配，本 starter 直接复用其写入 MDC 的 traceId；
 * 未接入链路追踪时由 {@link TraceIdFilter} 生成。</p>
 *
 * <p>MDC 为线程局部变量，Servlet 异步请求（Callable/DeferredResult 等）在
 * 独立工作线程上完成处理，starter 不做跨线程传递——已接入链路追踪的使用方
 * 由其自身的上下文传播机制保证，未接入的使用方在异步端点上将拿不到 traceId。</p>
 */
public final class TraceIds {

    private TraceIds() {
    }

    /**
     * 读取当前线程 MDC 中的 traceId。
     *
     * @param mdcKey MDC key，空白视为未配置，返回 null
     * @return 当前 traceId；不存在或为空白时返回 null
     */
    public static String current(String mdcKey) {
        if (mdcKey == null || mdcKey.isBlank()) {
            return null;
        }
        String value = MDC.get(mdcKey);
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 生成新的 traceId：32 位小写十六进制（UUID 去连字符），长度对日志列宽友好。
     *
     * @return 生成的 traceId
     */
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
