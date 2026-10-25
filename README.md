# web-common-spring-boot-starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.biglv666/web-common-spring-boot-starter)](https://central.sonatype.com/artifact/io.github.biglv666/web-common-spring-boot-starter) [![Build](https://github.com/BIGLV666/web-common-spring-boot-starter/actions/workflows/build.yml/badge.svg)](https://github.com/BIGLV666/web-common-spring-boot-starter/actions/workflows/build.yml)

轻量级 Web 层通用封装 Spring Boot Starter：统一 `Result` 返回体 + 分段错误码枚举 + 全局异常处理 + 响应自动包装。引入依赖即生效，零代码、零配置。

[English](README.en.md)

## 快速开始

```xml
<dependency>
    <groupId>io.github.biglv666</groupId>
    <artifactId>web-common-spring-boot-starter</artifactId>
    <version>0.5.0</version>
</dependency>
```

要求：Spring Boot 3.x 或 4.x（Servlet Web 应用）、Java 17+。String 返回值的自动包装序列化会按类路径自动适配 Jackson 2（Boot 3.x 默认）与 Jackson 3（Boot 4 默认），使用方无需额外引入 JSON 依赖。

## 统一返回结构

HTTP 状态码恒为 200，成败由 `code` 区分：

```json
{ "code": 0, "message": "操作成功", "data": { } }
```

开启 traceId 透出后（默认关闭），响应会额外携带可选的 `traceId` 字段，与 `X-Trace-Id` 响应头一致；未开启时字段不出现在 JSON 中，结构不变。

## 响应自动包装（默认开启）

Controller 直接返回业务对象，starter 自动包装为 `Result`，无需手写 `Result.ok(...)`：

```java
@GetMapping("/user/{id}")
public UserVO getUser(@PathVariable Long id) {   // 返回自动变为 {"code":0,...,"data":{...}}
    return userService.getById(id);
}
```

需要原样输出（文件下载、健康检查等）时，在方法或类上标注 `@NoWrap` 豁免。也可显式返回 `Result`，两种风格混用没问题。

异步返回类型（`CompletableFuture` / `Callable` / `DeferredResult`）同样自动包装：Spring MVC 在异步完成后携带解析值进入消息转换，包装行为与同步返回一致。springdoc / knife4j 的文档端点（`/v3/api-docs`、swagger-ui 资源等）自动豁免，不影响文档消费。

## 业务异常

业务逻辑中抛 `BusinessException`，由全局异常处理器统一包装为失败返回：

```java
// 内置错误码 + 默认文案
throw new BusinessException(ResultCode.NOT_FOUND);

// 内置错误码 + 自定义文案
throw new BusinessException(ResultCode.CONFLICT, "用户名已被注册");

// 模板文案：{} 占位符按顺序填充
throw BusinessException.of(OrderErrorCode.STOCK_NOT_ENOUGH, "库存不足，剩余 {} 件", 3);
```

## 声明式抛错：已有领域异常零改造接入

已有继承 `RuntimeException` 的领域异常时，标注 `@DefaultErrorCode` 即可，无需改造继承关系：

```java
// 单常量枚举只写类；多常量枚举（如 ResultCode）需指定 constant
@DefaultErrorCode(value = OrderErrorCode.class, constant = "ORDER_NOT_FOUND")
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) { super(message); }
}
```

响应 code 取注解声明的错误码，message 取异常自身文案（为空回退错误码默认文案）。

## 自定义错误码

实现 `ErrorCode` 接口新建枚举，并声明到 `web-common.error-codes` 参与启动期校验：

```java
public enum OrderErrorCode implements ErrorCode {
    STOCK_NOT_ENOUGH(51001, "库存不足"),
    ORDER_ALREADY_PAID(51002, "订单已支付，请勿重复支付");

    // 实现 getCode() / getMessage()
}
```

嫌逐个写类名麻烦时，也可以在任意 `@Configuration` 配置类上标注 `@ErrorCodeScan`，按包扫描 `ErrorCode` 枚举自动注册（未指定包时默认扫描标注类所在包），两种方式可并存：

```java
@Configuration
@ErrorCodeScan("com.example.order")
public class WebConfig { }
```

