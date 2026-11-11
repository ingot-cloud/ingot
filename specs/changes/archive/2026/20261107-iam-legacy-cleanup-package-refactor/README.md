# IAM 历史代码清理、职责分层与命名重构

> 状态：completed
> 创建日期：2026-11-07
> 负责人：jy

## 批准依据

本对话已完成两轮只读评估及规划。2026-11-07，jy 明确确认补充命名审查后的方案，并要求创建 active change、分阶段实施；历史表、数据及数据库升级脚本本次不动。本 change 据此从评审结论进入 approved，开始修改业务代码前转为 implementing。

## 目标与边界

退出被新 IAM 替代的旧用户、组织、目录和授权实现；收敛 API 与 commons 的模型职责；梳理 provider 分层及保留类型的命名。保持正式 HTTP/RPC 数据契约、权限、事务、缓存失效和持久任务语义。

本次不修改数据库 DDL、初始化 SQL、历史数据、数据库升级脚本，不执行真实库迁移或发布。Git commit 仅按用户明确要求创建。已有前端工作区的无关改动保留。

## 关联变更

- [IAM 主变更 T18](../20260912-iam-identity-access-management/TASKS.md)：本 change 接管其旧实现清理的代码工作，不代替原进程/RPC及产品验收。
- [旧库迁移发布验收](../20261106-iam-tenant-live-authorization-release/README.md)由独立变更跟踪，后因确认按新库首次部署而取消归档。
- [持久化基线](../../../../current/iam/database-persistence/SPEC.md)、[平台能力](../../../../current/iam/platform-capabilities/SPEC.md)、[即时授权](../../../../current/iam/tenant-live-authorization/SPEC.md)、[版本应用](../../../../current/iam/role-version-application/SPEC.md)：作为保持行为的依据。

## 工件

- [需求](REQUIREMENTS.md) · [设计](DESIGN.md) · [任务](TASKS.md)
- [类处置与命名清单](CLASS-INVENTORY.md)
- [阶段与退出条件](phases/README.md)
- [验证记录](VALIDATION.md)

## 完成记录

2026-11-07：P0完成，批准门禁满足后开始P1。基线测试失败单独登记，不将其归因于本次修改。

2026-11-07：P0–P6完成，进入validating；原902类型台账中删除164、迁移/更名656、保留82，新增替代/拆分50类型。API现23、commons IAM现60、provider现703个生产Java文件。前端关联清理150文件。P7仍待目标环境与用户验收，不更新current，不归档；当时未创建commit。

2026-11-07 验收修正：恢复根包 InIamApplication，补充唯一入口、默认组件扫描及bootJar清单验证；详见VALIDATION与startup-root-validation.json。仍为validating。

2026-11-07 用户反馈：重启服务、平台/租户登录及部分接口测试通过，记入P7部分验收证据；未宣告全部功能验收完成，状态保持validating。

2026-11-07 用户明确要求提交代码：历史清理与分层提交 `b1422e27`，验收 SQL 兼容修复提交 `ed90e3c1`。提交不替代 P7 验收，状态保持 validating；数据库与升级脚本保持不变。

2026-11-10 用户确认 P7 补充设计并要求实施。修正租户成员详情和保存成功响应的字段编辑提示。补充设计与任务已确认，进入 approved。

2026-11-10 字段回显修正已实现，进入 validating。

## 验收与归档

- 2026-11-10 完成本机 P7 和 A09 验收。消费者 RPC、受限委派、到期撤权、双节点接管、模拟微信链路及字段权限均按约定覆盖。最终回归与真实 OAuth 复测见 [验证记录](VALIDATION.md#字段回显复验)。
- 关联历史实现提交为 `b1422e27` 和 `ed90e3c1`。P7 补充修正以工作区代码、测试报告和验收制品为依据。
- 已更新 [租户成员](../../../../current/iam/tenant-members/SPEC.md#成员资料详情与保存)、[租户字段策略](../../../../current/iam/tenant-field-policy/SPEC.md#成员资料的编辑提示)和 [IAM 模块职责](../../../../current/iam/database-persistence/SPEC.md#provider-职责边界)基线。
- 相比原设计，恢复模块根启动入口，并在 P7 补充对齐租户成员详情和保存响应的编辑提示。两项成员字段绑定契约断言随私有 DTO 归入 provider。数据库、缓存协议与原写入门禁沿用既有契约。
