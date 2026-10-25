package io.github.biglv666.webcommon.result;

/**
 * 分页对象归一化扩展点：把第三方分页框架的返回对象转换为 {@link PageResult}。
 *
 * <p>starter 内置 Spring Data {@code Page} 与 MyBatis-Plus {@code IPage} 两个实现，
 * 使用方引入其他分页框架时，只需声明自己的 {@code PageResultNormalizer} Bean：</p>
 *
 * <pre>{@code
 * @Bean
 * public PageResultNormalizer myPageNormalizer() {
 *     return new PageResultNormalizer() {
 *         public boolean supports(Object body) { return body instanceof MyPage<?>; }
 *         public PageResult<?> normalize(Object body) {
 *             MyPage<?> page = (MyPage<?>) body;
 *             return PageResult.of(page.getList(), page.getTotal(), page.getPageNo(), page.getPageSize());
 *         }
 *     };
 * }
 * }</pre>
 *
 * <p>实现必须无状态、线程安全，且 {@link #normalize(Object)} 不得返回 null
 * （返回 null 时 starter 回退为原对象，不阻断请求）。多个实现按 {@code @Order}
 * 排序；需要覆盖内置的 Spring Data / MyBatis-Plus 类型时，给自定义实现加
 * {@code @Order} 提高优先级即可。</p>
 */
public interface PageResultNormalizer {

    /**
     * 判断是否支持归一该响应体。
     *
     * @param body Controller 返回的对象，可能为 null
     * @return true 表示本实现可以归一
     */
    boolean supports(Object body);

    /**
     * 将分页对象归一为 {@link PageResult}。
     *
     * @param body 已由 {@link #supports(Object)} 判定支持的对象
     * @return 归一后的分页数据；不得返回 null
     */
    PageResult<?> normalize(Object body);
}
