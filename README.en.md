# web-common-spring-boot-starter

Lightweight Spring Boot starter for the web layer: a unified `Result` response body + segmented error-code enums + global exception handling + automatic response wrapping. Works out of the box — zero code, zero configuration.

[简体中文](README.md)

## Quick Start

```xml
<dependency>
    <groupId>io.github.biglv666</groupId>
    <artifactId>web-common-spring-boot-starter</artifactId>
    <version>0.5.0</version>
</dependency>
```

Requires Spring Boot 3.x or 4.x (Servlet web application) and Java 17+. Automatic wrapping of String return values adapts to Jackson 2 (default on Boot 3.x) and Jackson 3 (default on Boot 4) based on the classpath — no extra JSON dependency needed.

## Unified Response Structure

HTTP status is always 200; success or failure is indicated by `code`:

```json
{ "code": 0, "message": "操作成功", "data": { } }
```

With trace-id exposure enabled (off by default), responses also carry an optional `traceId` field matching the `X-Trace-Id` response header; when disabled the field is absent and the structure is unchanged.

## Automatic Response Wrapping (enabled by default)

Controllers return business objects directly; the starter wraps them into `Result` automatically, no `Result.ok(...)` boilerplate:

```java
@GetMapping("/user/{id}")
public UserVO getUser(@PathVariable Long id) {   // response becomes {"code":0,...,"data":{...}}
    return userService.getById(id);
}
```

When raw output is needed (file downloads, health checks, etc.), annotate the method or class with `@NoWrap` to opt out. Explicitly returning `Result` also works; both styles mix freely.

Async return types (`CompletableFuture` / `Callable` / `DeferredResult`) are wrapped the same way: Spring MVC re-enters message conversion with the resolved value after async completion, so wrapping behaves identically to synchronous returns. springdoc / knife4j documentation endpoints (`/v3/api-docs`, swagger-ui resources, etc.) are automatically exempt so documentation keeps working.

## Business Exceptions

Throw a `BusinessException` in business logic; the global exception handler turns it into a unified failure response:

```java
// Built-in error code + default message
throw new BusinessException(ResultCode.NOT_FOUND);

// Built-in error code + custom message
throw new BusinessException(ResultCode.CONFLICT, "username already taken");

// Message template: {} placeholders filled in order
throw BusinessException.of(OrderErrorCode.STOCK_NOT_ENOUGH, "insufficient stock, {} left", 3);
```

## Declarative Error Mapping: adopt existing domain exceptions without refactoring

Already have domain exceptions extending `RuntimeException`? Annotate them with `@DefaultErrorCode` — no inheritance changes needed:

```java
// For single-constant enums, the class alone is enough; multi-constant enums (like ResultCode) require `constant`
@DefaultErrorCode(value = OrderErrorCode.class, constant = "ORDER_NOT_FOUND")
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) { super(message); }
}
```

