# IAM 应用开通批量查询与操作目录缓存

> 状态：completed

| 项目 | 值 |
|---|---|
| Change ID | `20261109-iam-entitlement-batch-action-catalog-cache` |
| 领域 | IAM |
| 创建日期 | 2026-11-09 |
| 本机目标环境验收与归档 | 2026-11-10 |

## 目标与范围

用户于 2026-11-09 确认首期范围。将普通租户成员的应用开通与人群校验改为批量实时查询，并为平台和租户的 `action-catalog` 接入单层 Redis。租户授权按现行即时授权契约生效。

缓存范围限于两个 `action-catalog` 入口使用的资源与操作目录。角色列表、菜单、用户可访问应用列表、授权视图和角色版本定义沿各自的现行读取契约处理。现有授权分层缓存与本次目录缓存各自独立。

## 工件

- [隔离基线](./BASELINE.md)
- [实施验证与复测](./VALIDATION.md)
- [需求与验收标准](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 关联规格

- [租户即时授权与角色工作区](../../../../current/iam/tenant-live-authorization/SPEC.md)
- [平台管理读取性能](../../../../current/iam/platform-read-performance/SPEC.md)
- [平台能力](../../../../current/iam/platform-capabilities/SPEC.md)
- [IAM 重构](../20260912-iam-identity-access-management/README.md)约束现有授权入口。首期目录缓存只作为展示读取的优化。
- [旧库迁移发布验收](../20261106-iam-tenant-live-authorization-release/README.md)因改用新库首次部署已取消归档。

## 评审记录

2026-11-09 用户确认首期详细设计。应用存在性继续实时检查，缓存命中省去资源与操作两次装载查询。

2026-11-09 首期实现、本地回归及基线复测通过。用户随后确认本次无需兼容旧目录写节点，并要求精简配置。移除 Redis 读取开关，配置 Redis 后自动使用目录缓存。应用存在性和租户授权继续实时检查。

自动启用方案的本地回归通过。

## 完成记录

2026-11-10 普通成员真实 BFF 会话通过本机验收。可用应用从 1 个增至 16 个时，`bootstrap` 和 `capabilities` 的开通与人群批量查询分别保持每请求两条和一条。应用开通与人群的提交后首请求检查通过，临时数据已清理。关联实现提交为 `c57394f4`。[验收记录](./VALIDATION.md#本机目标环境验收)

现行实现保留开通与人群批量查询，已写入 [租户即时授权基线](../../../../current/iam/tenant-live-authorization/SPEC.md#普通成员的应用开通与人群)。[读取性能基线](../../../../current/iam/platform-read-performance/README.md)提供查询预算入口。首期短目录缓存已由实时单 SQL 替换，原设计和隔离实测保留为对照。[目录直查验收](../20261109-iam-versioned-shared-catalog-cache/README.md)
