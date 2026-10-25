package io.github.biglv666.webcommon.customtest;

import java.util.List;

/**
 * 第三方分页框架的模拟对象，用于验证自定义 PageResultNormalizer 扩展点。
 *
 * @param items 当前页数据
 * @param total 总条数
 */
public record CustomPage(List<String> items, long total) {
}
