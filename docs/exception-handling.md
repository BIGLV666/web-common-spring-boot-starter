# 异常处理与配置

[返回 README](../README.md) ｜ [错误码与异常](error-codes.md) ｜ [统一返回与包装](result-and-wrapping.md)

## 1. 全局异常处理覆盖范围

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

## 2. HTTP 状态码模式

默认 HTTP 状态码恒为 200，成败由 `code` 区分。需要网关、监控识别语义化状态码时，可开启 `semantic` 模式：`0`→200；`40400`→404；`500`→500；其余 `4xxxx`→400；`5xxxx` 业务码→200（业务失败不是服务端故障，返回 5xx 会误触发网关告警与重试）。开启后响应体结构不变，但接入方需同步调整网关与监控策略：

```yaml
web-common:
  http-status-mode: SEMANTIC   # 默认 ALWAYS_200，与 0.2.0 契约一致
```

## 3. traceId 透出（默认关闭）

开启后成功与失败响应都携带 traceId，并写 `X-Trace-Id` 响应头，前端报障回传即可对上服务端日志（全局异常日志同步输出 `traceId=`）：

```yaml
web-common:
  trace-id:
    enabled: true
    mdc-key: traceId          # 默认即 traceId，与 Micrometer Tracing / Spring Cloud 约定一致
    response-header: X-Trace-Id  # 置空串可关闭响应头
```

traceId 唯一来源是 MDC：已接入链路追踪时直接复用其值（不覆盖、不清理生命周期）；未接入时内置过滤器生成并在请求结束时清理。Servlet 异步端点的跨线程传递由使用方的链路追踪组件负责，未接入时异步端点可能拿不到 traceId。

<a id="4-configuration"></a>

## 4. 配置项

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
  pagination:
    enabled: true                   # 默认 true；关闭后 Page/IPage 按原对象序列化
```

各异常分支日志均携带请求 URI 与 traceId，级别可按需调整。使用方声明自己的 `GlobalExceptionHandler` / `ResultWrapAdvice` / `ExceptionMetrics` Bean 即可覆盖默认实现。