启动时 starter 会校验**重复码、越段码（只允许 0、500、40000~59999）、保留值（0/500）**，冲突直接启动失败——错误码冲突拖到运行期才暴露是排障灾难。错误码分段约定：`0` 成功；`4xxxx` 客户端侧；`5xxxx` 业务侧；`500` 系统兜底。内置码适用场景见 `ResultCode` 各常量的 Javadoc。

## 错误码字典端点

错误码分散在各业务枚举里，靠人肉同步给前端和测试容易错漏。在任意 `@Configuration` 配置类上标注 `@EnableErrorCodeEndpoint`，即可开启 `GET /web-common/error-codes`：

```java
@Configuration
@EnableErrorCodeEndpoint
public class WebConfig { }
```

端点输出启动期校验通过的全量错误码（内置码 + 业务声明码，含默认文案与来源枚举，按 code 升序），前端、测试与网关直接取用，无需任何配置；默认关闭，标注即开启：

```json
{
  "code": 0, "message": "操作成功",
  "data": [
    { "code": 40000, "message": "参数错误", "enumClass": "...ResultCode", "constant": "PARAM_ERROR" },
    { "code": 51001, "message": "库存不足", "enumClass": "com.example.order.OrderErrorCode", "constant": "STOCK_NOT_ENOUGH" }
  ]
}
```

固定路径与业务路由冲突或需要自定义输出形态时，可自建 Controller 注入 `ErrorCodeRegistry`，调用 `descriptors()` 读取同一份字典。

> 部署提示：端点未做鉴权，且输出内容包含内部枚举类全限定名。仅供前端、测试与网关在内网联调时取用，不要将 `/web-common/error-codes` 暴露到公网网关的公开路由；确有公网需求时改用自建 Controller，套上自己的鉴权后再输出。

## 文案 i18n（默认开启）

错误码默认文案与成功文案支持多语言：约定 key `web-common.error.{code}`（如 `web-common.error.51001`）与 `web-common.success`，starter 按请求 locale 从容器 `MessageSource` 解析，**查不到即回退枚举默认文案**——不配语言资源时零影响，配置后按需覆盖：

```properties
# i18n/messages_en.properties，配合 spring.messages.basename=i18n/messages
web-common.success=Success
web-common.error.51001=Stock not enough
```

解析范围仅限「默认文案」：`BusinessException` 的自定义文案、模板填充后的文案原样透出，参数化文案的多语言由业务层负责；参数校验明细仍走 Hibernate Validator 自身机制。locale 取 `LocaleContextHolder`（尊重使用方配置的 LocaleResolver，缺省按 Accept-Language）。开关：`web-common.i18n.enabled`（默认 true）。

## traceId 透出（默认关闭）

开启后成功与失败响应都携带 traceId，并写 `X-Trace-Id` 响应头，前端报障回传即可对上服务端日志（全局异常日志同步输出 `traceId=`）：

```yaml
web-common:
  trace-id:
    enabled: true
    mdc-key: traceId          # 默认即 traceId，与 Micrometer Tracing / Spring Cloud 约定一致
    response-header: X-Trace-Id  # 置空串可关闭响应头
```

traceId 唯一来源是 MDC：已接入链路追踪时直接复用其值（不覆盖、不清理生命周期）；未接入时内置过滤器生成并在请求结束时清理。Servlet 异步端点的跨线程传递由使用方的链路追踪组件负责，未接入时异步端点可能拿不到 traceId。

## 分页

### 偏移量分页 PageResult

统一形状 `{total, records, current, size, pages}`，自动包装后即 `{"code":0,"data":{"total":15,"records":[...],"current":2,"size":2,"pages":8}}`。`current` 从 1 开始，`pages` 按 `ceil(total / size)` 计算；三个元数据字段可选，手动 `PageResult.of(records, total)` 时只输出 `{total, records}`，与 0.4.0 形状一致。

**自动归一（默认开启）**：Controller 直接返回 Spring Data `Page` 或 MyBatis-Plus `IPage` 时，starter 自动转换为统一结构，无需手写 `PageResult.from`：

```java
@GetMapping("/users")
public Page<UserVO> list(Pageable pageable) {
    return userService.page(pageable);   // 自动变成 {total, records, current, size, pages}
}

@GetMapping("/users-mp")
public IPage<UserVO> listMp() {
    return userService.pageMp(...);      // MyBatis-Plus 同样自动归一
}
```

