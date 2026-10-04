package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.ErrorCode;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 基于 {@link MessageSource} 的文案解析器，为错误与成功文案提供 i18n 能力。
 *
 * <p>key 约定：</p>
 * <ul>
 *     <li>错误文案：{@code web-common.error.{code}}，如 {@code web-common.error.40400}；</li>
 *     <li>成功文案：{@code web-common.success}。</li>
 * </ul>
 *
 * <p>解析优先级：MessageSource 中当前 locale 的 key 值 &gt; 构造时传入的回退默认值
 * （即枚举默认文案 / 配置的成功文案）。未启用 i18n 或容器无 MessageSource 时
 * 直接返回回退默认值，行为与未引入本功能时完全一致。locale 取
 * {@link LocaleContextHolder#getLocale()}，尊重使用方配置的 LocaleResolver，
 * 未配置时按请求 Accept-Language 解析。</p>
 *
 * <p>仅枚举默认文案参与解析：业务自定义文案（如 {@code BusinessException} 的
 * 自定义 message、模板填充后的文案）原样透出，参数化文案的多语言由业务层负责。</p>
 */
public class LocalizedMessageResolver {

    /** 错误文案 key 前缀，完整 key 为 前缀 + 错误码数值 */
    public static final String ERROR_KEY_PREFIX = "web-common.error.";

    /** 成功文案 key */
    public static final String SUCCESS_KEY = "web-common.success";

    private final MessageSource messageSource;

    private final boolean enabled;

    /**
     * 构造文案解析器。
     *
     * @param messageSource 容器中的 MessageSource，可为 null（此时全部回退默认值）
     * @param enabled       是否启用 i18n 解析
     */
    public LocalizedMessageResolver(MessageSource messageSource, boolean enabled) {
        this.messageSource = messageSource;
        this.enabled = enabled;
    }

    /**
     * 解析错误码文案：优先按 {@code web-common.error.{code}} 取当前 locale 文案，
     * 缺失回退错误码默认文案。
     *
     * @param errorCode 错误码
     * @return 解析后的文案
     */
    public String resolveError(ErrorCode errorCode) {
        return resolve(ERROR_KEY_PREFIX + errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 解析业务异常文案：自定义文案（与错误码默认文案不同）原样透出；
     * 为 null 或恰为错误码默认文案时按错误码 key 解析。
     * 异常文案为 null 通常意味着构造时未传自定义文案，需回退错误码文案。
     *
     * @param errorCode 异常携带的错误码
     * @param message   异常自身文案，可为 null
     * @return 应写出的最终文案
     */
    public String resolveIfDefault(ErrorCode errorCode, String message) {
        return message != null && !message.equals(errorCode.getMessage())
                ? message
                : resolveError(errorCode);
    }

    /**
     * 解析成功文案：优先按 {@code web-common.success} 取当前 locale 文案，
     * 缺失回退配置的成功文案。
     *
     * @param fallback 配置的默认成功文案
     * @return 解析后的文案
     */
    public String resolveSuccess(String fallback) {
        return resolve(SUCCESS_KEY, fallback);
    }

    /**
     * 按 key 与当前 locale 解析文案，未启用或无 MessageSource 时返回回退默认值。
     *
     * @param key      MessageSource key
     * @param fallback 回退默认值
     * @return 解析后的文案
     */
    private String resolve(String key, String fallback) {
        if (!enabled || messageSource == null) {
            return fallback;
        }
        // 四参重载：key 缺失即返回 fallback，不抛 NoSuchMessageException
        return messageSource.getMessage(key, null, fallback, LocaleContextHolder.getLocale());
    }
}
