package io.github.biglv666.webcommon.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 统一偏移量分页数据结构，作为 {@code Result.data} 携带分页业务数据。
 *
 * <p>核心字段只有两个：{@code total} 总条数与 {@code records} 当前页数据。
 * 归一分页对象时刻带 {@code current}、{@code size}、{@code pages} 三个可选元数据，
 * 手动 {@link #of(List, long)} 构造时不带，JSON 形状与 0.4.0 完全一致：</p>
 *
 * <pre>{@code
 * @GetMapping("/users")
 * public PageResult<UserVO> list(Pageable pageable) {
 *     Page<UserVO> page = userService.page(pageable);
 *     return PageResult.from(page);
 * }
 * }</pre>
 *
 * <p>Controller 直接返回 Spring Data {@code Page} 或 MyBatis-Plus {@code IPage} 时，
 * 自动包装会归一为本结构，无需手动调用 {@link #from(Page)}。不依赖 Spring Data 时
 * 使用 {@link #of(List, long)}；{@link #from(Page)} 需要 spring-data-commons
 * 在类路径（provided + optional，不传递给使用方）。</p>
 *
 * <p>偏移量分页适合需要总条数、随机跳页的后台列表；无限滚动、大数据量场景
 * 请使用 {@link CursorResult}。</p>
 *
 * @param <T> 记录类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "偏移量分页数据：total 总条数，records 当前页数据，current/size/pages 为可选分页元数据")
public record PageResult<T>(
        @Schema(description = "总条数", example = "120", requiredMode = Schema.RequiredMode.REQUIRED) long total,
        @Schema(description = "当前页数据列表", requiredMode = Schema.RequiredMode.REQUIRED) List<T> records,
        @Schema(description = "当前页码，从 1 开始；仅框架分页对象归一或显式传入时出现", example = "2")
        Long current,
        @Schema(description = "每页条数；仅框架分页对象归一或显式传入时出现", example = "20")
        Long size,
        @Schema(description = "总页数；仅框架分页对象归一或显式传入时出现", example = "6")
        Long pages) {

    /**
     * 紧凑构造器：records 为 null 时规整为空列表，拷贝为不可变列表防外部篡改。
     */
    public PageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }

    /**
     * 兼容 0.4.0 的构造器：只携带总条数与当前页数据，不输出分页元数据。
     *
     * <p>保留该构造器是为了让升级到 0.5.0 的使用方源码与二进制均无需改动。</p>
     *
     * @param total   总条数
     * @param records 当前页数据，允许为 null（视为空列表）
     */
    public PageResult(long total, List<T> records) {
        this(total, records, null, null, null);
    }

    /**
     * 构造分页数据，不依赖 Spring Data，不携带分页元数据。
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
     * 构造携带分页元数据的分页数据，不依赖 Spring Data。
     *
     * <p>总页数按 {@code ceil(total / size)} 计算，与归一两个框架时保持同一口径；
     * {@code size <= 0} 时总页数为 0。</p>
     *
     * @param records 当前页数据，允许为 null（视为空列表）
     * @param total   总条数
     * @param current 当前页码，从 1 开始
     * @param size    每页条数
     * @param <T>     记录类型
     * @return 携带分页元数据的分页数据
     */
    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        return new PageResult<>(total, records, current, size, totalPages(total, size));
    }

    /**
     * 从 Spring Data 分页结果转换。
     *
     * <p>Spring Data 页码从 0 开始，转换后 {@code current} 从 1 开始。</p>
     *
     * @param page Spring Data 分页结果，不允许为 null
     * @param <T>  记录类型
     * @return 携带总条数、当前页内容与分页元数据的分页数据
     */
    public static <T> PageResult<T> from(Page<T> page) {
        return of(page.getContent(), page.getTotalElements(), page.getNumber() + 1L, page.getSize());
    }

    /**
     * 计算总页数：{@code size <= 0} 时为 0，其余向上取整。
     *
     * @param total 总条数
     * @param size  每页条数
     * @return 总页数
     */
    private static long totalPages(long total, long size) {
        return size <= 0 ? 0 : (total + size - 1) / size;
    }
}
