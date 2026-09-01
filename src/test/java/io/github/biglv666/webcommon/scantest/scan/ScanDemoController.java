package io.github.biglv666.webcommon.scantest.scan;

import io.github.biglv666.webcommon.exception.BusinessException;
import io.github.biglv666.webcommon.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 使用扫描注册错误码的测试端点。
 */
@RestController
public class ScanDemoController {

    /**
     * 抛出扫描注册的 ScanDemoErrorCode，验证扫描枚举真实参与错误处理。
     */
    @GetMapping("/demo/scan-code")
    public Result<Void> scanCode() {
        throw new BusinessException(ScanDemoErrorCode.PAY_REQUIRED);
    }
}
