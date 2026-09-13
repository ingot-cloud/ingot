# 实施核对记录

## 2026-09-13：实施启动与 T01

用户已明确批准开始后端实施。工作区启动时干净；已读取 SDD 工作流、全部 change 工件、应用授权与数据授权 current 基线。未修改 current、历史归档及相邻前端仓库，未连接数据库或执行迁移。

已完成的独立工作：

- [旧入口清单](./LEGACY-ENDPOINTS.md)：38 个控制器、163 个 HTTP 方法映射；包含 PMS 和 Security 的内部 RPC。注解盘点不代替真实执行器审计。
- [源表清单](./SOURCE-TABLES.md)：仓库 SQL 的 28 张表均列明逻辑去向；不包含任何 INSERT 数据，不能充当真实源库预检。
- `ingot-commons` 的 `com.ingot.framework.commons.model.iam`：ScopeKind、ScopeBindingKind、RoleDeltaOperation；ScopeExpression、ScopeBinding、ActionGrant、RoleDelta。ID 使用 String，类型带 OpenAPI Schema 描述，嵌套输入使用 Bean Validation，集合防御性复制。
- DTO 仅定义传输结构与基础输入校验，尚不校验资源归属、范围能力、差异与基础版本关系；不得直接作为已验证授权使用。
- 验证命令：`./gradlew :ingot-framework:ingot-commons:test --tests '*IamScopeContractTest' --console=plain`；6 项测试通过，覆盖大 ID 字符串、未知枚举、缺失/空集合、嵌套非法值、REMOVE 可选范围和集合不可变性。

下一步：D01 已确认，继续补齐身份与主体契约、端点 ACTION/执行器映射及 DDL，完成 T01 再进入 T02/T03/T04。未将这批基础类型计作业务授权引擎或阶段验收完成。

## D01：平台域授权主体（2026-09-13 已确认）

DESIGN 第 2 节只定义租户成员、租户组；API 的 SubjectRef 只有 MEMBER/GROUP，DelegationInput 使用 administratorMemberId；但 API 第 4 节同时提供平台授权与委派。现有契约没有定义平台成员、平台组及其与 Account 的关系，不能把默认租户 ID 或 ROLE_ADMIN 隐式当成平台身份。

用户确认采用以下模型，已同步写入 REQUIREMENTS/DESIGN/API/MIGRATION/ACCEPTANCE/FRONTEND：

- Account 继续作为全局认证主体；新增 PlatformMember 作为平台域有效身份，与 TenantMember 分离。每个账号最多一个平台成员身份，平台成员暂停不删除 Account，也不暂停租户成员。
- 平台授权上下文携带 PLATFORM、accountId、平台 memberId，tenantId 为空；租户上下文携带 TENANT、tenantId、accountId、租户 memberId。身份切换必须经认证流程，不能靠传入 ID 切换。
- MEMBER/GROUP 引用必须与当前 domain 一致；平台组仅包含平台成员，不引用租户成员或租户部门，不新增平台部门模型。租户组保留成员/部门及 includeDescendants。
- 平台委派的 administratorMemberId 和 recipientSelection 使用平台成员；平台角色不接受租户部门参数。所有平台权限只作用于平台资源，全局账号安全操作也不开放租户组织资料。
- 迁移需显式平台成员映射。不能按旧角色名或默认租户中的所有账号自动授予平台成员资格或治理角色；无法证明的旧治理权进入阻塞清单。

不采用平台直接使用 ACCOUNT 主体的备选方案。确认同时明确：一个 Account 可以同时拥有平台和多个租户成员身份，隔离成员资格与授权，不重复注册账号。

## 已确认规则对应的旧入口处置

以下处置来自现有 REQUIREMENTS/DESIGN，不是新增业务决策；实现时仍须逐入口验证。

