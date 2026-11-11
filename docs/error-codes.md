# 错误码与异常

[返回 README](../README.md) ｜ [统一返回与包装](result-and-wrapping.md) ｜ [异常处理与配置](exception-handling.md)

## 1. 业务异常

业务逻辑中抛 `BusinessException`，由全局异常处理器统一包装为失败返回：

```java
// 内置错误码 + 默认文案
throw new BusinessException(ResultCode.NOT_FOUND);

// 内置错误码 + 自定义文案
throw new BusinessException(ResultCode.CONFLICT, "用户名已被注册");

// 模板文案：{} 占位符按顺序填充
throw BusinessException.of(OrderErrorCode.STOCK_NOT_ENOUGH, "库存不足，剩余 {} 件", 3);
```

## 2. 声明式抛错：已有领域异常零改造接入

已有继承 `RuntimeException` 的领域异常时，标注 `@DefaultErrorCode` 即可，无需改造继承关系：

```java
// 单常量枚举只写类；多常量枚举（如 ResultCode）需指定 constant
@DefaultErrorCode(value = OrderErrorCode.class, constant = "ORDER_NOT_FOUND")
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) { super(message); }
}
```

响应 code 取注解声明的错误码，message 取异常自身文案（为空回退错误码默认文案）。

## 3. 自定义错误码

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

## 4. 错误码字典端点

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

## 5. 文案 i18n（默认开启）

错误码默认文案与成功文案支持多语言：约定 key `web-common.error.{code}`（如 `web-common.error.51001`）与 `web-common.success`，starter 按请求 locale 从容器 `MessageSource` 解析，**查不到即回退枚举默认文案**——不配语言资源时零影响，配置后按需覆盖：

```properties
# i18n/messages_en.properties，配合 spring.messages.basename=i18n/messages
web-common.success=Success
web-common.error.51001=Stock not enough
```

解析范围仅限「默认文案」：`BusinessException` 的自定义文案、模板填充后的文案原样透出，参数化文案的多语言由业务层负责；参数校验明细仍走 Hibernate Validator 自身机制。locale 取 `LocaleContextHolder`（尊重使用方配置的 LocaleResolver，缺省按 Accept-Language）。开关：`web-common.i18n.enabled`（默认 true）。

## 6. 覆盖默认实现

使用方声明自己的 `GlobalExceptionHandler` / `ResultWrapAdvice` / `ExceptionMetrics` / `ErrorCodeRegistry` Bean 即可覆盖对应默认实现。错误码字典若需自定义输出形态，推荐自建 Controller 读取 `ErrorCodeRegistry`。
