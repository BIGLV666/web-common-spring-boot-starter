package io.github.biglv666.webcommon.web;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 从请求体反序列化异常中提取出错字段路径。
 *
 * <p>底层异常可能是 Jackson 2（Boot 3.x）或 Jackson 3（Boot 4）的
 * {@code InvalidFormatException}，两者 API 同形但分属不同包；
 * 与 {@link ResultWrapAdvice} 的双代适配策略一致，这里通过反射读取，
 * 主代码不直接依赖任何 Jackson 类型，避免类加载耦合。</p>
 *
 * <p>只提取 JSON 字段路径（如 {@code orders[0].count}），不透出目标类型、
 * 类名等内部信息，维持响应不泄露实现细节的原则。</p>
 */
final class RequestBodyErrorDetail {

    /** 两代 Jackson 的类型不匹配异常类名一致，按 simple name 识别 */
    private static final String INVALID_FORMAT_SIMPLE_NAME = "InvalidFormatException";

    /** cause 链遍历深度上限，防御循环引用 */
    private static final int MAX_CAUSE_DEPTH = 10;

    private RequestBodyErrorDetail() {
    }

    /**
     * 沿 cause 链查找反序列化类型不匹配异常并提取出错字段路径。
     *
     * @param root 反序列化失败的根异常
     * @return 字段路径（如 {@code orders[0].count}）；未命中或无法提取时返回 null
     */
    static String fieldPathOf(Throwable root) {
        Throwable current = root;
        int depth = 0;
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            if (current.getClass().getSimpleName().equals(INVALID_FORMAT_SIMPLE_NAME)) {
                return extractPath(current);
            }
            current = current.getCause();
            depth++;
        }
        return null;
    }

    /**
     * 反射读取异常的 {@code getPath()}，把路径引用序列拼接为 JSON 字段路径：
     * 字段引用输出为 {@code fieldName}，数组下标引用输出为 {@code [index]}，
     * 段间以 {@code .} 连接（下标段除外）。
     *
     * @param exception 反序列化异常实例
     * @return 字段路径；无路径信息或反射失败时返回 null，交由调用方回退固定文案
     */
    private static String extractPath(Throwable exception) {
        try {
            Method getPath = exception.getClass().getMethod("getPath");
            Object result = getPath.invoke(exception);
            if (!(result instanceof List<?> references) || references.isEmpty()) {
                return null;
            }
            StringBuilder path = new StringBuilder();
            for (Object reference : references) {
                String fieldName = invokeString(reference, "getFieldName");
                if (fieldName != null && !fieldName.isEmpty()) {
                    // 字段段前补点（下标段紧贴前一段，无点）；path 为 items[0] 时接 count 应得 items[0].count
                    if (path.length() > 0) {
                        path.append('.');
                    }
                    path.append(fieldName);
                } else {
                    Integer index = invokeInt(reference, "getIndex");
                    if (index == null || index < 0) {
                        return null;
                    }
                    path.append('[').append(index).append(']');
                }
            }
            return path.length() > 0 ? path.toString() : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /**
     * 反射调用对象的无参方法并取 String 返回值。
     *
     * @param target     目标对象
     * @param methodName 方法名
     * @return 调用结果；失败或类型不符时返回 null
     */
    private static String invokeString(Object target, String methodName) {
        Object result = invoke(target, methodName);
        return result instanceof String s ? s : null;
    }

    /**
     * 反射调用对象的无参方法并取 Integer 返回值。
     *
     * @param target     目标对象
     * @param methodName 方法名
     * @return 调用结果；失败或类型不符时返回 null
     */
    private static Integer invokeInt(Object target, String methodName) {
        Object result = invoke(target, methodName);
        return result instanceof Integer i ? i : null;
    }

    /**
     * 反射调用对象的无参方法。
     *
     * @param target     目标对象
     * @param methodName 方法名
     * @return 调用结果；失败时返回 null
     */
    private static Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
