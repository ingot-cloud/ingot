# 统一时间契约

- 时间点请求为带 Z 或偏移量的 ISO-8601，响应为 UTC Z，保留字段精度；JSON、query、form 相同，无时区/旧格式/非法值返回 400。
- 后端判断和存储为 UTC。新 DTO 优先 Instant；已有 LocalDateTime 时间点明确为 UTC，不整体替换数据库类型。
- LocalDate、LocalTime、Duration 保留语义；Cron 明确业务时区。JWT NumericDate、expires_in、TTL、内部秒/毫秒时间戳保留协议。
- 前端显示浏览器当地时间，识别失败回退 Asia/Shanghai，默认 yyyy-MM-dd HH:mm:ss；工具支持显式展示时区，不增加设置页面。模型保留 ISO，选择结果提交 ISO，不隐式扫描 JSON 字符串。
- 全新环境同步切换，不兼容旧 API 格式，不迁移历史数据库、Redis、队列数据。回退配套前后端版本和配置。

## 公共层

在 commons 新增 API 专用时间模块，由 core 自动配置与 MVC 参数转换装配，Feign 消费相同契约。保留长整数、OSS、mixin 及扩展模块。Redis/OAuth 保留独立存储模块与安全类型信息，不因修改 API 格式而整体改变 Token/Session 编码；业务缓存字段改变逐项验证。

业务数据源 IAM/Auth/Member/Security 各环境在服务 JDBC URL 中统一配置 connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true，由驱动设置连接和 MySQL 会话时区。公共 in-database.yml 不重复配置这两个连接属性，也不重复执行 SET SESSION time_zone；保留 Druid 监控属性、字符集和 SQL 模式初始化。自建数据源逐个配置，不全局覆盖任意数据源，不改中间件自身契约，不强制全局 JVM UTC。

提供无 IAM 依赖的自动配置测试及完整业务接入文档，说明 Instant DTO、UTC LocalDateTime 持久化、截止判断、前端展示、自定义 mapper/data source 接入规则。

## 业务改造

审计所有时间点接口与持久化、SQL 默认时间和比较。账号锁定、TTL、密码过期、初始密码、失败窗口、用户元数据统一 UTC，截止瞬间过期，永久/null 语义保留。锁定信号秒 TTL 向上取整，查询按信号 UTC 截止值判断（截止时刻即失效），永久值及 key/编码保留。事件生成/上报/存储/查询/保留期统一 UTC；网关名单有效期、缓存降级记录统一 UTC，不改缓存机制。验证码 expireTime 改 Instant，寿命不变。

Token/Session 保留签名、期限、刷新、撤销、并发、key、索引、TTL。BFF 事务响应 expiresAt 从 epoch seconds 改 ISO，使用公开视图的 Instant 字段声明 OpenAPI string/date-time；内部时间戳保持原类型，其他视图字段及省略空值规则保持既有行为。

TSS Spring 配置 ingot.tss.spring.time-zone 类型 ZoneId 默认 Asia/Shanghai，非法值启动失败；注册和 Cron 更新均显式使用。XXL Admin 部署明确 TZ/JVM Asia/Shanghai，不新增单任务时区，执行记录仍毫秒。

## 验收与完成

验证 UTC/上海/纽约 JVM 和浏览器，同一时刻偏移等价，JSON/query/form 拒绝旧格式，日期和时长不变；真实 MySQL DATETIME/TIMESTAMP/default/JDBC 密钥时间；过期前/瞬间/后/null，完整登录/授权码/Access/Refresh/退出/撤销/踢出/Redis/BFF；TSS Cron 注册与更新及 XXL Admin；前后端测试/类型检查/构建。

同步双方 AGENTS、时间 skill、开发文档、OpenAPI string/date-time、IAM active 约定。验收全部完成后才更新 current/framework/time-contract 等基线并归档。若外部运行环境不可用，记录证据和待验收项，保持 validating，不宣称已完成实际联调。
