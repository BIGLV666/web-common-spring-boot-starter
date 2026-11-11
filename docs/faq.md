# FAQ 与限制

[返回 README](../README.md) ｜ [异常处理与配置](exception-handling.md) ｜ [分页](pagination.md)

## 用户可见限制

- HTTP 状态码默认恒为 200；需要语义化状态码时显式开启 `SEMANTIC` 模式，并同步调整网关与监控。
- 错误码字典端点 `/web-common/error-codes` 默认关闭、未做鉴权，输出包含内部枚举全限定名；仅限内网联调，不要暴露到公网网关。
- 自定义错误码只允许 `0`、`500`、`40000~59999` 段；重复码、越段码、保留值会在启动期直接失败。
- `traceId` 唯一来源是 MDC；Servlet 异步端点的跨线程传递依赖使用方的链路追踪组件，未接入时异步端点可能拿不到 traceId。
- 分页自动归一仅覆盖 Spring Data `Page` 与 MyBatis-Plus `IPage`；`Slice` 与游标类型不自动归一。
- 游标内容由业务编码，starter 不定义格式、不做加解密与签名。
- `expose-exception-message=true` 仅用于联调，生产必须关闭，避免内部异常信息外泄。
- 响应自动包装只作用于 Web 层；非 Web 线程/内部调用不会自动包装。

## FAQ

### 为什么所有响应 HTTP 状态码都是 200？

默认契约以响应体 `code` 区分成败，便于前端统一处理。需要网关/监控识别语义状态码时配置 `web-common.http-status-mode=SEMANTIC`：`0`→200、`40400`→404、`500`→500、其余 `4xxxx`→400、`5xxxx`→200。

### 如何让某个接口不包装？

方法或类上标注 `@NoWrap`。整体关闭用 `web-common.auto-wrap=false`，关闭整个 starter 用 `web-common.enabled=false`。

### 已经显式返回 `Result`，会被重复包装吗？

不会。显式 `Result` 保持原样；普通对象才自动包装，两种风格可混用。

### 自定义异常如何接入统一返回？

给异常类标 `@DefaultErrorCode(value = 错误码枚举.class, constant = "常量名")`。单常量枚举可只写类；多常量枚举（如 `ResultCode`）必须指定 `constant`。message 取异常自身文案，为空回退错误码默认文案。

### 错误码枚举必须逐个写到配置里吗？

不必。可在 `@Configuration` 类上使用 `@ErrorCodeScan("包名")` 自动扫描，也可与 `web-common.error-codes` 并存。

### i18n 为什么没有生效？

确认 `web-common.i18n.enabled=true`（默认）、`MessageSource` 已配置、key 使用 `web-common.error.{code}` / `web-common.success`。仅“默认文案”参与解析；`BusinessException` 自定义文案与模板填充结果原样透出。locale 来自 `LocaleContextHolder`（缺省 Accept-Language）。

### 升级 0.5.0 后 `IPage` 响应字段变少了？

这是分页自动归一的预期行为，只保留 `total`、`records`、`current`、`size`、`pages`。需要旧形状时设置 `web-common.pagination.enabled=false`，或对单接口加 `@NoWrap`。

### 支持 Spring Boot 4 / Jackson 3 吗？

支持 Spring Boot 3.x 与 4.x（Servlet Web）。String 包装序列化会按类路径自动选择 Jackson 2 或 Jackson 3，无需额外依赖。

### 可以替换默认的全局异常处理器或指标实现吗？

可以。声明自己的 `GlobalExceptionHandler` / `ResultWrapAdvice` / `ExceptionMetrics` / `ErrorCodeRegistry` Bean 即可覆盖对应默认实现。分页可声明 `PageResultNormalizer` 并按 `@Order` 调整优先级。
