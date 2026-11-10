# IAM 登录后接口提速与场景提示

> 状态：completed
> 归档日期：2026-11-10

用户于 2026-11-09 批准实施。

2026-11-10 用户要求归档已完成工作。归档包含两端用户组读取与提示、登录后代表接口验证、字段权限上下文、角色分配批量查询和 10 万条分配分页优化。

- [需求与验收](REQUIREMENTS.md)
- [设计](DESIGN.md)
- [任务](TASKS.md)
- [验收记录](VALIDATION.md)
- [前端配套归档](../../../../../../ingot-admin/specs/changes/archive/2026/20261109-iam-post-login-read-performance/README.md)

## 完成记录

- 实现与隔离回归完成。组摘要沿用原分页与对象范围。字段权限上下文只在请求内复用。租户角色分配保留页码和精确计数，新增覆盖索引和 `037` 在线迁移。通用内部异常使用固定中文提示，已知业务错误保留场景文案。后续设计扩展见 [设计](DESIGN.md)。
- 在 [租户用户组](../../../../current/iam/tenant-groups/README.md)、[平台读取性能](../../../../current/iam/platform-read-performance/README.md)、[租户即时授权](../../../../current/iam/tenant-live-authorization/README.md) 和 [数据持久化](../../../../current/iam/database-persistence/README.md) 索引归档证据。运行规格待实际发布确认后更新。
- 实现提交为后端 `d40d3dff`、前端 `9682655`。
- 真实 BFF 会话、两端页面 P95、缓存命中率和运行环境 `037` 验证已完成，见[现场与发布验收](../20261110-iam-post-login-release-acceptance/README.md)。
