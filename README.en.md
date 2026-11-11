# web-common-spring-boot-starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.biglv666/web-common-spring-boot-starter)](https://central.sonatype.com/artifact/io.github.biglv666/web-common-spring-boot-starter) [![Build](https://github.com/BIGLV666/web-common-spring-boot-starter/actions/workflows/build.yml/badge.svg)](https://github.com/BIGLV666/web-common-spring-boot-starter/actions/workflows/build.yml)

Lightweight Spring Boot starter for the web layer: a unified `Result` response body + segmented error-code enums + global exception handling + automatic response wrapping. Works out of the box — zero code, zero configuration.

[简体中文](README.md)

## Features

- **Unified response body**: HTTP status is 200 by default; `{ code, message, data }` indicates success or failure, with an optional `traceId` field.
- **Automatic wrapping**: controllers return business objects and the starter wraps them into `Result`; opt out with `@NoWrap`. Async return types and springdoc/knife4j endpoints are handled automatically.
- **Error codes**: built-in `ResultCode` segment conventions; custom `ErrorCode` enums are validated at startup for duplicates, out-of-segment values and reserved codes.
- **Global exception handling**: business exceptions, `@DefaultErrorCode` domain exceptions, validation errors, deserialization failures, 404 and fallback 500 are mapped uniformly.
- **Pagination**: Spring Data `Page` / MyBatis-Plus `IPage` are normalized to `PageResult`; cursor pagination (`CursorResult`) and a `PageResultNormalizer` extension point are provided.
- **Observability**: Micrometer `web.common.exceptions` metric; optional error-code dictionary endpoint and OpenAPI `x-error-codes` extension.
- **i18n**: default error and success messages resolve from `MessageSource` by locale, falling back to enum defaults.
- **Test toolkit**: MockMvc `ResultMatchers` shipped in the main artifact for asserting the envelope contract.

## Quick Start

```xml
<dependency>
    <groupId>io.github.biglv666</groupId>
    <artifactId>web-common-spring-boot-starter</artifactId>
    <version>0.5.0</version>
</dependency>
```

Requires Spring Boot 3.x or 4.x (Servlet web application) and Java 17+. Automatic wrapping of String return values adapts to Jackson 2 (default on Boot 3.x) and Jackson 3 (default on Boot 4) based on the classpath — no extra JSON dependency needed.

### Minimal Example

```java
@RestController
public class UserController {

    @GetMapping("/user/{id}")
    public UserVO getUser(@PathVariable Long id) {   // wrapped as {"code":0,...,"data":{...}}
        return userService.getById(id);
    }

    @GetMapping("/download")
    @NoWrap                                           // raw output, e.g. file downloads
    public byte[] download() { return bytes; }
}
```

```java
// Business failures are wrapped by the global exception handler
throw new BusinessException(ResultCode.NOT_FOUND);
throw BusinessException.of(OrderErrorCode.STOCK_NOT_ENOUGH, "insufficient stock, {} left", 3);
```

Implement `ErrorCode` for custom codes and declare them under `web-common.error-codes`, or use `@ErrorCodeScan` for package scanning. See [Error Codes and Exceptions](docs/error-codes.md) (Chinese).

## Core Configuration

```yaml
web-common:
  enabled: true                 # master switch
  auto-wrap: true               # automatic response wrapping
  http-status-mode: ALWAYS_200  # or SEMANTIC
  error-codes:                  # business error-code enums (or use @ErrorCodeScan)
    - com.example.order.OrderErrorCode
```

For traceId, i18n, log levels and pagination switches, see [Exception Handling and Configuration](docs/exception-handling.md#4-configuration) (Chinese).

## Documentation

Detailed guides are maintained in Chinese:

| Document | Content |
|----------|---------|
| [Result and Wrapping](docs/result-and-wrapping.md) | `Result` shape, `@NoWrap`, async returns, wrapping boundaries |
| [Error Codes and Exceptions](docs/error-codes.md) | `BusinessException`, `@DefaultErrorCode`, custom codes, dictionary endpoint, i18n |
| [Exception Handling and Configuration](docs/exception-handling.md) | Exception coverage, HTTP status modes, all configuration keys, logging |
| [Pagination](docs/pagination.md) | `PageResult`, `CursorResult`, auto-normalization, custom normalizers |
| [Integrations](docs/integrations.md) | Micrometer metrics, test toolkit, OpenAPI, example project |
| [FAQ and Limitations](docs/faq.md) | User-visible limitations, deployment notes, common questions |

Release history: [CHANGELOG.md](CHANGELOG.md).

## Upgrade Note (0.5.0)

Projects returning MyBatis-Plus `IPage` directly will now receive the five core `PageResult` fields instead of the framework object (`orders`, `optimizeCountSql` and other internals are dropped). Set `web-common.pagination.enabled=false` to keep the old shape, or annotate a single endpoint with `@NoWrap`. See [Pagination](docs/pagination.md) (Chinese).

## Compatibility

- Java 17+; Spring Boot 3.x / 4.x (Servlet web application).
- `spring-data-commons`, `mybatis-plus-core`, `micrometer-core`, `swagger-annotations`, etc. are provided + optional and are not transitive; the corresponding feature is skipped when absent.
- The error-code dictionary endpoint is off by default and unauthenticated; use it only on internal networks. See [FAQ and Limitations](docs/faq.md) (Chinese).

## License

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt).
