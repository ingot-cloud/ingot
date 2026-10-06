# IAM 自定义资源接入

适用于新增或修改业务资源的权限执行，目录配置本身不会让业务接口自动受保护。平台普通字段权限纳入角色固定版本；现有共享角色、租户策略和通讯录按原路径执行。

## 资源和操作

- 使用 `ingot-authorization` 的 ResourceKey(domain, applicationCode, resourceCode)，不能只按资源编码、IamAction 枚举、操作编码前缀或集合第一项猜归属。
- 目录的 application_id/resource_id 关联必须与注册描述一致。业务精确操作可用编译期常量，不必扩充 IAM 核心枚举。
- ResourceObjectProvider 注册实际对象ID（可UUID）、可信读写模式、可靠支持的范围、树能力、实际DTO字段及默认策略。候选/回显/存在性与执行接口使用同一ID；未知或未接入能力明确拒绝，不回退ALL。
- 同一参数跨操作共享时必须同应用、同资源、同类型。操作新增后独立发布固定版本，不修改已发布版本。

## 准入和执行

`HasAnyAuthority` 是旧 authority 注解，不能证明 IAM 对象或字段范围。`IamAccess` 是 IAM 服务的身份、治理/委派准入与发号门面，不应让独立业务服务直接依赖 IAM 实现。新业务服务使用 `AuthorizationAccess` / `@RequireIamAction`；精确注解只做操作准入。

- READ_ONLY 走同源在线求值及最长30秒热缓存；MUTATING 必须fresh。依赖统一分层缓存和失效总线，不开放授权 LKG/地板降级放行。到期结果必须拒绝，即使仍在物理TTL内。
- 列表、count、详情、导出在SQL层应用 ScopeSql 的同一条件；租户列来自可信身份。完整范围保留 OR 条款和每条 AND 维度，禁止把多个交集部门集平铺成并集。
- 详情/写入用真实实体ID、ownerMemberId、tenantId、departmentIds构造 ScopeTarget。创建验证新归属；转移归属验证旧/新两端。写入在业务事务内锁定对象后fresh重验；不能只信前端预览。
- 使用 `FieldPolicyProcessor.forAction(decision, actionCode)` 取精确操作字段结果，字段结果只在各操作内，禁止跨操作取值 或把其他操作/角色的 FULL 拼给本操作。多角色同目标按 FULL > MASKED > HIDDEN 并集，每条贡献范围先施加委派上限。固定版本缺少新字段时隐藏，缺失快照拒绝，不回退资源默认。
- FieldPolicyProcessor 在序列化前投影注册字段，HIDDEN省略、MASKED由后端脱敏。身份等固定公开字段用专用DTO白名单，不直接返回实体。写入只允许FULL+editable；显式null、未知字段和脱敏占位必须校验。原值筛选/排序检查整份策略，不能靠当前页样本放行；导出沿用范围与投影。

## 独立服务

IAM侧只从服务器白名单注册固定服务发现名和完整描述，不能接受客户端URL。资源对象RPC使用专用HMAC密钥及短时间窗，并校验原始认证身份与签名中的AuthorizationContext完全一致；内部header不替代签名。候选仅返回配置所需最小信息，不返回敏感字段。远程故障明确503，合法空结果不可当故障或ALL。

参考 [可编译样板](../../../../examples/iam-ops/README.md) 和 [部署/验收](../../../../specs/changes/active/20260912-iam-identity-access-management/RESOURCE-EXTENSION-VERIFICATION.md)。接入至少验证命名空间隔离、范围SQL的page/count一致、详情和写入真实目标、字段隐藏/脱敏/显式清空、身份变化、到期和RPC故障。

## 平台角色字段闭环

资源注册与目录只声明真实 DTO 字段、能力上限和安全默认；具体可见性和可编辑性由平台自定义角色按资源配置一次并冻结版本。字段变更发布新版本、分配显式升级，不按成员/组独立覆盖。治理身份不绕过字段策略，完整可见显式授权。

逐操作 ActionDecision.fields 携带正向授权条款；必须保留各分配的独立范围，不用合并最大范围搭配另一角色 FULL。写操作本身必须提供 FULL+editable，只读 FULL 不补写操作。v2请求只声明资源与操作，每个精确操作字段结论必需，缺失即拒绝。列表批量读快照并在逐目标投影，不能逐行查询或 RPC。

框架未投产，直接使用平台角色字段模型；无启用开关、独立平台成员/组策略或迁移兼容路径。最新完整初始化为平台治理版本冻结安全字段默认，测试库由用户按验收文档重建；租户字段策略和通讯录保持既有语义。
