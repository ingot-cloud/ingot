# 平台能力补齐与页面归属整理

- 状态：completed
- 日期：2026-10-12
- 批准依据：本对话中用户明确 “PLEASE IMPLEMENT THIS PLAN”。
- 关联：/Users/jy/Repositories/Github/ingot-cloud/ingot-admin/specs/changes/archive/2026/20261012-iam-platform-capability-completion/README.md
- 主 IAM 变更：[既有 IAM 重构](../../../active/20260912-iam-identity-access-management/README.md)
- 工件：[需求](REQUIREMENTS.md)、[设计](DESIGN.md)、[任务](TASKS.md)、[接口矩阵](API.md)、[验证](VALIDATION.md)

## 完成与归档（2026-10-14）

- 验收依据：用户明确“20261012-iam-platform-capability-completion spec也已经验收完成，归档并提交”。
- 完成与归档日期：2026-10-14；此前 validating、待部署及待人工验收描述为历史阶段记录，以本次确认结项。
- 当前基线：[iam/platform-capabilities](../../../../current/iam/platform-capabilities/README.md)，同步修正 [会话管理面权限](../../../../current/security/session-safety/SPEC.md)。
- 关联实现提交：`3e952ba9`（平台能力及安全中心）、`89119770`（内部授权身份恢复）；原验证依据 `cd027676`。本次归档提交为 `docs(iam): 固化已验收的平台能力并归档规格`。
- 关联前端：已于 2026-10-13 在 ingot-admin 归档，提交 `f5b0871`；本次不重复归档或修改前端仓库。
- 最终实现与设计差异：无；保留完整 API、升级及验证工件。内部授权缺陷修复已同步设计与既有测试记录。
- 本次只整理基线与归档，不重新执行业务验收、目标库升级或部署；以用户验收确认及既有自动化记录为依据。

主 IAM 重构保持 implementing，其余 active change 的状态与验收不受本次确认影响。