- Spring Data 页码 0-based 转 1-based；两个框架的总页数统一按 `ceil(total / size)` 计算。
- MyBatis-Plus 只保留五个核心字段，丢弃 `orders`、`optimizeCountSql` 等查询对象内部状态。
- 显式 `Result.ok(page)`、`ResponseEntity<Page>`、异步 `Callable<Page>` 同样归一。
- 关闭：`web-common.pagination.enabled=false`；单接口豁免：`@NoWrap`。
- 未引入 Spring Data / MyBatis-Plus 时自动跳过，不产生任何强制依赖。
- 其他分页框架：声明 `PageResultNormalizer` Bean 即可接入（见下文）。

> **0.5.0 升级提示**：直接返回 `IPage` 的项目升级后，响应会从框架原始对象变为五个核心字段（`orders`、`optimizeCountSql` 等内部状态不再出现）。需要保持原样时设置 `web-common.pagination.enabled=false`，或给单个接口加 `@NoWrap`。

### 游标分页 CursorResult

游标分页不返回 `total`，适合无限滚动、大数据量、避免 count 查询：

```java
@GetMapping("/messages")
public CursorResult<MessageVO> list(@RequestParam(required = false) String cursor) {
    List<MessageVO> records = messageService.next(cursor, 20);
    return records.size() < 20
            ? CursorResult.last(records)
            : CursorResult.of(records, messageService.cursorOf(records.get(records.size() - 1)));
}
```

响应：

```json
{ "code": 0, "data": { "records": [ ], "nextCursor": "eyJpZCI6MTAwfQ==", "hasNext": true } }
```

- 末页 `nextCursor=null`、`hasNext=false`；`hasNext` 是计算属性，恒等于 `nextCursor != null`。
- 游标内容由业务编码（id、id + 时间、Base64 JSON 等），starter 不定义格式、不做加解密与签名。
- 游标分页不做框架自动归一：Spring Data `Slice` 没有游标，MyBatis-Plus 没有游标类型；由业务显式构造，避免生成“有下一页但拿不到游标”的假结构。

### 自定义分页框架

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

多个实现按 `@Order` 排序；需要覆盖内置的 Spring Data / MyBatis-Plus 类型时，给自定义实现加 `@Order` 提高优先级。实现必须无状态、线程安全。

## 可观测（Micrometer 异常指标）

类路径存在 micrometer（引入 actuator 即有）时，失败响应自动打点：计数器 `web.common.exceptions`，tag `code`（错误码数值）+ `category`（`param`/`business`/`system` 分段归类）。刻意不携带 uri 等高基数标签。需要接入自有监控时，声明自己的 `ExceptionMetrics` Bean 即可替换。

## 测试工具包

针对本 starter 包络契约的 MockMvc 断言工具，让测试代码聚焦业务语义（主 artifact 携带，测试代码直接引用即可）：

```java
import static io.github.biglv666.webcommon.test.ResultMatchers.*;

mockMvc.perform(get("/users/1"))
        .andExpect(status().isOk())
        .andDo(success());                        // code=0

mockMvc.perform(get("/orders/1"))
        .andDo(error(51001, "库存不足"));          // code + message 片段

mockMvc.perform(post("/users", badBody))
        .andDo(paramError("age", "正数"));         // 校验明细按字段断言

mockMvc.perform(get("/users/1"))                  // trace-id 开启时
        .andDo(traceId());                        // 字段与 X-Trace-Id 头一致
```

## 全局异常处理覆盖范围

| 异常 | 返回 |
|---|---|
| `BusinessException` | 异常携带的错误码与文案 |
| `@DefaultErrorCode` 注解的自定义异常 | 注解声明的错误码与异常文案 |
| `@Valid` 请求体 / 表单校验失败 | `40000` + 「字段名： 原因」明细，`data` 携带结构化明细列表 |
| `@Validated` 单参数校验失败 | `40000` + 「参数名： 原因」明细，`data` 携带结构化明细列表 |
| 方法参数直接标注约束注解（Spring 6.1+ 内置校验） | `40000` + 「参数名： 原因」明细，`data` 携带结构化明细列表 |
| JSON 反序列化失败 | `40000` 请求体格式错误（类型不匹配时 message 附出错字段路径，如 `字段 orders[0].count 类型不匹配`） |
| 上传文件超出大小上限 | `40000` 上传文件过大 |
| 方法不支持 / 参数缺失 / 类型转换失败等 | `40000` |
| 路径不存在（含 Spring 6.1+ 静态资源未命中） | `40400` |
| 其他未识别异常 | `500` 系统繁忙，堆栈只进日志 |

