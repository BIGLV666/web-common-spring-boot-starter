package io.github.biglv666.webcommon.customtest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 自定义分页框架的模拟控制器，仅由自定义 Normalizer 测试显式导入。
 */
@RestController
public class CustomPageController {

    /**
     * 返回第三方分页对象，应由自定义 Normalizer 转换为 PageResult。
     *
     * @return 模拟的第三方分页对象
     */
    @GetMapping("/demo/custom-page")
    public CustomPage customPage() {
        return new CustomPage(List.of("x", "y"), 7);
    }
}
