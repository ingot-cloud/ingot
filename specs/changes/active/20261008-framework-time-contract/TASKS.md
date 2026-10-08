# Tasks

## 决策

所有实现决策按已批准 DESIGN；前后端同步切换，全新环境，无历史迁移，无单任务时区。

2026-10-08 用户确认简化公共数据库配置：DEV/TEST/PROD 的 in-database.yml 移除重复的时区连接属性和会话时区初始化 SQL，各业务服务 JDBC URL 保留 UTC 参数。

## 实施和验收

- [x] T1：公共 API 编码、MVC/Feign 和独立服务自动配置；验收见 DESIGN。
- [x] T2：UTC 数据源、初始化 SQL、连接池与示例；验收见 DESIGN。
- [x] T3：UTC 业务写入、安全截止与元数据转换；验收见 DESIGN。
- [x] T4：事件链路、网关名单、验证码与缓存时间；验收见 DESIGN。
- [x] T5：BFF ISO 事务接口与 Token/Session 存储隔离；验收见 DESIGN。
- [x] T6：TSS 显式 Cron 时区及 XXL Admin 配置；验收见 DESIGN。
- [x] T7：前端公共时间工具、模型、输入和全部时间展示；验收见 DESIGN。
- [x] T8：规范、接入文档、AGENTS/skill 与契约副本；验收见 DESIGN。
- [ ] T9：时间相关自动化矩阵、真实 MySQL/Redis、构建已通过；全量回归仍有两项既有失败，见 ACCEPTANCE.md。
- [ ] T10：浏览器矩阵和 XXL Admin 实际 Cron 预览已通过；全新环境的跨服务登录/刷新/退出/撤销/踢出仍待验收。

- [ ] Current 已在完整验收后更新，Change 已归档

详细证据、命令及待验收项见 [验收记录](./ACCEPTANCE.md)。
