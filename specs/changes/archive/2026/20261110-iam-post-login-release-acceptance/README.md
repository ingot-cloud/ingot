# IAM 登录后接口现场与发布验收

> 状态：completed
> 创建日期：2026-11-10
> 完成日期：2026-11-10

承接 IAM 登录后接口提速和场景提示的现场验收。运行库已应用 `037`，通过 10 万条分配的列表、尾页和搜索检查、平台授权缓存分层路径检查，以及真实受限成员的权限与租户隔离验证。两端使用真实 BFF 会话在同一批数据上对照，平台首批成员可见 P95 下降 64.7%，租户下降 68.4%，均超过 20% 的验收门槛。临时数据清理后复查无残留。

- [后端实现档案](../20261109-iam-post-login-read-performance/README.md)
- [前端实现档案](../../../../../../ingot-admin/specs/changes/archive/2026/20261109-iam-post-login-read-performance/README.md)
- [验收要求](REQUIREMENTS.md)
- [验收与回退设计](DESIGN.md)
- [任务](TASKS.md)
- [本机验收记录](VALIDATION.md)

关联实现为后端 `d40d3dff`、前端 `9682655`，对照旧版为后端 `f7feb1fb`、前端 `dd3ec45`。现场对照使用独立回环验收入口切换版本，复用本机数据库、Redis 和真实登录会话。平台授权计数来自验收时的临时探针。

后端 `current` 已同步 [平台读取性能](../../../../current/iam/platform-read-performance/README.md)、[租户用户组](../../../../current/iam/tenant-groups/README.md)、[租户即时授权](../../../../current/iam/tenant-live-authorization/README.md) 与 [数据库持久化](../../../../current/iam/database-persistence/README.md)。前端 `current` 已同步 [平台能力](../../../../../../ingot-admin/specs/current/base/platform-capabilities/README.md)、[组织用户组](../../../../../../ingot-admin/specs/current/org/group-workspace/README.md) 和 [管理台 UI](../../../../../../ingot-admin/specs/current/common/admin-ui-experience/README.md)。
