package io.github.biglv666.webcommon.web;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 错误码字典 OpenAPI 扩展：将 {@link ErrorCodeRegistry} 的全量错误码
 * 以 {@code x-error-codes} 扩展字段挂到 OpenAPI 文档根节点，
 * 前端与网关从 swagger 文档即可消费错误码字典，无需另调字典端点。
 *
 * <p>输出形态与字典端点一致（code/message/enumClass/constant，按 code 升序）：</p>
 * <pre>{@code
 * {
 *     "openapi": "3.1.0",
 *     "x-error-codes": [
 *         { "code": 40000, "message": "参数错误", "enumClass": "...ResultCode", "constant": "PARAM_ERROR" }
 *     ]
 * }
 * }</pre>
 *
 * <p>本类依赖 springdoc 接口，由自动装配按类名条件注册——使用方工程无
 * springdoc 时不会实例化。</p>
 */
public class ErrorCodeOpenApiCustomizer implements OpenApiCustomizer {

    /** OpenAPI 文档根节点的扩展字段名 */
    public static final String EXTENSION_NAME = "x-error-codes";

    private final ErrorCodeRegistry registry;

    /**
     * 构造错误码字典扩展。
     *
     * @param registry 错误码注册器，字典数据来源
     */
    public ErrorCodeOpenApiCustomizer(ErrorCodeRegistry registry) {
        this.registry = registry;
    }

    /**
     * 将错误码字典写入 OpenAPI 根节点扩展。
     *
     * @param openApi springdoc 构建的 OpenAPI 文档对象
     */
    @Override
    public void customise(OpenAPI openApi) {
        // LinkedHashMap 保持字段顺序且容忍 constant 为 null（非枚举 ErrorCode 实现）
        List<Map<String, Object>> entries = registry.descriptors().stream()
                .map(descriptor -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("code", descriptor.code());
                    entry.put("message", descriptor.message());
                    entry.put("enumClass", descriptor.enumClass());
                    entry.put("constant", descriptor.constant());
                    return entry;
                })
                .toList();
        openApi.addExtension(EXTENSION_NAME, entries);
    }
}
