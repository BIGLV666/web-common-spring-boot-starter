package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.Result;

/**
 * 缺失 Jackson 库时的兜底实现：类路径既无 Jackson 2 也无 Jackson 3 databind
 * （极端裁剪环境）时由 {@link ResultJsonWriterFactory} 返回。
 * 仅影响 String 返回值的自动包装路径，抛出带修复指引的明确异常；
 * 其余类型返回值的包装与全局异常处理不受影响。
 */
public class MissingJacksonResultJsonWriter implements ResultJsonWriter {

    /**
     * 序列化调用直接失败：此环境本就无法产出 JSON 响应，
     * 静默降级为原始 String 反而会造成响应结构漂移。
     *
     * @param result 包装后的 Result（未使用）
     * @return 永不返回
     * @throws IllegalStateException 始终抛出，说明缺失的依赖与修复方式
     */
    @Override
    public String toJson(Result<?> result) {
        throw new IllegalStateException(
                "检测不到 Jackson 2（com.fasterxml）或 Jackson 3（tools.jackson）databind，"
                        + "无法序列化 String 返回值的自动包装结果；"
                        + "请引入 spring-boot-starter-json，或自行声明 ResultJsonWriter Bean 并覆盖默认装配");
    }
}
