package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.ErrorCode;
import io.github.biglv666.webcommon.result.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.*;

/**
 * 错误码注册与启动期校验器。
 *
 * <p>业务方通过配置项声明自己的错误码枚举，本类在容器启动完成后统一校验：</p>
 * <ul>
 *     <li>重复校验：任意两个错误码（含内置 {@link ResultCode}）的 code 值不得重复；</li>
 *     <li>分段校验：code 只允许 0（成功）、500（系统兜底）或 40000~59999 区间；</li>
 *     <li>保留值校验：业务枚举不得占用 0 和 500 这两个保留值。</li>
 * </ul>
 *
 * <p>校验失败抛出 {@link IllegalStateException}，应用启动即失败——
 * 错误码冲突若拖到运行期才发现，会导致不同接口返回相同 code 而无法排障。</p>
 *
 * <p>配置示例（yaml）：</p>
 * <pre>{@code
 * web-common:
 *     error-codes:
 *         - com.example.order.OrderErrorCode
 *         - com.example.user.UserErrorCode
 * }</pre>
 */
public class ErrorCodeRegistry implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(ErrorCodeRegistry.class);

    /** 业务方声明的错误码枚举类 */
    private final List<Class<? extends ErrorCode>> enumClasses;

    /**
     * 构造注册器。
     *
     * @param enumClasses 业务方通过 {@code web-common.error-codes} 声明的枚举类
     */
    public ErrorCodeRegistry(List<Class<? extends ErrorCode>> enumClasses) {
        this.enumClasses = enumClasses == null ? List.of() : enumClasses;
    }

    /**
     * 容器单例全部就绪后执行统一校验，失败即中断启动。
     */
    @Override
    public void afterSingletonsInstantiated() {
        validate();
    }

    /**
     * 执行错误码冲突与分段校验。
     *
     * @throws IllegalStateException 任一规则不满足时抛出，message 指明冲突来源
     */
    void validate() {
        // 同一枚举可能同时被 yaml 配置与 @ErrorCodeScan 声明，去重避免自己和自己报冲突
        List<Class<? extends ErrorCode>> distinctEnums = enumClasses.stream().distinct().toList();
        // 以内置码为基线，业务码逐个比对
        Map<Integer, String> seen = new LinkedHashMap<>();
        for (ResultCode code : ResultCode.values()) {
            seen.put(code.getCode(), "内置 ResultCode." + code.name());
        }
        for (Class<? extends ErrorCode> enumClass : distinctEnums) {
            for (ErrorCode errorCode : constantsOf(enumClass)) {
                checkSegment(enumClass, errorCode);
                String previous = seen.put(errorCode.getCode(), enumClass.getName() + "#" + errorCode);
                if (previous != null) {
                    throw new IllegalStateException(String.format(
                            "错误码冲突: %s 与 %s 均使用了 code=%d，请修正后重启",
                            previous, enumClass.getName() + "#" + errorCode, errorCode.getCode()));
                }
            }
        }
        log.info("错误码校验通过: 内置 {} 个 + 业务枚举 {} 个",
                ResultCode.values().length, distinctEnums.size());
    }

    /**
     * 输出全量错误码字典（内置码 + 业务枚举码），按 code 升序。
     *
     * <p>与启动期校验同源，字典中的 code 与文案即运行期实际生效值；
     * 供错误码字典端点或使用方自行导出文档（前端联调、测试断言、网关配置）。</p>
     *
     * @return 按 code 升序排列的错误码条目
     */
    public List<ErrorCodeDescriptor> descriptors() {
        List<ErrorCodeDescriptor> descriptors = new ArrayList<>();
        for (ResultCode code : ResultCode.values()) {
            descriptors.add(ErrorCodeDescriptor.of(code, ResultCode.class));
        }
        for (Class<? extends ErrorCode> enumClass : enumClasses.stream().distinct().toList()) {
            for (ErrorCode errorCode : constantsOf(enumClass)) {
                descriptors.add(ErrorCodeDescriptor.of(errorCode, enumClass));
            }
        }
        descriptors.sort(Comparator.comparingInt(ErrorCodeDescriptor::code));
        return descriptors;
    }

    /**
     * 校验单个错误码的分段规则。
     *
     * @param enumClass  所属枚举类
     * @param errorCode 待校验的错误码
     */
    private void checkSegment(Class<? extends ErrorCode> enumClass, ErrorCode errorCode) {
        int code = errorCode.getCode();
        boolean valid = code == 0 || code == 500 || (code >= 40000 && code <= 59999);
        if (!valid) {
            throw new IllegalStateException(String.format(
                    "错误码越段: %s#%s 的 code=%d 不在合法区间（0、500 或 40000~59999）",
                    enumClass.getName(), errorCode, code));
        }
        // 0 与 500 是系统保留值，业务枚举即使首字母对齐也不得占用
        if (code == 0 || code == 500) {
            throw new IllegalStateException(String.format(
                    "错误码保留值冲突: %s#%s 占用了系统保留 code=%d",
                    enumClass.getName(), errorCode, code));
        }
    }

    /**
     * 从枚举类读取全部错误码常量。
     *
     * @param enumClass 错误码枚举类
     * @return 常量列表；类不是枚举时抛出启动异常
     */
    private List<ErrorCode> constantsOf(Class<? extends ErrorCode> enumClass) {
        if (!enumClass.isEnum()) {
            throw new IllegalStateException(
                    "错误码声明错误: " + enumClass.getName() + " 不是枚举类");
        }
        return new ArrayList<>(Arrays.asList(enumClass.getEnumConstants()));
    }
}
