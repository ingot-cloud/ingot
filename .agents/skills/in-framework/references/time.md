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

引入 ingot-core 及所需数据模块，自动继承 API mapper 和 MVC 转换；Feign 同一契约。数据源使用标准 UTC 配置，业务代码遵守 UTC 语义。自建 ObjectMapper 需保留业务模块并显式装配 InApiTimeModule；自建 data source 单独配 UTC。接入示例见 docs/guides/TIME-CONTRACT.md，当前基线见 specs/current/framework/time-contract/，完整验收记录见接入指南引用的时间契约归档。
