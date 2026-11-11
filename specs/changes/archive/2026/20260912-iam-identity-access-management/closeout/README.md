# 结项候选包

状态：completed。2026-11-11 用户确认 H01–H04 全部通过，候选已应用、current 与引用已更新、两仓主变更已归档。本目录保留审阅时的候选原文和前置指纹；最终事实以 current 为准。

## 可直接审阅的候选

| 目标 | 完整候选 |
|---|---|
| `specs/current/iam/identity-access-management/README.md` | [新增](./candidates/specs/current/iam/identity-access-management/README.md) |
| `specs/current/iam/identity-access-management/SPEC.md` | [新增](./candidates/specs/current/iam/identity-access-management/SPEC.md) |
| `specs/current/framework/authorization-execution/README.md` | [新增](./candidates/specs/current/framework/authorization-execution/README.md) |
| `specs/current/framework/authorization-execution/SPEC.md` | [新增](./candidates/specs/current/framework/authorization-execution/SPEC.md) |
| `specs/current/pms/application-authorization/README.md` | [替换/更新](./candidates/specs/current/pms/application-authorization/README.md) |
| `specs/current/pms/application-authorization/SPEC.md` | [替换/更新](./candidates/specs/current/pms/application-authorization/SPEC.md) |
| `specs/current/pms/data-authorization/README.md` | [替换/更新](./candidates/specs/current/pms/data-authorization/README.md) |
| `specs/current/pms/data-authorization/SPEC.md` | [替换/更新](./candidates/specs/current/pms/data-authorization/SPEC.md) |
| `specs/current/iam/platform-capabilities/SPEC.md` | [替换/更新](./candidates/specs/current/iam/platform-capabilities/SPEC.md) |
| `specs/current/iam/role-version-application/SPEC.md` | [替换/更新](./candidates/specs/current/iam/role-version-application/SPEC.md) |
| `specs/current/iam/database-persistence/SPEC.md` | [替换/更新](./candidates/specs/current/iam/database-persistence/SPEC.md) |
| `specs/current/iam/tenant-members/SPEC.md` | [替换/更新](./candidates/specs/current/iam/tenant-members/SPEC.md) |
| `specs/current/iam/tenant-departments/SPEC.md` | [替换/更新](./candidates/specs/current/iam/tenant-departments/SPEC.md) |
| `specs/current/README.md` | [替换/更新](./candidates/specs/current/README.md) |
| `specs/current/security/session-safety/README.md` | [替换/更新](./candidates/specs/current/security/session-safety/README.md) |
| `specs/current/security/session-safety/SPEC.md` | [替换/更新](./candidates/specs/current/security/session-safety/SPEC.md) |
| `specs/current/framework/layered-cache/SPEC.md` | [替换/更新](./candidates/specs/current/framework/layered-cache/SPEC.md) |
| `specs/changes/active/20260912-mybatis-data-scope-predicate-scale/README.md` | [替换/更新](./candidates/specs/changes/active/20260912-mybatis-data-scope-predicate-scale/README.md) |

## 应用与归档顺序

1. 在主变更记录用户 H01–H04 的实际结论，未通过继续 validating。
2. 核对 [baseline-plan.json](./baseline-plan.json) 的 beforeSha256；当前文件若已变化，先重对账，禁止覆盖后续变更。
3. 将 candidates 中的完整文件写入各自 target。候选按最终目标目录解析链接，不能按本目录直接执行相对跳转。新 current 来自当前事实，旧 PMS/RBAC 路径只保留替代入口，历史归档不删。
4. 同步完成记录：日期、实际 Git HEAD 加未提交范围、候选 capability、原设计差异。没有提交请求不 commit，不虚构关联提交。
5. 先更新 current，再标 completed，最后将两个主变更移至各仓 specs/changes/archive/2026/。移动后批量校正引用、最新 SOURCES 源路径/指纹和主索引；同时检查其他 active/current 对主变更的引用。
6. 再跑链接、契约、来源与 git diff --check；保留本包及实际应用结果。不把 archive 移动本身当成验收通过。

上述步骤已在用户确认后由 Agent 完成。原候选字节不再修改；归档引用修正后的最终 SHA 记录在 baseline-plan.json 的 appliedSha256。

## 实现与差异

- 后端基底 2825431954e1cdf85c0142dbb27e0e9289fe29af，前端基底 34dbc25fa8b61ba4cc544ab72c9559136524b20c；本轮有未提交修改，基底不是最终制品身份。[准备时实现清单](./implementation-manifest.json)保留验收前快照；[归档后实现清单](./archive-implementation-manifest.json)记录最终工作区文件与 SHA-256，未提交范围均保留。
- 租户当前权限/动态拥有者、同源双入口、新库初始化为后续确认方案；平台固定版本继续保留。
- 旧通讯录、旧租户差异/版本、平台独立字段策略、旧 PMS 业务及四站登录退出；必要框架消费者保留。
- 缓存/社会化、异步导出和工作簿死锁、草稿403/503、列偏好/微秒状态、导航恢复、独立对象 SDK/RPC 缺陷均按当前规范修复；细节保留主变更错误分析。
- 中间件/供应商仅按本项目实现及接入验收；自然流量与目标发布独立，不承诺生产 SLA。


## 最终复核

[归档验证](./archive-validation.json)记录实际执行命令及结果。后端 closeout/verify.py 可只读复核两仓 completed、用户确认、应用文件、候选原文、来源、引用与最终工作区指纹。最终清单自身及验证结果文件不参与自身 SHA。目标环境发布和自然流量不因归档被冒称完成。
