# IAM 应用开通批量查询与操作目录缓存

> 状态：validating

| 项目 | 值 |
|---|---|
| Change ID | `20261109-iam-entitlement-batch-action-catalog-cache` |
| 领域 | IAM |
| 创建日期 | 2026-11-09 |
| 目标发布日期 | 待定 |

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

- [租户即时授权与角色工作区](../../../current/iam/tenant-live-authorization/SPEC.md)
- [平台管理读取性能](../../../current/iam/platform-read-performance/SPEC.md)
- [平台能力](../../../current/iam/platform-capabilities/SPEC.md)
- [IAM 重构](../20260912-iam-identity-access-management/README.md)约束现有授权入口。目录缓存只作为展示读取的优化。
- [实际库发布验收](../20261106-iam-tenant-live-authorization-release/README.md)继续独立跟踪实际库切换。

## 评审记录

2026-11-09 用户确认首期详细设计。应用存在性继续实时检查，缓存命中省去资源与操作两次装载查询。

2026-11-09 首期实现、本地回归及基线复测通过。用户随后确认本次无需兼容旧目录写节点，并要求精简配置。移除 Redis 读取开关，配置 Redis 后自动使用目录缓存。应用存在性和租户授权继续实时检查。

自动启用方案的本地回归通过，等待目标环境发布验收后更新 current 并归档。

后续已批准的[共享目录对照变更](../20261109-iam-versioned-shared-catalog-cache/README.md)以实时单 SQL 替换本次短期目录缓存，并保留该版本的本地实测作为对照。首期仍有效的开通与人群批量查询待目标环境验收。
