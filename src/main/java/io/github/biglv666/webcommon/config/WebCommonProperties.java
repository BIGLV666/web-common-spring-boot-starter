package io.github.biglv666.webcommon.config;

import io.github.biglv666.webcommon.result.Result;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * starter 配置项，前缀为 {@code web-common}。
 *
 * <p>所有配置均可省略，使用内置默认值；典型配置示例：</p>
 * <pre>{@code
 * web-common:
 *     enabled: true                        # 是否启用统一返回与全局异常封装，默认 true
 *     auto-wrap: true                      # 是否启用响应自动包装，默认 true
 *     http-status-mode: ALWAYS_200         # HTTP 状态码模式，默认恒为 200，可选 SEMANTIC
 *     expose-exception-message: false      # 兜底异常是否透出原始消息，默认 false
 *     error-codes:                         # 业务方错误码枚举，启动期做冲突校验
 *         - com.example.order.OrderErrorCode
 *     log:
 *         business-level: WARN             # 业务异常日志级别，默认 WARN
 *         param-level: WARN                # 参数类异常日志级别，默认 WARN
 *         system-level: ERROR              # 系统兜底异常日志级别，默认 ERROR
 * }</pre>
 */
@ConfigurationProperties(prefix = "web-common")
public class WebCommonProperties {

    /**
     * 成功响应的提示文案，默认「操作成功」。
     * 生效于自动包装的响应，以及显式 {@code Result.ok(...)} 的静态默认值。
     */
    private String successMessage = Result.SUCCESS_MESSAGE_DEFAULT;

    /**
     * 是否启用本 starter 的统一返回与全局异常封装。
     * 设为 false 时不再注册 {@code GlobalExceptionHandler} 与 {@code ResultWrapAdvice}，
     * 适用于使用方想完全接管异常处理的场景。
     */
    private boolean enabled = true;

    /**
     * 是否启用响应自动包装。启用后 Controller 可直接返回业务对象，
     * 由 starter 统一包成 {@code Result}；接口或类上标注 {@code @NoWrap} 可豁免。
     */
    private boolean autoWrap = true;

    /**
     * 兜底异常分支是否将原始异常消息透出到响应中。
     * 默认 false 以防信息泄露；仅在开发/联调环境临时开启辅助排障，
     * 生产环境必须保持 false。
     */
    private boolean exposeExceptionMessage = false;

    /**
     * HTTP 状态码模式，默认 {@link HttpStatusMode#ALWAYS_200}（HTTP 状态码恒为 200，
     * 与 0.2.0 契约一致）；设为 {@link HttpStatusMode#SEMANTIC} 时按错误码段
     * 映射语义化状态码（40400→404、500→500、4xxxx→400、5xxxx 业务码→200），
     * 接入方需同步调整网关与监控策略。
     */
    private HttpStatusMode httpStatusMode = HttpStatusMode.ALWAYS_200;

    /**
     * 业务方错误码枚举类全限定名列表。
     * 启动期由 {@code ErrorCodeRegistry} 做重复与分段校验，冲突即启动失败。
     * 也可改用 {@code @ErrorCodeScan} 注解按包扫描注册，两种方式可并存。
     */
    private List<String> errorCodes = new ArrayList<>();

    /** 异常日志相关配置 */
    private final Log log = new Log();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoWrap() {
        return autoWrap;
    }

    public void setAutoWrap(boolean autoWrap) {
        this.autoWrap = autoWrap;
    }

    public boolean isExposeExceptionMessage() {
        return exposeExceptionMessage;
    }

    public void setExposeExceptionMessage(boolean exposeExceptionMessage) {
        this.exposeExceptionMessage = exposeExceptionMessage;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    public HttpStatusMode getHttpStatusMode() {
        return httpStatusMode;
    }

    public void setHttpStatusMode(HttpStatusMode httpStatusMode) {
        this.httpStatusMode = httpStatusMode;
    }

    public List<String> getErrorCodes() {
        return errorCodes;
    }

    public void setErrorCodes(List<String> errorCodes) {
        this.errorCodes = errorCodes;
    }

    public Log getLog() {
        return log;
    }

    /**
     * 异常日志配置：各级别均可省略，默认业务/参数 WARN、系统 ERROR。
     */
    public static class Log {

        /** 业务异常（BusinessException）日志级别 */
        private LogLevel businessLevel = LogLevel.WARN;

        /** 参数类异常（校验失败、请求体不可读等）日志级别 */
        private LogLevel paramLevel = LogLevel.WARN;

        /** 系统兜底异常日志级别 */
        private LogLevel systemLevel = LogLevel.ERROR;

        public LogLevel getBusinessLevel() {
            return businessLevel;
        }

        public void setBusinessLevel(LogLevel businessLevel) {
            this.businessLevel = businessLevel;
        }

        public LogLevel getParamLevel() {
            return paramLevel;
        }

        public void setParamLevel(LogLevel paramLevel) {
            this.paramLevel = paramLevel;
        }

        public LogLevel getSystemLevel() {
            return systemLevel;
        }

        public void setSystemLevel(LogLevel systemLevel) {
            this.systemLevel = systemLevel;
        }
    }
}
