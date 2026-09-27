package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 错误码字典查询端点，由 {@code @EnableErrorCodeEndpoint} 注册，
 * 输出启动期校验通过的全量错误码（内置 + 业务声明），按 code 升序。
 *
 * <p>响应直接构造 {@code Result} 成功结构，不依赖响应自动包装开关
 * （{@code web-common.auto-wrap}），两种配置下形态一致。</p>
 */
@RestController
public class ErrorCodeEndpointController {

    /** 固定端点路径；与业务路由冲突时，使用方可自建 Controller 读取 ErrorCodeRegistry 替代 */
    static final String PATH = "/web-common/error-codes";

    private final ErrorCodeRegistry registry;

    /**
     * 构造端点控制器。
     *
     * @param registry 错误码注册器，字典数据来源
     */
    public ErrorCodeEndpointController(ErrorCodeRegistry registry) {
        this.registry = registry;
    }

    /**
     * 查询全量错误码字典。
     *
     * @return code=0 的成功结果，data 为按 code 升序的错误码条目列表
     */
    @GetMapping(PATH)
    public Result<List<ErrorCodeDescriptor>> list() {
        return Result.ok(registry.descriptors());
    }
}
