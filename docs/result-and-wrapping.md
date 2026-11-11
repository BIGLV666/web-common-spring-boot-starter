# 统一返回与自动包装

[返回 README](../README.md) ｜ [错误码与异常](error-codes.md) ｜ [分页](pagination.md)

## 1. 统一返回结构

HTTP 状态码默认恒为 200，成败由 `code` 区分：

```json
{ "code": 0, "message": "操作成功", "data": { } }
```

开启 traceId 透出后（默认关闭），响应会额外携带可选的 `traceId` 字段，与 `X-Trace-Id` 响应头一致；未开启时字段不出现在 JSON 中，结构不变。见[异常处理与配置](exception-handling.md#3-traceid-透出默认关闭)。

## 2. 响应自动包装（默认开启）

Controller 直接返回业务对象，starter 自动包装为 `Result`，无需手写 `Result.ok(...)`：

```java
@GetMapping("/user/{id}")
public UserVO getUser(@PathVariable Long id) {   // 返回自动变为 {"code":0,...,"data":{...}}
    return userService.getById(id);
}
```

需要原样输出（文件下载、健康检查等）时，在方法或类上标注 `@NoWrap` 豁免。也可显式返回 `Result`，两种风格混用没问题。

异步返回类型（`CompletableFuture` / `Callable` / `DeferredResult`）同样自动包装：Spring MVC 在异步完成后携带解析值进入消息转换，包装行为与同步返回一致。springdoc / knife4j 的文档端点（`/v3/api-docs`、swagger-ui 资源等）自动豁免，不影响文档消费。

## 3. 包装边界

| 返回形态 | 行为 |
|---|---|
| 普通业务对象 / String | 自动包装为 `Result.ok(data)` |
| 显式 `Result` | 保持不变（不重复包装） |
| `@NoWrap` 方法/类 | 原样输出 |
| `CompletableFuture` / `Callable` / `DeferredResult` | 异步完成后按解析值包装 |
| Spring Data `Page` / MyBatis-Plus `IPage` | 先归一为 `PageResult` 再包装，见[分页](pagination.md) |
| springdoc / knife4j 文档端点 | 自动豁免 |

整体关闭自动包装：`web-common.auto-wrap=false`；整体关闭 starter：`web-common.enabled=false`。

## 4. JSON 适配

String 返回值的自动包装序列化会按类路径自动适配 Jackson 2（Spring Boot 3.x 默认）与 Jackson 3（Spring Boot 4 默认），使用方无需额外引入 JSON 依赖。两条路径由 `ResultJsonWriter` 工厂在启动期按类路径选择。
