# 统一时间契约

- 时间点请求为带 Z 或偏移量的 ISO-8601，响应为 UTC Z，保留字段精度；JSON、query、form 相同，无时区/旧格式/非法值返回 400。
- 后端判断和存储为 UTC。新 DTO 优先 Instant；已有 LocalDateTime 时间点明确为 UTC，不整体替换数据库类型。
- LocalDate、LocalTime、Duration 保留语义；Cron 明确业务时区。JWT NumericDate、expires_in、TTL、内部秒/毫秒时间戳保留协议。
- 前端显示浏览器当地时间，识别失败回退 Asia/Shanghai，默认 yyyy-MM-dd HH:mm:ss；工具支持显式展示时区，不增加设置页面。模型保留 ISO，选择结果提交 ISO，不隐式扫描 JSON 字符串。
- 全新环境同步切换，不兼容旧 API 格式，不迁移历史数据库、Redis、队列数据。回退配套前后端版本和配置。

## 验收标准


验证 UTC/上海/纽约 JVM 和浏览器，同一时刻偏移等价，JSON/query/form 拒绝旧格式，日期和时长不变；真实 MySQL DATETIME/TIMESTAMP/default/JDBC 密钥时间；过期前/瞬间/后/null，完整登录/授权码/Access/Refresh/退出/撤销/踢出/Redis/BFF；TSS Cron 注册与更新及 XXL Admin；前后端测试/类型检查/构建。

同步双方 AGENTS、时间 skill、开发文档、OpenAPI string/date-time、IAM active 约定。验收全部完成后才更新 current/framework/time-contract 等基线并归档。若外部运行环境不可用，记录证据和待验收项，保持 validating，不宣称已完成实际联调。