The response `code` comes from the annotation, and `message` from the exception itself (falling back to the error code's default message when empty).

## Custom Error Codes

Implement the `ErrorCode` interface with a new enum and declare it under `web-common.error-codes` to join the startup-time validation:

```java
public enum OrderErrorCode implements ErrorCode {
    STOCK_NOT_ENOUGH(51001, "insufficient stock"),
    ORDER_ALREADY_PAID(51002, "order already paid");

    // implement getCode() / getMessage()
}
```

To skip listing class names one by one, annotate any `@Configuration` class with `@ErrorCodeScan` to register `ErrorCode` enums by package scan (defaults to the annotated class's package). Both approaches can be combined:

```java
@Configuration
@ErrorCodeScan("com.example.order")
public class WebConfig { }
```

At startup the starter validates **duplicate codes, out-of-segment codes (only 0, 500, and 40000~59999 are allowed), and reserved values (0/500)**; any conflict fails startup immediately — error-code conflicts discovered at runtime are a debugging disaster. Segment conventions: `0` success; `4xxxx` client-side; `5xxxx` business-side; `500` system fallback. See the Javadoc on each `ResultCode` constant for when to use the built-in codes.

## Error-Code Dictionary Endpoint

Error codes live across business enums; syncing them to frontend and QA by hand invites mistakes. Annotate any `@Configuration` class with `@EnableErrorCodeEndpoint` to expose `GET /web-common/error-codes`:

```java
@Configuration
@EnableErrorCodeEndpoint
public class WebConfig { }
```

The endpoint returns every error code that passed startup validation (built-in + business-declared, with default messages and source enums, sorted by code) for frontend, QA, and gateway consumption. Off by default; annotate to enable — no configuration needed:

```json
{
  "code": 0, "message": "操作成功",
  "data": [
    { "code": 40000, "message": "参数错误", "enumClass": "...ResultCode", "constant": "PARAM_ERROR" },
    { "code": 51001, "message": "库存不足", "enumClass": "com.example.order.OrderErrorCode", "constant": "STOCK_NOT_ENOUGH" }
  ]
}
```

If the fixed path conflicts with business routes, or a custom output shape is needed, declare your own controller, inject `ErrorCodeRegistry`, and call `descriptors()` to read the same dictionary.

> Deployment note: the endpoint is unauthenticated and its output includes internal enum class names. It is meant for frontend, QA, and gateway use on internal networks during integration; do not expose `/web-common/error-codes` on a public gateway route. If you must serve it publicly, use your own controller and put your own authentication in front of it.

## Message i18n (enabled by default)

Default error messages and the success message support localization: with the key conventions `web-common.error.{code}` (e.g. `web-common.error.51001`) and `web-common.success`, the starter resolves messages per request locale from the container `MessageSource` and **falls back to the enum's default message when the key is missing** — no impact without language resources, opt-in coverage once configured:

```properties
# i18n/messages_en.properties, together with spring.messages.basename=i18n/messages
web-common.success=Success
web-common.error.51001=Stock not enough
```

Only "default messages" are resolved: custom `BusinessException` messages and template-filled messages pass through as-is (parameterized localization is the business layer's job); validation details keep going through Hibernate Validator's own mechanism. The locale comes from `LocaleContextHolder` (respects the consumer's LocaleResolver, defaults to Accept-Language). Switch: `web-common.i18n.enabled` (default true).

## Trace-Id Exposure (off by default)

When enabled, both success and failure responses carry a traceId and the `X-Trace-Id` response header, so frontend incident reports map straight to server logs (global exception logs emit `traceId=` as well):

```yaml
web-common:
  trace-id:
    enabled: true
    mdc-key: traceId          # default traceId, matching Micrometer Tracing / Spring Cloud conventions
    response-header: X-Trace-Id  # set to empty string to disable the header
```

The traceId's single source is the MDC: with an existing tracing solution its value is reused as-is (never overwritten or cleaned up); without one, a built-in filter generates it and cleans up at request end. Cross-thread propagation for Servlet async endpoints is the tracing solution's responsibility — without tracing, async endpoints may lack a traceId.

## Pagination

### Offset pagination: PageResult

The unified shape is `{total, records, current, size, pages}`; with automatic wrapping the response is `{"code":0,"data":{"total":15,"records":[...],"current":2,"size":2,"pages":8}}`. `current` is 1-based and `pages` is `ceil(total / size)`. The three metadata fields are optional: `PageResult.of(records, total)` outputs only `{total, records}`, exactly the 0.4.0 shape.

**Automatic normalization (on by default)**: when a controller returns a Spring Data `Page` or a MyBatis-Plus `IPage`, the starter converts it to the unified structure — no manual `PageResult.from` needed:

```java
@GetMapping("/users")
public Page<UserVO> list(Pageable pageable) {
    return userService.page(pageable);   // becomes {total, records, current, size, pages}
}

@GetMapping("/users-mp")
public IPage<UserVO> listMp() {
    return userService.pageMp(...);      // MyBatis-Plus is normalized the same way
}
```

- Spring Data page numbers are 0-based and become 1-based; both frameworks compute `pages` as `ceil(total / size)`.
- MyBatis-Plus keeps only the five core fields and drops query-object internals such as `orders` and `optimizeCountSql`.
- Explicit `Result.ok(page)`, `ResponseEntity<Page>` and async `Callable<Page>` are normalized as well.
- Disable globally with `web-common.pagination.enabled=false`; exempt a single endpoint with `@NoWrap`.
- Without Spring Data / MyBatis-Plus on the classpath, normalization is skipped and no hard dependency is added.
- Other pagination frameworks: declare a `PageResultNormalizer` bean (see below).

> **Upgrading to 0.5.0**: projects returning `IPage` directly will see the five core fields instead of the framework object (`orders`, `optimizeCountSql` and other internals are dropped). Set `web-common.pagination.enabled=false` to keep the old shape, or annotate a single endpoint with `@NoWrap`.

### Cursor pagination: CursorResult

Cursor pagination omits `total` and fits infinite scrolling, large datasets and count-free queries:

```java
@GetMapping("/messages")
public CursorResult<MessageVO> list(@RequestParam(required = false) String cursor) {
    List<MessageVO> records = messageService.next(cursor, 20);
    return records.size() < 20
            ? CursorResult.last(records)
            : CursorResult.of(records, messageService.cursorOf(records.get(records.size() - 1)));
}
```

Response:

```json
{ "code": 0, "data": { "records": [ ], "nextCursor": "eyJpZCI6MTAwfQ==", "hasNext": true } }
```

- On the last page `nextCursor` is `null` and `hasNext` is `false`; `hasNext` is a computed property that always equals `nextCursor != null`.
- Cursor content is encoded by the application (id, id + timestamp, Base64 JSON, etc.); the starter defines no format and performs no encryption/signing.
- Cursor pagination is intentionally not auto-normalized: Spring Data `Slice` has no cursor and MyBatis-Plus has no cursor type. Building it explicitly avoids a "has next page but no cursor" structure.

### Custom pagination frameworks

```java
@Bean
public PageResultNormalizer myPageNormalizer() {
    return new PageResultNormalizer() {
        public boolean supports(Object body) { return body instanceof MyPage<?>; }
        public PageResult<?> normalize(Object body) {
            MyPage<?> page = (MyPage<?>) body;
            return PageResult.of(page.getList(), page.getTotal(), page.getPageNo(), page.getPageSize());
        }
    };
}
```

Implementations are ordered by `@Order`; to override a built-in Spring Data / MyBatis-Plus type, give your normalizer a higher-precedence `@Order`. Implementations must be stateless and thread-safe.

## Observability (Micrometer exception metrics)

When micrometer is on the classpath (present once actuator is included), every failed response increments a counter named `web.common.exceptions` with tags `code` (numeric code) and `category` (`param`/`business`/`system` segment classification). High-cardinality tags like uri are deliberately excluded. Declare your own `ExceptionMetrics` bean to plug into a custom monitoring stack.

## Test Toolkit

MockMvc assertion helpers for this starter's envelope contract, keeping test code focused on business semantics (shipped in the main artifact; reference it directly from test code):

```java
import static io.github.biglv666.webcommon.test.ResultMatchers.*;

mockMvc.perform(get("/users/1"))
        .andExpect(status().isOk())
        .andDo(success());                        // code=0

mockMvc.perform(get("/orders/1"))
        .andDo(error(51001, "stock"));            // code + message fragment

mockMvc.perform(post("/users", badBody))
        .andDo(paramError("age", "positive"));    // per-field validation detail assertion

mockMvc.perform(get("/users/1"))                  // with trace-id enabled
        .andDo(traceId());                        // field matches the X-Trace-Id header
```

## Global Exception Handling Coverage

| Exception | Response |
|---|---|
| `BusinessException` | code and message carried by the exception |
| Custom exceptions annotated with `@DefaultErrorCode` | code declared by the annotation and the exception's message |
| `@Valid` request-body / form validation failure | `40000` + 「field: reason」detail, with a structured detail list in `data` |
| `@Validated` single-parameter validation failure | `40000` + 「parameter: reason」detail, with a structured detail list in `data` |
| Constraint annotations directly on method parameters (Spring 6.1+ built-in validation) | `40000` + 「parameter: reason」detail, with a structured detail list in `data` |
| JSON deserialization failure | `40000` malformed request body (type mismatches append the offending field path, e.g. `field orders[0].count type mismatch`) |
| Upload file exceeding size limit | `40000` uploaded file too large |
| Unsupported method / missing parameter / type conversion failure, etc. | `40000` |
| Path not found (incl. Spring 6.1+ static resource misses) | `40400` |
| Any other unrecognized exception | `500` system busy; stack trace goes to logs only |

Structured validation details look like `"data": [{ "field": "age", "message": "年龄必须为正数" }]` so frontends can highlight form fields directly; the joined text in `message` is unchanged for logs and human readers. In other failure scenarios `data` is omitted. Deserialization field paths only expose JSON field names — no target types or internal class names.

## HTTP Status Code Modes

By default HTTP status is always 200 and success is judged by `code`. To let gateways and monitoring recognize semantic status codes, enable `semantic` mode: `0`→200; `40400`→404; `500`→500; other `4xxxx`→400; `5xxxx` business codes→200 (business failures are not server-side faults; returning 5xx would falsely trigger gateway alerts and retries). The response body shape is unchanged, but consumers must adjust gateway and monitoring policies accordingly:

```yaml
web-common:
  http-status-mode: SEMANTIC   # default ALWAYS_200, consistent with the 0.2.0 contract
```

## Configuration

```yaml
web-common:
  enabled: true                     # default true; set false to disable the whole starter
  auto-wrap: true                   # default true; toggles automatic response wrapping
  success-message: 操作成功          # success message, fully replaceable (e.g. "OK")
  http-status-mode: ALWAYS_200      # default always 200; SEMANTIC maps semantic status codes by code segment
  expose-exception-message: false   # default false; enable temporarily while debugging, MUST be off in production
  error-codes:                      # business error-code enums, validated at startup (or use @ErrorCodeScan)
    - com.example.order.OrderErrorCode
  trace-id:
    enabled: false                  # default false; when on, responses carry traceId and the X-Trace-Id header
    mdc-key: traceId                # MDC key for the traceId, matching mainstream tracing conventions
    response-header: X-Trace-Id     # response header name, empty string disables it
  i18n:
    enabled: true                   # default true; resolves web-common.error.{code} via MessageSource, falls back when missing
  log:
    business-level: WARN            # log level for business exceptions, default WARN
    param-level: WARN               # log level for parameter-type exceptions, default WARN
    system-level: ERROR             # log level for system fallback exceptions, default ERROR
```

Every exception branch logs the request URI and traceId, and levels are adjustable. Consumers may declare their own `GlobalExceptionHandler` / `ResultWrapAdvice` / `ExceptionMetrics` beans to override the defaults.

## API Documentation (springdoc / knife4j)

Once the consumer project has springdoc-openapi or knife4j, the descriptions and example values on `Result` and `PageResult` fields (from class-level Swagger annotations) appear in the generated docs automatically — no need to redefine the response structure. With `@EnableErrorCodeEndpoint` enabled (or `web-common.error-codes` configured on its own), the full error-code dictionary is also attached to the OpenAPI document root as an `x-error-codes` extension for frontend consumption straight from the swagger document:

```json
{
  "openapi": "3.1.0",
  "x-error-codes": [
    { "code": 40000, "message": "参数错误", "enumClass": "...ResultCode", "constant": "PARAM_ERROR" },
    { "code": 51001, "message": "库存不足", "enumClass": "com.example.order.OrderErrorCode", "constant": "STOCK_NOT_ENOUGH" }
  ]
}
```

## Example Project

[`examples/demo-app`](examples/demo-app) is a runnable, complete example (custom error codes, declarative exceptions, `@NoWrap`, PageResult / CursorResult, traceId, i18n messages, etc.). First run `./mvnw install` at the repository root to install the starter locally, then run `../../mvnw -f pom.xml spring-boot:run` from the example directory.

## Release

Maintainers push a `v*` tag to publish to Maven Central automatically (GitHub Actions, see `.github/workflows/publish.yml`); pushes to `main` and pull requests run the full test suite via `.github/workflows/build.yml`:

```bash
git tag v0.5.0 && git push origin v0.5.0
```

## Local Build

```bash
mvnw test        # run the integration test suite (124 test cases)
mvnw package     # package (jar + sources + javadoc)
```
