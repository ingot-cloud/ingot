# Tasks

## 实施任务

- [x] T1：创建本 change 工件并对齐决策
  - 依赖：无
  - 验收：README 状态可实施，REQUIREMENTS/DESIGN 写清权限、清理范围、响应
- [x] T2：仓储锁定、覆盖资料与按成员批量清理
  - 依赖：T1
  - 验收：含 REMOVED 的持锁查询；分配/委派/组/负责人/人群/选择器可按成员清理
- [x] T3：实现 `MemberRejoin` 并接入 `TenantMemberOnboarding`
  - 依赖：T2
  - 验收：创建路径命中 REMOVED 复用原行；在职仍报已存在
- [x] T4：新增 rejoin 接口与 `TenantMemberCreated.rejoined`
  - 依赖：T3
  - 验收：`POST /{id}/rejoin` 与创建共用权限和出参
- [x] T5：自动化测试
  - 依赖：T4
  - 验收：Onboarding 与 Rejoin 测试覆盖清理、冲突和回滚

## 验证任务

- [x] V1：运行相关单元测试
- [x] V2：更新 `specs/current/iam/tenant-members` 与 API.md 后归档

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新
- [x] Change 已记录完成信息并归档
