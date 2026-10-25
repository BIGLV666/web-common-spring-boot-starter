package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.result.PageResult;
import io.github.biglv666.webcommon.result.PageResultNormalizer;
import org.springframework.data.domain.Page;

/**
 * Spring Data {@link Page} 归一化实现。
 *
 * <p>仅在类路径存在 spring-data-commons 时由自动装配注册，使用方无需
 * 手动声明；页码从 0 开始转换为从 1 开始。</p>
 */
public class SpringDataPageResultNormalizer implements PageResultNormalizer {

    /**
     * 判断响应体是否为 Spring Data 分页结果。
     *
     * @param body 响应体
     * @return true 表示是 {@link Page}
     */
    @Override
    public boolean supports(Object body) {
        return body instanceof Page<?>;
    }

    /**
     * 将 Spring Data 分页结果转换为统一分页结构。
     *
     * @param body Spring Data 分页结果
     * @return 携带总条数、当前页数据与分页元数据的分页结构
     */
    @Override
    public PageResult<?> normalize(Object body) {
        return PageResult.from((Page<?>) body);
    }
}
