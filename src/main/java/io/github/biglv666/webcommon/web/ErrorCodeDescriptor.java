package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 错误码字典条目：数字错误码、默认文案与来源枚举信息。
 *
 * <p>由 {@link ErrorCodeRegistry#descriptors()} 输出，与启动期校验同源，
 * 字典中的 code 与文案即运行期实际生效值；供错误码字典端点或使用方
 * 自行导出文档使用。</p>
 *
 * @param code      数字错误码
 * @param message   默认错误文案
 * @param enumClass 来源错误码枚举类全限定名
 * @param constant  枚举常量名，非枚举实现为 null
 */
@Schema(description = "错误码字典条目")
public record ErrorCodeDescriptor(

        @Schema(description = "数字错误码", example = "51001")
        int code,

        @Schema(description = "默认错误文案", example = "库存不足")
        String message,

        @Schema(description = "来源错误码枚举类全限定名",
                example = "io.github.biglv666.webcommon.result.ResultCode")
        String enumClass,

        @Schema(description = "枚举常量名", example = "STOCK_NOT_ENOUGH")
        String constant) {

    /**
     * 从错误码实例构造字典条目。
     *
     * @param errorCode 错误码实例
     * @param enumClass 来源枚举类
     * @return 字典条目
     */
    public static ErrorCodeDescriptor of(ErrorCode errorCode, Class<? extends ErrorCode> enumClass) {
        String constant = errorCode instanceof Enum<?> enumConstant ? enumConstant.name() : null;
        return new ErrorCodeDescriptor(errorCode.getCode(), errorCode.getMessage(), enumClass.getName(), constant);
    }
}
