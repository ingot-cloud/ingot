# 统一时间契约与 UTC 处理

## 时间类型与接口

时间点表示绝对瞬间。JSON、query、form 接收带 `Z` 或偏移量的 ISO-8601，响应统一 UTC `Z`，保留字段可表达的精度。无时区、旧 `yyyy-MM-dd HH:mm:ss`、非法日期、空字符串或数字形式的普通 API 时间点返回 400，不依据请求或 JVM 时区推测输入。

新 DTO 时间点优先使用 `Instant`；已有 `LocalDateTime` 时间点明确表示 UTC，框架边界以 UTC 转换。`Date`、`OffsetDateTime` 和 `ZonedDateTime` 作为时间点也统一输出 UTC。

`LocalDate` 使用 `yyyy-MM-dd`，`LocalTime` 使用 `HH:mm:ss`，不转换为瞬间。Duration 保留其契约。JWT NumericDate、OAuth `expires_in`、TTL 及既有内部 epoch seconds/millis 继续使用协议规定的数字，不因 HTTP 契约改成 ISO 字符串。

## 公共装配与存储编码隔离

commons 提供 `InApiTimeModule` 与严格解析及格式转换；core 自动装配 API mapper 和 MVC 参数转换，Feign 使用相同格式。基础长整型、OSS、mixin 与业务扩展模块继续生效。独立业务服务引入 core 及所需数据模块即可复用，不依赖 IAM。

HTTP 与内部存储编码分开。Redis/OAuth 使用独立的 `InJavaTimeModule` 等存储配置，保留安全类型元数据、键、索引及 TTL；不能将 HTTP mapper 注入 Token/Session 存储。自建 mapper 需明确保留业务模块并装配 API 时间模块，自建数据源单独配置 UTC。

BFF 事务公开视图的 `expiresAt` 为 `Instant`，响应 ISO，OpenAPI 为 `string/date-time`；内部存储仍为 epoch seconds。其他视图字段及空值省略规则沿用各自契约。

## 后端计算与数据库

到期、锁定、授权截止及时间差使用 `Instant` 或 UTC 时钟。时间区间默认起点包含、终点不包含，截止瞬间判定过期；各业务永久、无限期限及 null 语义保持不变。

持久化 `LocalDateTime` 时间点时，使用 `DateUtil.utc()` 或 `LocalDateTime.ofInstant(value, ZoneOffset.UTC)`；读取用 `toInstant(ZoneOffset.UTC)`。不使用本地 `LocalDateTime.now()` 写入时间点，不强制改变全局 JVM 时区。

各业务服务 JDBC URL 配置 `connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`，由驱动设置连接及 MySQL 会话时区。每个连接池和自建数据源独立遵守，UTC 连接不能修正业务代码已经写错的本地值。

DATETIME/TIMESTAMP 不整体替换；SQL `CURRENT_TIMESTAMP` 使用 UTC 会话，业务初始化脚本设置会话 UTC。公共 `in-database.yml` 不重复配置上述连接属性或会话时区初始化 SQL，仍保留字符集、SQL 模式与连接池监控配置。

账号失败窗口、锁定、密码到期、初始密码及用户时间元数据按 UTC 处理。锁定信号的秒 TTL 向上取整，是否有效按 UTC 截止值判断，永久信号和原键及编码保留。事件生成、上报、存储、查询、保留期、网关名单有效期及缓存时间记录使用 UTC；验证码到期值为 `Instant`，原寿命不变。

Token/Session 的签名、期限、刷新、撤销、并发规则、键、索引及 TTL 不因时间契约变化而改变。数字安全协议时间与 API 时间点的编码分别验证。

## 业务日历与调度

Cron 业务时区与数据库 UTC 时区分别配置。TSS Spring 的 `ingot.tss.spring.time-zone` 为 `ZoneId`，默认 `Asia/Shanghai`；非法时区阻止启动。任务注册和 Cron 更新均显式使用该时区，不依赖 JVM 默认时区。

XXL-JOB Admin 的部署显式使用上海时区及 JVM 时区。当前不提供单任务时区，任务执行记录仍使用毫秒时间戳。需要业务日历的逻辑明确指定 `ZoneId`，不随请求展示时区变化；上海 Cron 默认值不使无时区 API 输入变为合法。

## 前端交互边界

模型保留 API 原始 ISO 字符串。shared 的 `formatDateTime` 默认按浏览器时区展示 `yyyy-MM-dd HH:mm:ss`，识别不到时区时回退 `Asia/Shanghai`，也支持显式展示时区。

DatePicker 使用 `parseInstantDate` 与 `toApiInstant` 转换。仅在用户修改选择值时使用 `Date`/`toISOString`，避免将未编辑的纳秒模型截断到毫秒；不直接给本地字面量拼 `Z`。不遍历 JSON 字符串隐式转换，日期与 Duration 不调用时间点工具。

## 兼容、发布与回退

旧无时区及墙钟 API 格式不兼容，BFF `expiresAt` 从数字变为 ISO 字符串。前后端、相关服务及 Nacos 数据源配置在全新环境作为同一发布单元切换，不混用新旧契约。

本能力不迁移历史数据库、Redis 或队列数据，不能将全新环境验证结果作为已有数据环境升级承诺。回退成套恢复对应版本和配置，不单独回退 API mapper、服务或前端。

独立接入可复用 API/MVC/Feign、UTC JDBC、安全截止、Redis 授权及验证码、TSS 时区与浏览器矩阵测试；真实 MySQL/Redis 验证使用隔离库、临时表或独立测试键。具体命令和环境约束见接入指南。
