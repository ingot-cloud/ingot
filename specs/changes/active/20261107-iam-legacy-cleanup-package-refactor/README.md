# IAM 历史代码清理、职责分层与命名重构

> 状态：validating
> 创建日期：2026-11-07
> 负责人：jy

## 批准依据

本对话已完成两轮只读评估及规划。2026-11-07，jy 明确确认补充命名审查后的方案，并要求创建 active change、分阶段实施；历史表、数据及数据库升级脚本本次不动。本 change 据此从评审结论进入 approved，开始修改业务代码前转为 implementing。

## 目标与边界

退出被新 IAM 替代的旧用户、组织、目录和授权实现；收敛 API 与 commons 的模型职责；梳理 provider 分层及保留类型的命名。保持正式 HTTP/RPC 数据契约、权限、事务、缓存失效和持久任务语义。

本次不修改数据库 DDL、初始化 SQL、历史数据、数据库升级脚本，不执行真实库迁移或发布。Git commit 仅按用户明确要求创建。已有前端工作区的无关改动保留。

## 关联变更

- [IAM 主变更 T18](../20260912-iam-identity-access-management/TASKS.md)：本 change 接管其旧实现清理的代码工作，不代替原进程/RPC及产品验收。
- [实际库发布验收](../20261106-iam-tenant-live-authorization-release/README.md)：保持独立，本次不修改其状态或脚本。
- [持久化基线](../../../current/iam/database-persistence/SPEC.md)、[平台能力](../../../current/iam/platform-capabilities/SPEC.md)、[即时授权](../../../current/iam/tenant-live-authorization/SPEC.md)、[版本应用](../../../current/iam/role-version-application/SPEC.md)：作为保持行为的依据。

## 工件

- [需求](REQUIREMENTS.md) · [设计](DESIGN.md) · [任务](TASKS.md)
- [类处置与命名清单](CLASS-INVENTORY.md)
- [阶段与退出条件](phases/README.md)
- [验证记录](VALIDATION.md)

## 完成记录

尚未完成；不提前更新 current 或归档。开发、自动化、真实服务和用户验收分别登记。

2026-11-07：P0完成，批准门禁满足后开始P1。基线测试失败单独登记，不将其归因于本次修改。

2026-11-07：P0–P6完成，进入validating；原902类型台账中删除164、迁移/更名656、保留82，新增替代/拆分50类型。API现23、commons IAM现60、provider现703个生产Java文件。前端关联清理150文件。P7仍待目标环境与用户验收，不更新current，不归档；当时未创建commit。

2026-11-07 验收修正：恢复根包 InIamApplication，补充唯一入口、默认组件扫描及bootJar清单验证；详见VALIDATION与startup-root-validation.json。仍为validating。

2026-11-07 用户反馈：重启服务、平台/租户登录及部分接口测试通过，记入P7部分验收证据；未宣告全部功能验收完成，状态保持validating。

2026-11-07 用户明确要求提交代码：历史清理与分层提交 `b1422e27`，验收 SQL 兼容修复提交 `ed90e3c1`。提交不替代 P7 验收，状态保持 validating；数据库与升级脚本保持不变。
