# IAM 功能目录与授权补齐

> 状态：completed
> 创建日期：2026-11-06
> 负责人：jy

用户于 2026-11-06 确认 [完整方案](../../../../../docs/modules/iam/RESOURCE-CATALOG-PROPOSAL.md)，授权实施并在新建隔离库重建测试角色。先补齐上传、平台用户组、组织工作台和权限诊断，再统一目录、菜单、编码及显示名称。

与 [IAM 主变更](../../../active/20260912-iam-identity-access-management/README.md) 共用接口生成工具。授权审计展示、App 用户管理实际环境发布、租户即时授权实际库发布各自保留验收范围。

- [需求](REQUIREMENTS.md)
- [设计及目标目录](DESIGN.md)
- [任务及验证](TASKS.md)

## 完成记录

- 2026-11-07 用户确认双端管理台及小程序联调验收通过。
- 实现提交为后端 `d5c13cf1`、前端 `2464086`、小程序 `2f1a221`。[前端配套归档](../../../../../../ingot-admin/specs/changes/archive/2026/20261106-base-resource-catalog/README.md)
- 系统基线更新见[平台能力](../../../../current/iam/platform-capabilities/README.md)、[组织工作台](../../../../current/iam/tenant-workbench/README.md)、[组织用户组](../../../../current/iam/tenant-groups/README.md)及[持久化](../../../../current/iam/database-persistence/README.md)。
- 最终实现按确认后的设计交付，补充了空库预置单一平台账号和首次改密要求。本机测试库重建及测试账号恢复有独立记录，见 [TASKS](TASKS.md)。
