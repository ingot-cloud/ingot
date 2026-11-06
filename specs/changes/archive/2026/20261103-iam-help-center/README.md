# 已取消：IAM帮助内容方案

> 状态：cancelled

取消日期：2026-11-03。用户明确要求首期前端写死帮助说明，未来可能增加维护后台。本方案被前端静态内容方案替代；本轮移除未提交的IAM实现、Gradle任务和Python构建工具，迁移正文和配图到前端。

以下为取消前的历史记录，仅用于追溯，不表示当前提供IAM帮助接口；未完成的真实截图、BFF与发布验收不转记为通过，不更新current。前端替代规格：[静态帮助中心](../../../../../../ingot-admin/specs/changes/active/20261103-common-help-center/README.md)。

---

# 内置帮助中心与两端内容隔离

> 状态：cancelled

## 元数据

- Change ID：`20261103-iam-help-center`
- 领域：`iam`；跨端公共阅读能力
- 负责人：jy（需求与验收）；研发维护内容
- 创建日期：2026-11-03
- 目标发布：与帮助阅读前端及对应功能版本协调发布，具体窗口实施时安排
- 前端变更：[20261103-common-help-center](../../../../../../ingot-admin/specs/changes/active/20261103-common-help-center/README.md)

## 目标与范围

提供登录后使用的帮助中心，覆盖当前发布版本全部实际可用功能。租户端只读取通用及租户说明，平台端只读取通用及平台说明；目录、搜索、正文、图片、缩略图与关联导航全部执行同一边界。两端沿用各自同源登录，账号同时拥有两域资格也不跨当前上下文读取。

研发通过 Git 维护中文 Markdown、演示截图及元数据，内容随系统版本构建、发布与回滚。前端采用飞书式搜索首页、分类树、文章排版、章节定位和图片预览；本仓负责内容契约、检索、受保护资源和内容质量门禁。

首期不包含在线编辑与发布后台、数据库文档表、评论、阅读统计、AI 问答、多语言、租户自建文档或公共匿名文档站。首页“常见问题”和“快速入门”由研发编排，不伪造热度。

## 输入与状态说明

- 2026-11-03 对话：用户要求规划系统全部功能的使用说明，并强调租户不得查看平台信息。
- 用户确认研发维护、随版本发布；随后补充飞书风格、相同类型交互及必要配图。
- 2026-11-03 用户确认按两份Spec开始实施前后端，并要求完成隔离、交互及配图验证；已获得实施授权。
- 已完成对话中的需求、设计、兼容、测试与验收对齐。两份规格已获实施授权并按 `approved → implementing → validating` 推进。

## 工件

- [需求与验收](./REQUIREMENTS.md)
- [后端设计](./DESIGN.md)
- [接口契约（权威）](./API.md)
- [内容、配图与覆盖规范（权威）](./CONTENT.md)
- [任务与决策](./TASKS.md)
- [实施与验收记录](./ACCEPTANCE.md)

前端 `API.md`、`CONTENT.md` 为本目录同版副本；接口变化先在这里更新，再同步前端副本并重新确认设计变更。

## 关联事实与并行工作

- [双入口同源登录](../../../../current/security/same-origin-login/SPEC.md)：不同 origin、固定应用域、Cookie/BFF 和身份纪元；不另建认证链。
- [平台能力归属](../../../../current/iam/platform-capabilities/SPEC.md)：租户不装配平台安全中心，不能凭账号或 URL 获得跨域资格。
- [租户成员](../../../../current/iam/tenant-members/SPEC.md)、[租户部门](../../../../current/iam/tenant-departments/SPEC.md)、[下载中心](../../../../current/framework/download-center/SPEC.md)：首批内容的重要依据。
- [IAM 主变更](../../../active/20260912-iam-identity-access-management/README.md)、[租户工作台](../../../active/20261030-iam-tenant-workbench/README.md)、[字段策略](../../../active/20261027-iam-resource-field-policy/README.md)、[移除通讯录浏览](../../../active/20261024-iam-remove-directory-browse/README.md)、[授权审计](../../../active/20261024-iam-authorization-audit-readable/README.md)：文档按实际发布状态核实，不将其待验收项宣称为上线事实，不改变这些变更的状态。

## 完成记录

- 完成日期、关联提交或 PR、更新的 current capability：未发生
- 预期 current：`iam/help-center`，仅最终验收后创建
- 与设计差异、取消原因：无

## 当前验证状态

2026-11-03程序实现及定向自动化、双端构建和受控浏览器验证完成，进入validating。用户已明确真实操作截图后补；真实账号Gateway/BFF联调、逐篇实操、必要截图及目标环境发布/回滚仍未验收。正式内容发布门禁保持启用，未更新current、未归档、未创建commit。
