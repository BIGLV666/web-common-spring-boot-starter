package io.github.biglv666.webcommon.web;

/**
 * 异常指标 SPI：全局异常处理器每处理一个失败请求即回调一次。
 *
 * <p>starter 的默认实现基于 Micrometer（计数器 {@code web.common.exceptions}，
 * tag 含 {@code code} 与 {@code category}），仅当使用方类路径存在 micrometer 时
 * 由自动装配注册；类路径无 micrometer 时不注册任何实现，异常处理器空引用跳过，
 * 零类加载风险。使用方可自行声明 {@code ExceptionMetrics} Bean 接入
 * 自有监控体系，自动装配随之让位。</p>
 */
public interface ExceptionMetrics {

    /**
     * 记录一次失败响应。
     *
     * @param code 本次失败的错误码数值
     */
    void recordFailure(int code);

    /**
     * 按错误码分段归类：0 成功不记录；500 系统兜底；4xxxx 客户端侧（param）；
     * 5xxxx 业务侧（business）；其余为 other（正常不会出现，防御性归类）。
     *
     * @param code 错误码数值
     * @return category 标签值
     */
    static String categoryOf(int code) {
        if (code == 500) {
            return "system";
        }
        if (code >= 40000 && code < 50000) {
            return "param";
        }
        if (code >= 50000 && code < 60000) {
            return "business";
        }
        return "other";
    }
}
