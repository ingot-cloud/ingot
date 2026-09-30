# 平台角色分配与委派管理增量（2026-09-28）

状态：approved → implementing。批准依据：jy 于本会话明确要求 IMPLEMENT THIS PLAN，覆盖两端实施；既有主 change 保留 implementing。

## 需求与规则

平台优先。有效委派自动提供受限分配入口，不产生完整治理、业务、角色创建、委派创建或诊断资格。本人业务授权同操作范围并集，委派限制独立；一次分配不得拼接来源。受限记录查询/详情/预览/调整/撤销仅限本人持有的委派；来源、主体、角色版本不可修改。平台不接受部门范围。

人员页简单直接角色管理要求非委派的明确分配资格；替换同时要求撤销资格，并校验目标成员范围。快捷新增与移除均有同事务审计。

委派收窄必须完整重验角色、人群、逐操作范围、期限和既有派生授权；有冲突时原子拒绝，先调整/撤销再保存。委派、组和角色变更与分配事务互斥；关键写最新鉴权，提交后缓存失效。

## 交互

Tab 为角色/角色分配/授权管理员，各自鉴权，仅激活可见 Tab 请求。受限成员先选择授权依据（一条自动选择，多条分页选择），联动固定角色版本、人群、对象和期限；直接治理默认无来源。受限人员页角色只读，快捷分配使用统一入口。

委派操作按应用→资源→操作选择，逐操作上限完整，OBJECT_SET 绑定具体对象。同资源同类型参数可复用，跨资源对象集合禁止复用。平台已实现资源提供搜索分页及已选 ID 回显，未适配资源明确不支持，不改为 ALL。期限编辑用天/小时，Duration 保留秒精度。ISO 瞬时日期必须真实转换 UTC，不把本地字符串直接追加 Z。

## 接口与数据

新增 GET /v1/platform/assignments/context（directRead/directCreate/directUpdate/directRevoke、有效委派数）。新增 GET /v1/platform/assignments/{id}。
新增 GET /v1/platform/assignments/candidates，kind 为 DELEGATION/ROLE_REVISION/MEMBER/GROUP/OBJECT，支持 delegationGrantId、revisionId、parameterKey、actionId、keyword、ids、page/pageSize。服务端始终按当前身份和单一来源过滤，返回最小 Option(id,name,summary,roleRevisionRef,parameterDefinitions,grants)分页，固定版本额外携带 actions 的应用/资源/操作元数据。
新增 GET /v1/platform/delegations/candidates（MEMBER/ROLE_REVISION/OBJECT）与 /v1/platform/authorization/diagnose/candidates（MEMBER/APPLICATION/ACTION/OBJECT），使用同一候选契约但分别验证委派编辑/诊断权限；OBJECT 以实际被保护接口的对象标识查询，响应包含 supported 与 unavailableMessage。不要求先取得通用 CRUD 列表权限。

AssignmentRecord 保留原字段，增补 subjectName、roleName、revisionNumber、delegationSummary、createdAt、grantedBy、effectiveStatus；返回逐条 capabilities。createdAt 为请求当地墙钟，validFrom/validUntil 保留 ISO UTC Instant。授权人批量取最早 CREATE 审计，缺失为空显示未知；添加 assignment_id/change_type/occurred_at/id 审计索引。不以生效时间伪造创建时间或委派管理员伪造授权人。

DecisionSource 使用 assignmentId/delegationId/roleRevisionRef/summary 契约，真实来源和范围，披露受当前操作者限制。诊断仅只读，跳转重新鉴权。

## 验收

PR-A01 纯委派入口及不可扩权；PR-A02 本人权限重合/相交/独立及多来源拒绝；PR-A03 越界来源/角色/人群/对象/期限及全部记录入口；PR-A04 收窄/撤销/到期/组变化/并发；PR-A05 人员快捷新增与移除；PR-A06 创建/生效/到期/撤销/多时区/缺失审计；PR-A07 诊断披露、分页回显、迟到响应。

相关单元/组件测试、HTTP/MySQL、类型、只读 lint、边界、两管理台构建。真实环境证据未执行不得标验收完成。共享租户组件保持兼容，不拓展租户业务，不提前 current，不创建 commit。

## 开发及验证记录

2026-09-28：PR01–PR06 开发与相关自动化已完成；PR07 实际环境 HTTP/浏览器验收未完成，主状态保持 implementing。结果、命令与待验收边界见 [AUTHORIZATION-REFINEMENT-STATUS](./AUTHORIZATION-REFINEMENT-STATUS.md)。
