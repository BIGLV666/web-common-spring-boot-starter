package io.github.biglv666.webcommon.config;

/**
 * HTTP 状态码模式，由 {@code web-common.http-status-mode} 配置。
 *
 * <p>默认 {@link #ALWAYS_200} 完全保持 0.2.0 及以前的行为（HTTP 状态码恒为 200，
 * 成败由 {@code code} 字段区分）；{@link #SEMANTIC} 为可选增强，
 * 让失败响应携带语义化的 HTTP 状态码，便于网关、监控与通用客户端识别。</p>
 */
public enum HttpStatusMode {

    /**
     * 所有响应 HTTP 状态码恒为 200，成败由 Result.code 是否为 0 判定。
     * 这是本 starter 的默认契约，前端只需解析统一结构。
     */
    ALWAYS_200,

    /**
     * 按错误码段映射语义化 HTTP 状态码：
     * <ul>
     *     <li>code=0 → 200；</li>
     *     <li>code=40400（资源不存在）→ 404；</li>
     *     <li>code=500（系统兜底）→ 500；</li>
     *     <li>其余 4xxxx（客户端侧参数、鉴权类）→ 400；</li>
     *     <li>其余 5xxxx 业务码 → 200（业务失败不是服务端故障，
     *         返回 5xx 会误触发网关告警与重试）。</li>
     * </ul>
     * 开启后接入方需同步调整网关/监控对非 200 的处理策略。
     */
    SEMANTIC
}
