package io.github.biglv666.webcommon.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.biglv666.webcommon.result.PageResult;
import io.github.biglv666.webcommon.result.PageResultNormalizer;

/**
 * MyBatis-Plus {@link IPage} 归一化实现。
 *
 * <p>仅在类路径存在 mybatis-plus-core 时由自动装配注册。只保留
 * {@code total/records/current/size/pages} 五个前端需要的字段，
 * 刻意丢弃 {@code orders}、{@code optimizeCountSql} 等查询对象内部状态，
 * 避免把数据库列名与调优参数透出到响应。</p>
 */
public class MybatisPlusPageResultNormalizer implements PageResultNormalizer {

    /**
     * 判断响应体是否为 MyBatis-Plus 分页结果。
     *
     * @param body 响应体
     * @return true 表示是 {@link IPage}
     */
    @Override
    public boolean supports(Object body) {
        return body instanceof IPage<?>;
    }

    /**
     * 将 MyBatis-Plus 分页结果转换为统一分页结构。
     *
     * @param body MyBatis-Plus 分页结果
     * @return 携带总条数、当前页数据与分页元数据的分页结构
     */
    @Override
    public PageResult<?> normalize(Object body) {
        IPage<?> page = (IPage<?>) body;
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
