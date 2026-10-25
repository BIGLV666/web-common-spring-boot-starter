package io.github.biglv666.webcommon.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 统一游标分页数据结构，作为 {@code Result.data} 携带游标分页业务数据。
 *
 * <p>与 {@link PageResult} 的偏移量分页不同，游标分页不返回总条数与页码，
 * 只返回当前页数据与下一页游标，适合无限滚动、大数据量、避免 count 查询的场景：</p>
 *
 * <pre>{@code
 * @GetMapping("/messages")
 * public CursorResult<MessageVO> list(@RequestParam(required = false) String cursor) {
 *     List<MessageVO> records = messageService.next(cursor, 20);
 *     return records.size() < 20
 *             ? CursorResult.last(records)
 *             : CursorResult.of(records, messageService.cursorOf(records.get(records.size() - 1)));
 * }
 * }</pre>
 *
 * <p>游标内容由业务自行编码（如 id、id + createdAt、Base64 JSON 等），starter
 * 不定义编码格式、不做加解密与签名，避免绑定单列 id 或透出数据库字段。</p>
 *
 * <p>{@code nextCursor} 为核心字段，末页时输出 {@code null}；{@code hasNext} 是
 * 计算属性，恒等于 {@code nextCursor != null}，不参与 {@code equals/hashCode}。</p>
 *
 * @param <T> 记录类型
 */
@Schema(description = "游标分页数据：records 当前页数据，nextCursor 下一页游标（末页为 null），hasNext 是否还有下一页")
public record CursorResult<T>(
        @Schema(description = "当前页数据列表", requiredMode = Schema.RequiredMode.REQUIRED) List<T> records,
        @Schema(description = "下一页游标；末页为 null") String nextCursor) {

    /**
     * 紧凑构造器：records 为 null 时规整为空列表，拷贝为不可变列表防外部篡改。
     */
    public CursorResult {
        records = records == null ? List.of() : List.copyOf(records);
    }

    /**
     * 构造游标分页数据。
     *
     * <p>空白游标（null、空串、纯空白）一律规整为 null，避免出现
     * {@code hasNext=true} 但客户端拿着空游标无法继续翻页的非法状态。</p>
     *
     * @param records    当前页数据，允许为 null（视为空列表）
     * @param nextCursor 下一页游标，允许为 null 或空白
     * @param <T>        记录类型
     * @return 游标分页数据
     */
    public static <T> CursorResult<T> of(List<T> records, String nextCursor) {
        return new CursorResult<>(records, nextCursor == null || nextCursor.isBlank() ? null : nextCursor);
    }

    /**
     * 构造末页数据：{@code nextCursor} 恒为 null。
     *
     * @param records 当前页数据，允许为 null（视为空列表）
     * @param <T>     记录类型
     * @return 末页游标分页数据
     */
    public static <T> CursorResult<T> last(List<T> records) {
        return new CursorResult<>(records, null);
    }

    /**
     * 是否还有下一页，恒等于 {@code nextCursor != null}。
     *
     * <p>以计算属性形式序列化，保证与 {@code nextCursor} 永远一致，
     * 不会出现两个字段互相矛盾的情况。</p>
     *
     * @return true 表示还有下一页
     */
    @JsonProperty("hasNext")
    public boolean hasNext() {
        return nextCursor != null;
    }
}
