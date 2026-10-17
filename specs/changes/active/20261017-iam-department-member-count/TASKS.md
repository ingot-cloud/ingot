# Tasks

## 实施任务

- [x] T1：`DepartmentRecord` 增加可选 `memberCount`，同步两处构造
  - 依赖：无
  - 验收：详情/策略构造传 null；JavaDoc 说明填充边界
- [x] T2：Mapper/Repository 批量 ACTIVE 计数
  - 依赖：T1
  - 验收：空 ID 不查库；状态用 `MemberStatus.ACTIVE`
- [x] T3：`DepartmentService.list` 返回树、批量填计数、根与一级平级；`get` 不填
  - 依赖：T2
  - 验收：一次计数 SQL；树响应无 page
- [x] T4：更新 `contracts/openapi.json` 与 `schemas.json`
  - 依赖：T1
  - 验收：`memberCount` 可选，不在 required
- [x] T5：Repository / Service 测试
  - 依赖：T3
  - 验收：ACTIVE 计入、暂停不计、详情无字段

## 验证任务

- [x] V1：跑 `IamAuthorizationContractTest` 与新增测试
- [ ] V2：对照 ACCEPTANCE 勾选；浏览器与联调待人工确认

## 完成检查

- [ ] 实现与 DESIGN 一致
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] Current 待验收后再更新
- [ ] Change 已记录完成信息
