# web-common-spring-boot-starter

轻量级 Web 层通用封装 Spring Boot Starter：统一 `Result` 返回体 + 分段错误码枚举 + 全局异常处理 + 响应自动包装。引入依赖即生效，零代码、零配置。

## 快速开始

```xml
<dependency>
    <groupId>io.github.biglv666</groupId>
    <artifactId>web-common-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```

要求：Spring Boot 3.x（Servlet Web 应用）、Java 17+。

## 统一返回结构

HTTP 状态码恒为 200，成败由 `code` 区分：

```json
{ "code": 0, "message": "操作成功", "data": { } }
```

## 响应自动包装（默认开启）

Controller 直接返回业务对象，starter 自动包装为 `Result`，无需手写 `Result.ok(...)`：

```java
@GetMapping("/user/{id}")
public UserVO getUser(@PathVariable Long id) {   // 返回自动变为 {"code":0,...,"data":{...}}
    return userService.getById(id);
}
```

需要原样输出（文件下载、健康检查等）时，在方法或类上标注 `@NoWrap` 豁免。也可显式返回 `Result`，两种风格混用没问题。

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

启动时 starter 会校验**重复码、越段码（只允许 0、500、40000~59999）、保留值（0/500）**，冲突直接启动失败——错误码冲突拖到运行期才暴露是排障灾难。错误码分段约定：`0` 成功；`4xxxx` 客户端侧；`5xxxx` 业务侧；`500` 系统兜底。内置码适用场景见 `ResultCode` 各常量的 Javadoc。

## 全局异常处理覆盖范围

| 异常 | 返回 |
|---|---|
| `BusinessException` | 异常携带的错误码与文案 |
| `@DefaultErrorCode` 注解的自定义异常 | 注解声明的错误码与异常文案 |
| `@Valid` 请求体 / 表单校验失败 | `40000` + 「字段名： 原因」明细 |
| `@Validated` 单参数校验失败 | `40000` + 「参数名： 原因」明细 |
| JSON 反序列化失败 | `40000` 请求体格式错误 |
| 方法不支持 / 参数缺失 / 类型转换失败等 | `40000` |
| 路径不存在 | `40400` |
| 其他未识别异常 | `500` 系统繁忙，堆栈只进日志 |

## 配置项

```yaml
web-common:
  enabled: true                     # 默认 true，设为 false 整体关闭封装
  auto-wrap: true                   # 默认 true，响应自动包装开关
  success-message: 操作成功          # 成功响应文案，可整体替换（如「载入成功」「OK」）
  expose-exception-message: false   # 默认 false；联调时临时开启透出异常消息，生产必须关闭
  error-codes:                      # 业务错误码枚举，启动期冲突校验
    - com.example.order.OrderErrorCode
  log:
    business-level: WARN            # 业务异常日志级别，默认 WARN
    param-level: WARN               # 参数类异常日志级别，默认 WARN
    system-level: ERROR             # 系统兜底异常日志级别，默认 ERROR
```

各异常分支日志均携带请求 URI，级别可按需调整。使用方声明自己的 `GlobalExceptionHandler` / `ResultWrapAdvice` Bean 即可覆盖默认实现。

## API 文档（springdoc / knife4j）

使用方工程引入 springdoc-openapi 或 knife4j 后，`Result` 字段的描述与示例值（来自类上的 Swagger 注解）会自动出现在文档中，无需重复定义返回结构。

## 示例工程

[`examples/demo-app`](examples/demo-app) 是可跑的完整示例（含自建错误码、声明式异常、@NoWrap 等）。先在仓库根目录 `./mvnw install` 安装 starter 到本地仓库，再进入示例目录 `../../mvnw -f pom.xml spring-boot:run`。

## 发布

维护者推送 `v*` 标签即自动发布到 Maven Central（GitHub Actions，见 `.github/workflows/publish.yml`）：

```bash
git tag v0.2.0 && git push origin v0.2.0
```

## 本地构建

```bash
mvnw test        # 运行集成测试（18 个用例）
mvnw package     # 打包（jar + sources + javadoc）
```
