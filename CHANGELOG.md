# Changelog

## 0.5.0 (2026-10-25)

本版本以 0.5.0 发布，汇总 0.4.0 的 Web 层能力与本次分页增强。

### 新增

- **分页自动归一**：Controller 直接返回 Spring Data `Page` 或 MyBatis-Plus `IPage` 时，自动转换为统一 `PageResult`；支持 `Result<Page>`、`ResponseEntity<Page>`、异步 `Callable<Page>` 等形态。
- **游标分页 `CursorResult`**：提供 `records` / `nextCursor` / `hasNext` 结构，适用于无限滚动、大数据量、避免 count 查询的场景。
- **`PageResultNormalizer` 扩展点**：内置 Spring Data 与 MyBatis-Plus 实现，其他分页框架声明 Bean 即可接入。
- **`PageResult` 分页元数据**：新增可选 `current` / `size` / `pages`，保留 0.4.0 的两参构造器与旧 JSON 形状。
- **配置项 `web-common.pagination.enabled`**：默认开启；关闭后 `Page` / `IPage` 按原对象序列化，行为与 0.4.0 一致。
- 0.4.0 能力：traceId 透出、i18n 文案、测试工具包、OpenAPI 错误码字典、Micrometer 异常指标、`@EnableErrorCodeEndpoint`、`ParamErrorItem` 等。

### 变更

- 直接返回 `Page` / `IPage` 的响应默认归一为五个核心字段，MyBatis-Plus 的 `orders`、`optimizeCountSql` 等内部状态不再出现在响应中。
- 需要保持旧行为时，设置 `web-common.pagination.enabled=false`，或在单个接口/类上使用 `@NoWrap`。

### 兼容性

- Java 17+，Spring Boot 3.x / 4.x（Servlet Web 应用）。
- `spring-data-commons`、`mybatis-plus-core` 均为 provided + optional，不向使用方传递；未引入时对应归一实现自动跳过。
- `PageResult` 保留 0.4.0 的两参构造器，源码与二进制兼容。

### 测试

- Boot 3.5.14（Jackson 2）与 Boot 4.1.1（Jackson 3）双线 124 个用例全部通过。
- 压测（Docker，`-Xms256m -Xmx256m`，50 并发）：
  - 1000 条：P95 2.22ms（关闭归一基线 2.24ms），0 错误。
  - 10000 条：P95 6.83ms（基线 6.52ms，+4.8%），0 错误。
  - 30 分钟 soak：71,138,896 次请求，0 错误，内存稳定在 504~524 MiB。
