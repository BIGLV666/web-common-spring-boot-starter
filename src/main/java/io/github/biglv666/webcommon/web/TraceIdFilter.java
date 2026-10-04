package io.github.biglv666.webcommon.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * traceId 过滤器，仅在 {@code web-common.trace-id.enabled=true} 时注册。
 *
 * <p>注册顺序为最低优先级（最后执行）：使用方已接入链路追踪时，其过滤器
 * （通常为最高优先级）先运行并写入 MDC，本过滤器读取到已有值后原样透传，
 * 既不覆盖也不清理，生命周期完全归链路追踪组件所有；未接入链路追踪时
 * MDC 为空，本过滤器生成 traceId 并自行管理生命周期（finally 中清理，
 * 防止容器线程池复用导致串号）。</p>
 *
 * <p>两种情况下都会按配置写响应头（默认 {@code X-Trace-Id}，配置为空串关闭），
 * 前端报障时可直接回传该值对上服务端日志。</p>
 */
public class TraceIdFilter extends OncePerRequestFilter {

    private final String mdcKey;

    private final String responseHeader;

    /**
     * 构造 traceId 过滤器。
     *
     * @param mdcKey         MDC key
     * @param responseHeader 响应头名，空白表示不写响应头
     */
    public TraceIdFilter(String mdcKey, String responseHeader) {
        this.mdcKey = mdcKey;
        this.responseHeader = responseHeader;
    }

    /**
     * 读取或生成 traceId，写响应头后放行请求；
     * 仅对自行生成的 traceId 清理 MDC，不触碰使用方链路追踪组件写入的值。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String existing = TraceIds.current(mdcKey);
        boolean generated = existing == null;
        String traceId = generated ? TraceIds.generate() : existing;
        if (generated) {
            MDC.put(mdcKey, traceId);
        }
        if (responseHeader != null && !responseHeader.isBlank()) {
            response.setHeader(responseHeader, traceId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (generated) {
                MDC.remove(mdcKey);
            }
        }
    }
}
