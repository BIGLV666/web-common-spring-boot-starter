package io.github.biglv666.webcommon.result;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 统一分页数据结构，作为 {@code Result.data} 携带分页业务数据。
 *
 * <p>只保留两个前端真正需要的字段：{@code total} 总条数与 {@code records} 当前页数据，
 * 页码、页大小由请求方持有，不在响应中重复。自动包装开启时直接返回本结构
 * 即被包成 {@code {"code":0,"data":{"total":...,"records":[...]}}}：</p>
 *
 * <pre>{@code
 * @GetMapping("/users")
 * public PageResult<UserVO> list(Pageable pageable) {
 *     Page<UserVO> page = userService.page(pageable);
 *     return PageResult.from(page);
 * }
 * }</pre>
 *
 * <p>不依赖 Spring Data 时使用 {@link #of}；{@link #from(Page)} 需要
 * spring-data-commons 在类路径（provided + optional，不传递给使用方）。</p>
 *
 * @param <T> 记录类型
 */
@Schema(description = "分页数据：total 总条数，records 当前页数据")
public record PageResult<T>(
        @Schema(description = "总条数", example = "120", requiredMode = Schema.RequiredMode.REQUIRED) long total,
        @Schema(description = "当前页数据列表", requiredMode = Schema.RequiredMode.REQUIRED) List<T> records) {

    /**
     * 紧凑构造器：records 为 null 时规整为空列表，拷贝为不可变列表防外部篡改。
     */
    public PageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }

    /**
     * 构造分页数据，不依赖 Spring Data。
     *
     * @param records 当前页数据，允许为 null（视为空列表）
     * @param total   总条数
     * @param <T>     记录类型
     * @return 分页数据实例
     */
    public static <T> PageResult<T> of(List<T> records, long total) {
        return new PageResult<>(total, records);
    }

    /**
     * 从 Spring Data 分页结果转换。
     *
     * @param page Spring Data 分页结果，不允许为 null
     * @param <T>  记录类型
     * @return 携带总条数与当前页内容的分页数据
     */
    public static <T> PageResult<T> from(Page<T> page) {
        return new PageResult<>(page.getTotalElements(), page.getContent());
    }
}
