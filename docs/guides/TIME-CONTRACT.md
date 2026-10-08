# 时间交互与 UTC 规范

时间点描述绝对瞬间；日期、每天几点、Cron 和 Duration 保留各自语义。

## 接口

- JSON、query、form 时间点统一使用带 `Z` 或 `±HH:mm` 的 ISO-8601，后端响应输出 UTC `Z`，保留字段精度。
- 不接受无时区、旧 `yyyy-MM-dd HH:mm:ss`、非法日期或数字形式的普通 API 时间点；错误返回 400，不推测上海时区。
- 新 DTO 时间点优先 `Instant`。已有 `LocalDateTime` 时间点明确表示 UTC，框架边界转换；`Date` 同样输出 UTC。
- `LocalDate` 用 yyyy-MM-dd，`LocalTime` 用 HH:mm:ss，Duration 遵循其契约，不转换成时间点。
- JWT NumericDate、OAuth expires_in、TTL 和已有内部 epoch seconds/millis 遵循协议，不改成 ISO 字符串。

## 后端与数据库

- 到期判断、锁定、授权截止和时间差使用 Instant 或 UTC 时钟；区间默认起点包含、终点不包含。
- 存储 LocalDateTime 的时间点值为 UTC，写入用 `DateUtil.utc()` 或 `LocalDateTime.ofInstant(value, ZoneOffset.UTC)`，读取用 `value.toInstant(ZoneOffset.UTC)`。
- 业务 JDBC 连接使用 UTC；MySQL 配置 `connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`，每个连接池/数据源单独遵守。
- 连接 UTC 不会替业务代码转换一个错误的本地 LocalDateTime。不要用 LocalDateTime.now() 写时间点。
- 不整体替换 DATETIME/TIMESTAMP；CURRENT_TIMESTAMP 使用 UTC 会话；初始化脚本设置会话 UTC。
- API 使用 InApiTimeModule，Redis/OAuth 使用独立 InJavaTimeModule 等存储编码。不要把 HTTP mapper 注入内部安全存储，也不要改变类型元数据和 TTL。

## 前端

- 原始模型保存 API ISO 字符串；`formatDateTime` 按浏览器时区展示，识别失败回退 Asia/Shanghai，可显式指定展示时区。
- 默认展示 yyyy-MM-dd HH:mm:ss，由前端格式化；DatePicker 使用 `parseInstantDate` 和 `toApiInstant` 转换，不直接给本地字面量拼 Z。
- 只在修改时间选择时使用 Date/toISOString，避免把未修改的纳秒 ISO 全部截断为毫秒。
- 不遍历 JSON 字符串隐式转换；日期和 Duration 不调用时间点工具。

## 业务日历与调度

Cron 时区和存储时区分别配置：TSS Spring 的 ingot.tss.spring.time-zone 为 ZoneId，缺省 Asia/Shanghai；XXL-Job Admin 明确使用上海 JVM 时区。无时区 API 时间点不因该默认值而被接受。需要固定业务日历的功能显式声明 ZoneId，不跟请求时区走。

## 新服务接入

引入 ingot-core 及所需数据模块，自动继承 API mapper 和 MVC 转换；Feign 同一契约。数据源使用标准 UTC 配置，业务代码遵守 UTC 语义。自建 ObjectMapper 需保留业务模块并显式装配 InApiTimeModule；自建 data source 单独配 UTC。接入示例见 docs/guides/TIME-CONTRACT.md，完整变更验收记录见关联 time-contract active change。

## 独立业务服务示例

```groovy
dependencies {
    implementation project(ingot.framework_core)
    implementation project(ingot.framework_data_mybatis)
}
```

数据源模板（不用覆盖中间件、自定义连接池）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/${BUSINESS_DATABASE}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true
```

时间点 DTO 和持久化边界：

```java
/** 接口时间点 DTO，无需 IAM 即可继承框架编码。 */
public record BusinessTimeInput(Instant paidAt, Instant validUntil) {}

// HTTP 接收 2026-10-08T09:00:00+08:00，DTO 得到 2026-10-08T01:00:00Z。
LocalDateTime databaseValue = LocalDateTime.ofInstant(paidAt, ZoneOffset.UTC);
Instant deadline = entity.getValidUntil().toInstant(ZoneOffset.UTC);
boolean expired = !Instant.now().isBefore(deadline);
```

前端展示与选择（不修改未编辑的模型）：

```ts
import { formatDateTime, parseInstantDate, toApiInstant } from "@ingot/shared";
formatDateTime(record.paidAt);
formatDateTime(record.paidAt, { timeZone: "Asia/Shanghai" });
const picker = computed({
  get: () => parseInstantDate(record.paidAt),
  set: (value: Date | null) => { record.paidAt = toApiInstant(value); },
});
```

独立自动配置验证：ApiTimeAutoConfigurationTest 不依赖 IAM，覆盖真实 MVC JSON/query/form、长整型、日期/时长和 400；MySQL/JDBC 验证 UtcJdbcContractTest 使用独立连接临时表，不触碰业务表。接入时运行这些契约测试及自身截止场景。

## 发布与回退

前后端、相关服务、Nacos 数据源配置作为同一发布单元，在全新环境同步切换；不保留旧墙钟格式兼容入口，不迁移历史数据库、Redis 或队列数据。回退必须成套恢复对应版本和配置，不能混用新旧时间契约；已有数据环境升级不属于本次兼容承诺。

## 可复用验证

- `ApiTimeAutoConfigurationTest`：没有 IAM 的自动配置、JSON/query/form 与 400。
- `FeignTimeContractTest`：参数编码和独立 WebFlux mapper。
- `UtcJdbcContractTest`：设置 TIME_TEST_MYSQL_URL/USER/PASSWORD 后，使用临时表验证真实 Connector/J、MyBatis-Plus 实体和 OAuth JDBC 密钥期限；测试账号仅需该测试库的临时表权限。
- `RedisAuthorizationTimeContractTest` / `VerificationCodeUtcTest`：设置 TIME_TEST_REDIS_HOST/PORT/PASSWORD 后，用独立随机测试键验证内部编码、刷新/撤销和 TTL，并自动清理。
- 前端 `pnpm test:time-browser`：需要 Playwright Chromium，覆盖三个浏览器时区、日期选择提交与夏令时。

完成记录见 [时间变更验收](../../specs/changes/active/20261008-framework-time-contract/ACCEPTANCE.md)。
