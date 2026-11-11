# 分页

[返回 README](../README.md) ｜ [统一返回与包装](result-and-wrapping.md) ｜ [异常处理与配置](exception-handling.md)

## 1. 偏移量分页 PageResult

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
- 其他分页框架：声明 `PageResultNormalizer` Bean 即可接入（见第 3 节）。

> **0.5.0 升级提示**：直接返回 `IPage` 的项目升级后，响应会从框架原始对象变为五个核心字段（`orders`、`optimizeCountSql` 等内部状态不再出现）。需要保持原样时设置 `web-common.pagination.enabled=false`，或给单个接口加 `@NoWrap`。

## 2. 游标分页 CursorResult

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

## 3. 自定义分页框架

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

## 4. PageResult 兼容性

`PageResult` 保留 0.4.0 的两参构造器 `of(records, total)`，源码与二进制兼容；新增的 `current` / `size` / `pages` 为可选元数据。旧 JSON 形状（仅 `total`、`records`）在不传元数据时保持不变。
