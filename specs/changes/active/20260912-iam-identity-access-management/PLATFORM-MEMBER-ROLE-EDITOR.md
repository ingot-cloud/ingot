# 平台成员角色配置闭环

> 状态：validating（2026-10-06，用户批准计划后实施；approved → implementing → validating，人工 MR04 待执行）。不修改 current，不创建提交，保留此前修改。

## 需求与批准决策

成员详情只展示当前有效绑定的角色名称与固定版本，不展示 ACTIVE/REVOKED；用户组继承和委派来源只读。创建、编辑使用录入框打开两步对话框：选择角色 → 设置范围。多选角色，明确选择固定版本，不自动使用最新版。确认只更新成员草稿，取消保留原值，最终保存成员时原子提交。

角色范围类型由固定版本决定，展示全部操作并配置需要参数的指定对象，字段权限只读继承。使用统一 InLoading、角色版本树、对象选择器、权限背景 Token，分页默认20，保留跨页草稿，全局待配置清单定位错误。可选新增分配起止时间在第一步配置，默认立即/长期；既有记录有效期保持不变。

资料编辑和直接分配资格独立。没有可写资料字段但具备完整直接分配资格时仍可编辑角色；纯委派管理员不能使用人员页快捷直接配置。未加载、历史、继承和委派记录不得被覆盖。

## 设计与接口

- 新增 GET /v1/platform/members/{id}/bound-roles：按成员关系 SQL 分页当前有效角色，按角色/版本聚合；成员查看及对象边界鉴权，不披露无权用户组名称/ID。
- 成员 assignments 关联接口支持 effectiveStatus=ACTIVE、directOnly=true；后者仅限非委派直接分配，仍按可信身份及分配边界筛选。
- 平台 PATCH 使用 PlatformMemberEditInput，保留现有平铺资料字段，新增可选 roleChanges：additions（MemberRoleAssignmentDraft）、updates（assignmentId/expectedVersion/scopeBindings）、removals（assignmentId/expectedVersion）。租户 MemberProfileInput 不变。
- POST /v1/platform/members/{id}/preview 使用同一平台输入，返回资料及角色增删改摘要；角色变化最终保存先预览、失败保留草稿。提交仍事务内重验，不信任预览。
- 创建继续使用 MemberCreateInput.roleAssignments，但校验所选版本属于该角色且仍发布/可分配，不要求最新版。
- 差量操作，范围更新保持原记录、创建时间、角色版本、有效期与来源；撤销和新增分别审计。共用授权首锁 → 成员行 → 分配版本，角色操作按完整治理资格及成员对象边界校验。最后超级管理员保护、字段权限、范围对象与版本冲突均整批回滚，成功后失效缓存。
- 不新增数据库结构；没有迁移或真实库操作。

## 实施任务与验收

- [x] MR01 后端有效绑定摘要、关联过滤、固定版本创建、成员差量角色预览与事务保存
- [x] MR02 前端两步多角色对话框、创建与编辑本地草稿、固定版本/范围回显和有效只读摘要
- [x] MR03 公共契约、OpenAPI、夹具、前端副本及来源校验；后端单元/隔离 MySQL HTTP、前端组件/类型/lint/边界/两管理台构建
- [ ] MR04 人工：创建多角色并配置对象；编辑自动回显，取消不提交，保存一起生效；跨页未加载记录不丢失；只读有效摘要无状态；继承/委派只读；范围外与受限管理员拒绝；版本冲突整批回滚；旧固定版本保留、非最新版可明确分配；字段不可写仍可按资格编辑角色


## 实施与验证证据

后端按关系 SQL 聚合有效角色和固定版本，分配列表在分页前过滤 ACTIVE/非委派来源。成员 PATCH 为资料与新增/范围更新/撤销的单事务差量，复用原字段权限、分配审计及授权失效；范围更新保留旧版本和有效期。新版本选择明确，不限定最新版。没有修改租户输入或真实数据库。

前端新增 MemberRolesField、重做 MemberRoleAssignDialog，按“选择角色 → 设置范围”配置，关联直接记录正常分页；通过已保存分配的 selected-candidates 回显真实版本及对象，全部操作树和字段摘要只读继承。共享 ScopeStep 增加按记录的独立 key、逐角色只读及已选加载器；同版本多条分配不会合并参数。成员最终保存先预览并确认数量，只发送实际差量。

自动化结果（2026-10-06）：

- AssignmentServiceTest、PlatformMemberContactsTest、MemberCreationAssignmentTest、PlatformMemberFieldContextTest、PlatformMemberUpdateAccessTest 通过，覆盖旧发布版本显式分配、错误角色/外部记录/受限身份拒绝、资料与角色整批提交/回滚，以及仅角色编辑不要求可写资料字段。
- 隔离 MySQL 与真实 HTTP：RoleWorkspaceMySqlHttpTest 14项通过；有效摘要分页无组ID/名称，直接记录筛选在SQL内，范围调整保留原记录，分配版本冲突回滚成员版本。可信身份边界使用固定替身，不替代真实账号验收。测试容器自动清理。
- IamAuthorizationContractTest 16项通过；实际类型生成公共契约与夹具，OpenAPI 132路径/199操作，Python契约7项通过。
- 前端人员模块9文件26项、共享选择器/范围组件5文件26项通过；新行为覆盖确认/取消、迟到响应、同版本独立范围、有效只读摘要、角色单独编辑与预览失败不提交。类型检查、所改文件只读lint、依赖边界、公共包和两管理台构建通过。
- 修复回归中发现的空账号关系投影和只读属性布尔值；初次测试替身/空账号夹具及共享断言失败已修正重跑。构建保留现有包体/图标扫描与Gradle弃用提示。

人工步骤见 RESOURCE-EXTENSION-VERIFICATION.md 的 MR04 节，全部未勾选。保留此前未提交改动；本轮不更新 current，不提交，不新增DDL。
