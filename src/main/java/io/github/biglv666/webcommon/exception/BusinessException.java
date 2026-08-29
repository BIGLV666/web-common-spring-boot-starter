package io.github.biglv666.webcommon.exception;

import io.github.biglv666.webcommon.result.ErrorCode;

/**
 * 业务异常，业务代码表达「请求合法但业务上无法继续」的标准方式。
 *
 * <p>在 Service / 业务逻辑中按需抛出，由全局异常处理器统一捕获并
 * 转换为 {@code Result} 失败结构，业务代码无需自行拼装返回体：</p>
 *
 * <pre>{@code
 *     // 使用错误码默认文案
 *     throw new BusinessException(ResultCode.NOT_FOUND);
 *
 *     // 使用更具体的自定义文案
 *     throw new BusinessException(ResultCode.CONFLICT, "用户名已被注册");
 *
 *     // 使用业务方自建错误码枚举（实现 ErrorCode 接口）
 *     throw new BusinessException(OrderErrorCode.STOCK_NOT_ENOUGH);
 * }</pre>
 *
 * <p>本异常仅用于业务语义错误；系统级异常（空指针、连接失败等）
 * 不要包装成本异常，应直接抛出交由兜底分支处理并记录堆栈。</p>
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 使用错误码默认文案构造业务异常。
     *
     * @param errorCode 错误码，不允许为 null
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义文案构造业务异常，覆盖错误码默认文案。
     *
     * @param errorCode 错误码，不允许为 null
     * @param message   自定义提示信息，将出现在响应 message 中
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 获取本异常携带的错误码。
     *
     * @return 构造时传入的错误码，全局处理器据此生成响应 code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 使用 message 模板构造业务异常，模板中的 {@code {}} 占位符按顺序被实参替换。
     *
     * <p>适用于错误提示需要拼接运行期数据的场景，避免手工字符串拼接：</p>
     *
     * <pre>{@code
     * // 生成 message 为「库存不足，剩余 3 件」
     * throw BusinessException.of(OrderErrorCode.STOCK_NOT_ENOUGH, "库存不足，剩余 {} 件", 3);
     * }</pre>
     *
     * @param errorCode       错误码，不允许为 null
     * @param messageTemplate 含 {@code {}} 占位符的模板，占位符数量应与实参数量一致
     * @param args            按顺序填充占位符的实参，多余实参忽略
     * @return 携带填充后 message 的业务异常
     */
    public static BusinessException of(ErrorCode errorCode, String messageTemplate, Object... args) {
        return new BusinessException(errorCode, fillTemplate(messageTemplate, args));
    }

    /**
     * 将模板中的 {@code {}} 占位符按顺序替换为实参。
     * 按字符扫描，避免 String.replace 对实参内容中含 {@code {}} 的误替换。
     *
     * @param template 含占位符的模板
     * @param args     填充实参
     * @return 填充后的 message；实参不足时占位符保留原样
     */
    private static String fillTemplate(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder(template.length() + 32);
        int argIndex = 0;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            // 识别 "{}" 占位符，且实参未用尽时替换；转义场景（如 JSON 文案）不在业务模板范围内
            if (c == '{' && i + 1 < template.length() && template.charAt(i + 1) == '}' && argIndex < args.length) {
                sb.append(args[argIndex++]);
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
