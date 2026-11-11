# 集成能力

[返回 README](../README.md) ｜ [异常处理与配置](exception-handling.md) ｜ [FAQ 与限制](faq.md)

## 1. 可观测（Micrometer 异常指标）

类路径存在 micrometer（引入 actuator 即有）时，失败响应自动打点：计数器 `web.common.exceptions`，tag `code`（错误码数值）+ `category`（`param`/`business`/`system` 分段归类）。刻意不携带 uri 等高基数标签。需要接入自有监控时，声明自己的 `ExceptionMetrics` Bean 即可替换。

## 2. 测试工具包

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

## 3. API 文档（springdoc / knife4j）

使用方工程引入 springdoc-openapi 或 knife4j 后，`Result`、`PageResult` 字段的描述与示例值（来自类上的 Swagger 注解）会自动出现在文档中，无需重复定义返回结构。

开启 `@EnableErrorCodeEndpoint` 的同时（或单独配置 `web-common.error-codes`），全量错误码字典还会以 `x-error-codes` 扩展挂在 OpenAPI 文档根节点，前端从 swagger 文档即可消费：

```json
{
  "openapi": "3.1.0",
  "x-error-codes": [
    { "code": 40000, "message": "参数错误", "enumClass": "...ResultCode", "constant": "PARAM_ERROR" },
    { "code": 51001, "message": "库存不足", "enumClass": "com.example.order.OrderErrorCode", "constant": "STOCK_NOT_ENOUGH" }
  ]
}
```

springdoc / knife4j 的文档端点（`/v3/api-docs`、swagger-ui 资源等）自动豁免响应包装，不影响文档消费。

## 4. 示例工程

[`examples/demo-app`](../examples/demo-app) 是可跑的完整示例（含自建错误码、声明式异常、@NoWrap、PageResult / CursorResult、traceId、i18n 文案等）。

先在仓库根目录把 starter 安装到本地仓库：

```bash
./mvnw install
```

再进入示例目录运行：

```bash
cd examples/demo-app
../../mvnw -f pom.xml spring-boot:run
```

## 5. 与 state-kit 的协同

类路径同时存在 state-kit 时，其状态异常类上的 `@DefaultErrorCode` 注解会被本 starter 的全局异常处理消费，业务无需 try-catch 即可得到统一 `Result`。
