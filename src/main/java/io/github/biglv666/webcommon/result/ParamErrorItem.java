package io.github.biglv666.webcommon.result;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 参数校验失败明细条目。
 *
 * <p>全局异常处理器的参数校验分支（请求体校验、表单绑定校验、单参数校验、
 * 方法参数内置校验）在响应的 {@code data} 中携带本条目列表；
 * {@code message} 保持「字段名: 原因」拼接文本不变，两者内容一致：
 * message 供人阅读，data 供程序解析（如前端按字段标红表单）。</p>
 *
 * @param field   出错字段名或参数名
 * @param message 校验失败原因
 */
@Schema(description = "参数校验失败明细条目")
public record ParamErrorItem(

        @Schema(description = "出错字段名或参数名", example = "age")
        String field,

        @Schema(description = "校验失败原因", example = "年龄必须为正数")
        String message) {
}
