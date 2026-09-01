package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.Result;
import io.github.biglv666.webcommon.result.ResultCode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jackson 3（tools.jackson）序列化器单测：验证 Boot 4 默认 JSON 库下的
 * String 返回包装路径输出正确的统一结构。
 */
class Jackson3ResultJsonWriterTest {

    /**
     * 成功结果应输出 code=0 与 data 字段。
     */
    @Test
    void serializesSuccessResult() {
        Jackson3ResultJsonWriter writer = new Jackson3ResultJsonWriter(null);
        String json = writer.toJson(Result.ok("j3"));
        assertThat(json)
                .contains("\"code\":0")
                .contains("\"message\":\"操作成功\"")
                .contains("\"data\":\"j3\"");
    }

    /**
     * 失败结果 data 为 null，按 @JsonInclude 约定不应出现在 JSON 中。
     */
    @Test
    void omitsNullDataOnFailureResult() {
        Jackson3ResultJsonWriter writer = new Jackson3ResultJsonWriter(null);
        String json = writer.toJson(Result.fail(ResultCode.PARAM_ERROR));
        assertThat(json)
                .contains("\"code\":40000")
                .doesNotContain("data");
    }

    /**
     * 传入容器 Mapper 实例时应复用而非新建。
     */
    @Test
    void reusesContainerMapper() {
        JsonMapper containerMapper = JsonMapper.builder().build();
        Jackson3ResultJsonWriter writer = new Jackson3ResultJsonWriter(containerMapper);
        assertThat(writer.toJson(Result.ok("reuse"))).contains("\"data\":\"reuse\"");
    }
}
