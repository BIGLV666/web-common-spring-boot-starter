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
 *     pagination:
 *         enabled: true                    # 是否把 Page/IPage 自动归一为 PageResult，默认 true
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

    /** 链路追踪标识相关配置 */
    private final TraceId traceId = new TraceId();

    /** i18n 文案相关配置 */
    private final I18n i18n = new I18n();

    /** 分页归一相关配置 */
    private final Pagination pagination = new Pagination();

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

    public TraceId getTraceId() {
        return traceId;
    }

    public I18n getI18n() {
        return i18n;
    }

    public Pagination getPagination() {
        return pagination;
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

    /**
     * 链路追踪标识配置：默认关闭，开启后成功与失败响应的 {@code Result} 均携带
     * traceId（来源 MDC），并按配置写响应头。使用方已接入链路追踪
     * （Micrometer Tracing / Spring Cloud 等，默认 MDC key 即 {@code traceId}）
     * 时直接复用其值；未接入时由内置 {@code TraceIdFilter} 生成并管理生命周期。
     */
    public static class TraceId {

        /** 是否启用 traceId 透出，默认 false，响应包络保持与未启用时一致 */
        private boolean enabled = false;

        /** traceId 的 MDC key，默认 {@code traceId}（与主流链路追踪组件约定一致） */
        private String mdcKey = "traceId";

        /** traceId 响应头名，默认 {@code X-Trace-Id}；配置为空串表示不写响应头 */
        private String responseHeader = "X-Trace-Id";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMdcKey() {
            return mdcKey;
        }

        public void setMdcKey(String mdcKey) {
            this.mdcKey = mdcKey;
        }

        public String getResponseHeader() {
            return responseHeader;
        }

        public void setResponseHeader(String responseHeader) {
            this.responseHeader = responseHeader;
        }
    }

    /**
     * i18n 文案配置：默认启用，错误码默认文案与成功文案按 key 约定
     * （{@code web-common.error.{code}} / {@code web-common.success}）从
     * MessageSource 解析，缺失即回退默认值，未配置语言资源时零影响。
     * 业务自定义文案不参与解析。
     */
    public static class I18n {

        /** 是否启用文案 i18n 解析，默认 true（无语言资源时行为与关闭等价） */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 分页归一配置：默认开启。开启后 Controller 直接返回 Spring Data
     * {@code Page} 或 MyBatis-Plus {@code IPage} 会自动转为
     * {@code PageResult}（{@code total/records/current/size/pages}）；
     * 关闭后按原对象序列化，行为与 0.4.0 一致。单个接口可用
     * {@code @NoWrap} 豁免。
     */
    public static class Pagination {

        /** 是否启用框架分页对象自动归一，默认 true */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
