package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.ResultCode;

/**
 * {@code semantic} 模式下的 HTTP 状态码解析器：按错误码段将
 * Result.code 映射为语义化 HTTP 状态码。
 *
 * <p>映射规则与 {@link io.github.biglv666.webcommon.config.HttpStatusMode#SEMANTIC}
 * 的约定一致：</p>
 * <ul>
 *     <li>0 → 200（成功）；</li>
 *     <li>40400（{@link ResultCode#NOT_FOUND}）→ 404；</li>
 *     <li>500（系统兜底）→ 500；</li>
 *     <li>40000~49999 其余客户端侧码 → 400；</li>
 *     <li>5xxxx 业务码及其他未映射值 → 200，业务失败不伪造服务端故障状态。</li>
 * </ul>
 */
public final class HttpStatusCodeResolver {

    private HttpStatusCodeResolver() {
    }

    /**
     * 按错误码数值解析 HTTP 状态码。
     *
     * @param code Result 中的错误码
     * @return 应设置的 HTTP 状态码
     */
    public static int resolve(int code) {
        if (code == ResultCode.NOT_FOUND.getCode()) {
            return 404;
        }
        if (code == ResultCode.SYSTEM_ERROR.getCode()) {
            return 500;
        }
        if (code >= 40000 && code <= 49999) {
            return 400;
        }
        // 5xxxx 业务码与未映射值保持 200，成败仍由 code 字段区分
        return 200;
    }
}
