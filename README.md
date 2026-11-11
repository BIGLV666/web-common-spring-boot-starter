# web-common-spring-boot-starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.biglv666/web-common-spring-boot-starter)](https://central.sonatype.com/artifact/io.github.biglv666/web-common-spring-boot-starter) [![Build](https://github.com/BIGLV666/web-common-spring-boot-starter/actions/workflows/build.yml/badge.svg)](https://github.com/BIGLV666/web-common-spring-boot-starter/actions/workflows/build.yml)

轻量级 Web 层通用封装 Spring Boot Starter：统一 `Result` 返回体 + 分段错误码枚举 + 全局异常处理 + 响应自动包装。引入依赖即生效，零代码、零配置。

[English](README.en.md)

## 核心特性

- **统一返回体**：HTTP 默认恒为 200，`{ code, message, data }` 区分成败，可选 `traceId` 字段。
- **响应自动包装**：Controller 直接返回业务对象即自动包成 `Result`，`@NoWrap` 可豁免；支持异步返回与 springdoc/knife4j 端点自动跳过。
- **错误码体系**：内置 `ResultCode` 分段约定，自定义 `ErrorCode` 枚举启动期校验重复码、越段码与保留值。
- **全局异常处理**：业务异常、`@DefaultErrorCode` 领域异常、参数校验、反序列化、404、兜底 500 统一映射。
- **分页归一**：Spring Data `Page` / MyBatis-Plus `IPage` 自动转为 `PageResult`；提供游标分页 `CursorResult` 与 `PageResultNormalizer` 扩展点。
- **可观测**：Micrometer `web.common.exceptions` 指标；可选错误码字典端点与 OpenAPI `x-error-codes` 扩展。
- **i18n**：错误码默认文案与成功文案按 locale 从 `MessageSource` 解析，缺失回退枚举文案。
- **测试工具包**：主 artifact 内置 MockMvc `ResultMatchers`，直接断言包络契约。

## 快速开始

```xml
<dependency>
    <groupId>io.github.biglv666</groupId>
    <artifactId>web-common-spring-boot-starter</artifactId>
    <version>0.5.0</version>
</dependency>
```

要求：Spring Boot 3.x 或 4.x（Servlet Web 应用）、Java 17+。String 返回值的自动包装序列化会按类路径自动适配 Jackson 2（Boot 3.x 默认）与 Jackson 3（Boot 4 默认），无需额外引入 JSON 依赖。

### 最小示例

```java
@RestController
public class UserController {

    @GetMapping("/user/{id}")
    public UserVO getUser(@PathVariable Long id) {   // 自动包装为 {"code":0,...,"data":{...}}
        return userService.getById(id);
    }

    @GetMapping("/download")
    @NoWrap                                           // 文件下载等原样输出
    public byte[] download() { return bytes; }
}
```

```java
// 业务失败：全局异常处理统一包装
throw new BusinessException(ResultCode.NOT_FOUND);
throw BusinessException.of(OrderErrorCode.STOCK_NOT_ENOUGH, "库存不足，剩余 {} 件", 3);
```

自定义错误码实现 `ErrorCode` 并声明到 `web-common.error-codes`，或使用 `@ErrorCodeScan` 按包扫描。详见[错误码与异常](docs/error-codes.md)。

## 核心配置入口

```yaml
web-common:
  enabled: true                 # 整体开关
  auto-wrap: true               # 响应自动包装
  http-status-mode: ALWAYS_200  # 或 SEMANTIC
  error-codes:                  # 业务错误码枚举（也可用 @ErrorCodeScan）
    - com.example.order.OrderErrorCode
```

其余配置（traceId、i18n、日志级别、分组分页开关）见[配置与异常处理](docs/exception-handling.md#4-配置项)。

## 文档导航

| 文档 | 内容 |
|------|------|
| [统一返回与自动包装](docs/result-and-wrapping.md) | `Result` 结构、`@NoWrap`、异步返回、包装边界 |
| [错误码与异常](docs/error-codes.md) | `BusinessException`、`@DefaultErrorCode`、自定义错误码、字典端点、i18n |
| [异常处理与配置](docs/exception-handling.md) | 全局异常覆盖表、HTTP 状态码模式、全部配置项、日志 |
| [分页](docs/pagination.md) | `PageResult`、`CursorResult`、自动归一、自定义 `PageResultNormalizer` |
| [集成能力](docs/integrations.md) | Micrometer 指标、测试工具包、OpenAPI 文档、示例工程 |
| [FAQ 与限制](docs/faq.md) | 用户可见限制、部署提示、常见问题 |

版本变更见 [CHANGELOG.md](CHANGELOG.md)。

## 升级提示（0.5.0）

直接返回 MyBatis-Plus `IPage` 的项目升级后，响应会从框架原始对象变为 `PageResult` 五个核心字段（`orders`、`optimizeCountSql` 等内部状态不再出现）。需要保持原样时设置 `web-common.pagination.enabled=false`，或给单个接口加 `@NoWrap`。详见[分页](docs/pagination.md)。

## 兼容性

- Java 17+；Spring Boot 3.x / 4.x（Servlet Web 应用）。
- `spring-data-commons`、`mybatis-plus-core`、`micrometer-core`、`swagger-annotations` 等均为 provided + optional，不向使用方传递；未引入时对应能力自动跳过。
- 错误码字典端点默认关闭且未做鉴权，仅限内网联调使用，详见 [FAQ 与限制](docs/faq.md)。

## 许可证

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)。
