package io.github.biglv666.webcommon.result;

/**
 * 错误码契约接口。
 *
 * <p>所有参与统一返回与全局异常处理的错误码都必须实现本接口。
 * starter 内置的 {@link ResultCode} 是默认实现；业务方如需扩展错误码，
 * 在自己的模块内新建枚举实现本接口即可，全局异常处理器对任意
 * {@code ErrorCode} 一视同仁，无需修改 starter。</p>
 *
 * <p>错误码建议按分段约定划分，便于跨团队排障时快速定位错误层级：</p>
 * <ul>
 *     <li>{@code 0}：成功</li>
 *     <li>{@code 4xxxx}：客户端侧问题（参数、鉴权、资源、状态冲突等）</li>
 *     <li>{@code 5xxxx}：业务侧问题（业务规则校验失败、业务流程无法继续等）</li>
 *     <li>{@code 500}：系统侧问题（未识别的运行时异常兜底）</li>
 * </ul>
 */
public interface ErrorCode {

    /**
     * 获取错误码。
     *
     * @return 数字错误码，同一枚举内不允许重复
     */
    int getCode();

    /**
     * 获取默认错误提示。
     *
     * @return 面向调用方的错误描述，不应包含堆栈、内部路径等敏感信息
     */
    String getMessage();
}