校验类失败的结构化明细形如 `"data": [{ "field": "age", "message": "年龄必须为正数" }]`，前端可按字段直接标红表单；message 中的拼接文本保持不变，供日志与人读。其余失败场景 `data` 不输出。反序列化字段路径只透出 JSON 字段名，不泄露目标类型与内部类名。

## HTTP 状态码模式

默认 HTTP 状态码恒为 200，成败由 `code` 区分。需要网关、监控识别语义化状态码时，可开启 `semantic` 模式：`0`→200；`40400`→404；`500`→500；其余 `4xxxx`→400；`5xxxx` 业务码→200（业务失败不是服务端故障，返回 5xx 会误触发网关告警与重试）。开启后响应体结构不变，但接入方需同步调整网关与监控策略：

```yaml
web-common:
  http-status-mode: SEMANTIC   # 默认 ALWAYS_200，与 0.2.0 契约一致
```

## 配置项

```yaml
web-common:
  enabled: true                     # 默认 true，设为 false 整体关闭封装
  auto-wrap: true                   # 默认 true，响应自动包装开关
  success-message: 操作成功          # 成功响应文案，可整体替换（如「载入成功」「OK」）
  http-status-mode: ALWAYS_200      # 默认恒为 200；SEMANTIC 按错误码段映射语义化状态码
  expose-exception-message: false   # 默认 false；联调时临时开启透出异常消息，生产必须关闭
  error-codes:                      # 业务错误码枚举，启动期冲突校验（也可用 @ErrorCodeScan）
    - com.example.order.OrderErrorCode
  trace-id:
    enabled: false                  # 默认 false；开启后响应携带 traceId 并写 X-Trace-Id 头
    mdc-key: traceId                # traceId 的 MDC key，与主流链路追踪约定一致
    response-header: X-Trace-Id     # 响应头名，空串关闭
  i18n:
    enabled: true                   # 默认 true；按 web-common.error.{code} 从 MessageSource 解析，缺失回退
  log:
    business-level: WARN            # 业务异常日志级别，默认 WARN
    param-level: WARN               # 参数类异常日志级别，默认 WARN
    system-level: ERROR             # 系统兜底异常日志级别，默认 ERROR
```

各异常分支日志均携带请求 URI 与 traceId，级别可按需调整。使用方声明自己的 `GlobalExceptionHandler` / `ResultWrapAdvice` / `ExceptionMetrics` Bean 即可覆盖默认实现。

## API 文档（springdoc / knife4j）

使用方工程引入 springdoc-openapi 或 knife4j 后，`Result`、`PageResult` 字段的描述与示例值（来自类上的 Swagger 注解）会自动出现在文档中，无需重复定义返回结构。开启 `@EnableErrorCodeEndpoint` 的同时（或单独配置 `web-common.error-codes`），全量错误码字典还会以 `x-error-codes` 扩展挂在 OpenAPI 文档根节点，前端从 swagger 文档即可消费：

```json
{
  "openapi": "3.1.0",
  "x-error-codes": [
    { "code": 40000, "message": "参数错误", "enumClass": "...ResultCode", "constant": "PARAM_ERROR" },
    { "code": 51001, "message": "库存不足", "enumClass": "com.example.order.OrderErrorCode", "constant": "STOCK_NOT_ENOUGH" }
  ]
}
```

## 示例工程

[`examples/demo-app`](examples/demo-app) 是可跑的完整示例（含自建错误码、声明式异常、@NoWrap、PageResult / CursorResult、traceId、i18n 文案等）。先在仓库根目录 `./mvnw install` 安装 starter 到本地仓库，再进入示例目录 `../../mvnw -f pom.xml spring-boot:run`。

## 发布

维护者推送 `v*` 标签即自动发布到 Maven Central（GitHub Actions，见 `.github/workflows/publish.yml`）：

```bash
git tag v0.5.0 && git push origin v0.5.0
```

## 本地构建

```bash
mvnw test        # 运行集成测试（124 个用例）
mvnw package     # 打包（jar + sources + javadoc）
```