| 旧入口 | 新约束及处置 |
|---|---|
| SystemDeptAPI.tree / SystemRoleAPI 的 orgId 参数 | 禁止平台代入任意租户读取部门/角色；租户内能力归入可信租户上下文接口 |
| SystemUserAPI.orgInfo / userOrgEdit / userOrgLeave | 全局账号管理与组织资料分离，跨组织业务入口不能原样保留 |
| OrgUserAPI.initPassword | 租户成员管理不能重置全局账号凭证；凭证操作走账号安全职责和独立操作校验 |
| SystemUserAPI.searchByPhone | 不能原样复用全局模糊检索到成员候选；候选必须限制 purpose、对象范围及字段原值检索 |
| AuthUserAPI.info / menus / permissions | 收敛为同一有效视图的 bootstrap/capabilities；不信任旧会话业务权限 |
| DevAuthorizationAuditAPI.audit | RequiredAdmin 不能替代新平台审计 ACTION；诊断与源数据审计需区分，不泄露租户业务资料 |
| Inner*API | INNER 标记不是目标域 bypass；服务身份与目标上下文分别验证 |
| TestAPI.limit | 测试入口须单独归类，不能计入业务 ACTION 接入覆盖证明 |

## 当前门禁

T01 未完成；完整 DTO/端点 OpenAPI 尚未验收，目标结构测试通过不代表运行时业务已接入。T02–T17 未实施。D01 已确认，不再因平台主体模型暂停实施；按 TASKS 顺序推进契约、持久化及迁移。

## D01 确认后的实施

- 新增 AuthorizationDomain、SubjectType、MemberStatus，以及 AuthorizationContext、SubjectRef、DepartmentSelection、Selection。上下文构造检查域、账号、成员、租户 ID 的结构，Selection 可校验平台不携带部门；两者都不代替认证和数据库归属校验。
- 新增 `databases/iam/001_identity.sql` 的 11 张独立目标表，采用两类成员、两类组和复合归属外键。未修改运行时服务、旧库初始化或认证流程。
- Java 验证：`./gradlew :ingot-framework:ingot-commons:test --tests '*Iam*ContractTest' --console=plain`，范围 6 项 + 身份 7 项通过。
- MySQL 验证：`python3 databases/iam/test_identity_schema.py`，8 项约束测试通过。测试独立启动本地 MySQL 8.4 镜像、关闭网络、不映射端口，测试后停止并自动清理容器，不使用现有数据库。
- 数据库测试证明约束和事务回滚，不证明已实现真实登录、组委派影响分析或组织初始化业务。A01a/A01b 运行时验收仍未完成。

本批结束时剩余 T01：资源 ACTION/执行器逐接口映射、其他领域完整公共 DTO/OpenAPI、目录/角色/授权/策略/审计/迁移目标结构及规则。D01 已闭合，当前没有待用户确认的平台主体决策。后续进展见下节。

## 继续实施：目录、版本、委派与策略目标结构

- `endpoint-mapping.json` 对原 163 个入口逐项记录目标管理域、资源、操作、路由和执行约束；跨租户资料入口、租户重置全局密码入口等明确移除。内部 RPC 和当前身份入口单独标明身份边界，不自动转换成业务授权。
- 目标 SQL 共 55 张表：身份 11、目录与角色 17、委派与分配 6、策略/审计/迁移 12、保留辅助 9。辅助结构只提取 CREATE TABLE，不复制 INSERT、DROP 或 USE。
- 公共契约补齐角色版本/参数/元数据覆盖、分配、批量分配、委派上限、字段规则及能力结果；新增稳定 IamReasonCode。PLATFORM_CUSTOM 补齐已批准的平台自定义角色能力的类型表达，未新增跨域能力。
- Bean Validation 增加范围与参数组合、差异操作携带范围、重复角色定义、时间区间、正数委派时长及非 FULL 不可编辑校验。归属、资源能力、完整授权求值仍须后续服务实现。
- 发现并修正 Duration 的 JSON/schema 不一致：JSON 与 schema 均为 ISO-8601 字符串，测试显式校验。已导出 `contracts/schemas.json` 的 21 个组件与 5 个示例，schema 内部引用完整。
- 最终验证：22 项 Java 契约测试全部通过；55 张表在 MySQL 8.4 隔离容器成功建表，18 项约束测试全部通过。容器自动清理，无实际源库或现有业务库读写。`git diff --check` 通过。

当前剩余 T01：bootstrap、完整资源详情、预览/升级/诊断/审计响应与全部端点 OpenAPI；既有映射的执行器尚待后续任务接入。当前没有新增审批问题，未将 T01、运行时授权或实际迁移标记完成。
