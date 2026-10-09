# 已验证公共类型快照

`schemas.json` 是当前模型组件（含泛型实例和既有 R 信封）的 OpenAPI components，由 `IamAuthorizationContractTest` 使用 swagger ModelConverters 从 Java 类型导出。字典/发号/社交等无 IAM DTO 的成功信封在 OpenAPI 中补 `RJson`；`RVoid` 来自 `R<Void>`。它不是运行时接口证明，也不表示服务已提供这些接口。

`examples/` 包含已通过 Jackson 反序列化与嵌套 Bean Validation 的示例：共享只读角色、租户差异、分配、委派、字段规则、bootstrap、成员详情/分页、诊断、预览、升级冲突、审计、策略，以及角色创建/发布/升级和组织创建请求。示例 ID 均为测试数据，不是线上可调用对象。

来源：`ingot-framework/ingot-commons/src/main/java/com/ingot/framework/commons/model/iam/`；测试示例来源：同模块 `src/test/resources/iam/`。

再生成流程：

1. 执行 `./gradlew :ingot-framework:ingot-commons:test --tests '*Iam*ContractTest' --console=plain`。
2. 确认全部测试通过，执行 `python3 tools/iam/generate_routes.py` 与 `python3 tools/iam/build_contract.py` 同步 schemas 并生成目标 openapi.json。
3. 将同模块 `src/test/resources/iam/*.json` 同步到 examples，并核对 API.md。
4. 执行 `python3 tools/iam/build_contract.py --check` 与 `python3 tools/iam/test_contract.py` 检查快照、引用、域边界、查询参数、purpose/导出状态，以及 `/v1` 控制器映射（排除 OSS 与 inner）。这些检查不替代完整 OpenAPI 规范验证器或实际 HTTP 集成测试。

测试检查必填字段、字符串 ID/版本、ISO duration、UTC 时间、schema 引用完整性、泛型实际资源类型，以及校验方法不会被当作 JSON 属性。响应测试同时加载应用 InModule，验证统计数量仍为数字、隐藏字段省略、受限统计不伪造为零、R 信封保持稳定错误码。示例主要展示 data 内容，实际 HTTP 使用 R<T> 包装。OpenAPI 3.0 不能完整表达期限大小比较、字段可编辑性和差异结构等跨字段规则，实际请求仍须执行 Bean Validation 及业务校验。

## 2026-09-16 T13 重发

当前 JSON 快照包含 96 条路径、161 个操作，覆盖管理面、账号/本人资料、字典/发号/社交包装入口、候选 purpose、列表筛选与导出任务状态。`implemented` 仅表示 `/v1` 控制器已接入，不能证明权限、错误状态或业务语义验收。OSS 与内部 RPC 不进入本 OpenAPI。真实 HTTP 仍待 A23/A24/A28。前端不得将 77/132 旧快照当作本轮基线。

## 双入口 BFF 文档边界

2026-09-16 新增的 [BFF-LOGIN](../BFF-LOGIN.md) 是已确认契约。2026-09-19：后端 B01–B05 已落地，B06 四站浏览器与全量 IAM 证据未做。BFF 路径不属于 `/api/iam/v1`，不混入本目录已接入的 96/161 快照。前端必须同时读取 BFF-LOGIN，不能因为 OpenAPI 标记已接入就认为登录完成。

## 2026-09-28 平台角色分配增量

本次快照为 **115 条路径 / 182 个操作**。补齐源码已存在的目录/人员辅助入口，以及平台分配上下文、详情、更新预览、三类专用候选、委派创建预览。`assignment-context.json`、`assignment-record.json`、`authorization-candidates.json` 为新增验证夹具；AssignmentRecord 的授权创建时间是请求当地墙钟字符串，分配/委派有效期仍为 UTC ISO Instant。

委派候选与诊断候选的 kind 和参数各自受限，不能把一种编辑器的权限当成另一种编辑器的权限。上下文允许分配四种入口中的任意一种；委派编辑候选允许 READ/CREATE/UPDATE 任一独立治理资格，`x-iam-execution` 记录组合准入规则。契约与控制器检查已通过，仍不等同于真实 HTTP 验收。


## 2026-09-29 角色单选树增量（待手动验证）

快照增加角色树候选，当前 116 路径 / 183 操作。新增 AuthorizationRoleCandidatePage、AuthorizationRoleNode 与对应 R 信封；根与版本示例均同步到 commons 的夹具目录。schema 本轮按 Java 契约人工同步，契约导出入口已登记新类型；用户要求自行测试，因此未执行 Java 导出测试、契约 --check、HTTP 或构建。历史验证结果不覆盖本轮。后续可按前述再生成流程核对，不能据工件生成标记验收通过。

2026-09-30：平台分配列表 GET 在原分页参数之外增加可选 `subjectType` 和 `keyword`，租户列表不变；已从当前 schema 和路由清单刷新 OpenAPI。路径/操作数仍为 116/183；实际 HTTP 筛选和权限边界待人工验证。

2026-09-30：新增平台委派管理专用两级角色树候选，当前快照为 117 路径 / 184 操作。复用已导出的角色树响应类型，候选独立检查委派治理资格；路由与 OpenAPI 生成检查已通过，真实 HTTP 与浏览器验收仍待执行。

2026-10-02：新增租户角色分配范围对象候选，当前快照为 118 路径 / 185 操作。候选按固定版本、参数、可信租户及资源适配器过滤；预览和写入重验对象归属。真实 HTTP 与浏览器验收仍待执行。
## 2026-10-02 增量

MemberCreateInput 增加固定版本分配草稿；平台成员关联分配增加独立分页路径；范围候选在不改变普通列表默认行为的前提下增加树分支参数与层级元数据。OpenAPI 与前端来源副本须据 Java 契约重新生成，并保留真实 HTTP/MySQL 验收任务。

受限平台菜单树的祖先导航节点返回 `selectable=false`，实际可选对象仍由委派范围限定；Java 契约导出、路由生成与本地契约检查已重跑，真实 HTTP/MySQL 尚待验收。

## 2026-10-03 角色工作区与委派期限

当前快照 **123 路径 / 190 操作**，新增角色成员、组、成员真实来源以及委派已选关系分页。期限模式省略为 LIMITED；UNLIMITED 仅平台可用，时长为空，长期派生分配仍依赖来源有效性。新增 `delegation-unlimited.json` 与 `role-subject-page.json` 经 Jackson / Bean Validation 验证；版本号、可见计数保持数字。候选及已选关系均支持接收名单排除当前管理员。
2026-10-03：平台分配列表增加可选 `effectiveStatus` 五种计算状态过滤，路径/操作数保持 123/190；响应模型、租户列表和人员关联列表不变。路由及 OpenAPI 已从现有模型刷新，生成一致性和 7 项契约检查通过，实际页面人工验收单列 AS03。


## 2026-10-05 平台多角色分配与记录范围回显

当前快照 **124 路径 / 191 操作**，新增分配 `/{id}/selected-candidates`，只接受 ROLE_REVISION／OBJECT（OBJECT 必填 parameterKey），沿用详情可见边界。`assignment-multi-role.json` 与 `assignment-selected-candidates.json` 已通过 Jackson／嵌套 Bean Validation；逐角色参数互相独立，提交仍为原 AssignmentBatchInput。Java 契约 12 项、路由／OpenAPI 生成及 7 项契约检查通过，隔离 MySQL／真实 HTTP 亦验证固定版本、实际已选关联与整批回滚。页面人工验收待 MA05。

## 2026-10-05 资源扩展增量

管理面新增资源执行/字段策略及候选、分配升级与专用树/对象候选；当前 **133路径 / 201操作**。`resource-field-policy.json`、`assignment-upgrade.json` 与 `authorization-v2-request.json` 经 Jackson 与嵌套 Bean Validation 验证。内部 v2 与签名对象端点单列 `internal-openapi.json`，生成 `python3 tools/iam/build_internal_contract.py`，一致性检查附加 `--check`；不加入浏览器权限目录。FieldPolicyDecision 同时传输 filterableFields/sortableFields 及完整范围，原值筛选/排序需结合当前可见性检查。


## 2026-10-05 平台角色字段固定版本（最终契约）

当前公开快照 **129路径 / 196操作**，新增平台角色创建只读预览；角色草稿、固定版本、权限目录、详情、诊断来源和升级预览携带资源字段快照。`role-platform-fields.json` 与 `authorization-v2-request.json` 经Jackson/嵌套校验验证。内部v2每个操作传输独立字段条款与GRANTS合并模式，字段结论必需；请求只携带resource/actionCodes，无roleFieldsSupported，响应没有roleFields或顶层fields。

平台旧独立策略API、枚举和夹具已删除，不保留deprecated兼容端点；租户FieldPolicyInput/FieldRule及通讯录契约保持原行为。资源执行能力由角色权限目录提供，不再由独立平台策略入口查询。创建和修改成员隐藏的suppliedFields只用于服务端校验，不进入传输模型。当前无部署开关或迁移期；自动化与人工状态见[增量任务](../ROLE-FIELD-AUTHORIZATION-REFINEMENT.md)。

上文资源扩展增量中的独立平台策略夹具及133/201快照仅记录历史，不能用于当前调用。

## 2026-10-06 平台成员联系资料

平台成员的phone/email改为独立联系资料，空白清空后不回退全局账号，创建时仅一次复制账号初值。MemberRecord/MemberProfileInput的字段和请求结构不变，仅更新描述；Java契约导出、OpenAPI一致性及7项契约检查通过。快照仍为129路径/196操作，迁移和人工验收见[增量说明](../PLATFORM-MEMBER-CONTACTS-REFINEMENT.md)。

## 2026-10-06 平台系统超管与在线快照

公开管理面路径和请求保持129/196；内部快照 `/inner/authorization/snapshot` 新增服务器产生的 `platformAdministrator`，与业务 `permissionCodes` 分离，租户恒为false。内部OpenAPI现在有3个端点，包含原v2/签名对象接口。DTO位于iam-api，快照字段描述在内部生成器独立维护；在线快照序列化、源端身份重验与消费端拒绝旧JWT由相应回归验证。`authorization-snapshot.json` 为脱离实际身份的示例，不是客户端可以提交的超管凭证。


## 2026-10-06 平台成员字段上下文

公开快照 **130路径/197操作**，新增平台成员context（READ/CREATE任一准入）。PlatformMemberContext只携带潜在列可见性、新对象创建字段与显示名原值查询资格，不能作为逐行/提交授权。`platform-member-context.json` 是R响应信封夹具，按Java类型及实际成功码验证；成员列表参数与控制器name/status/ids同步。Java契约14项、生成一致性及Python契约7项通过，成员context隔离MySQL/真实HTTP通过；真实身份/界面验收待MF04。


2026-10-06 平台成员角色编辑：新增 PlatformMemberEditInput/MemberRoleChanges/MemberBoundRole/PlatformMemberEditPreview 与 bound-roles、preview 路由，成员 assignments 增加 effectiveStatus/directOnly；租户 MemberProfileInput 不变。member-role-edit 是差量请求夹具，member-bound-roles 是无历史状态的有效摘要夹具。固定版本创建不要求最新版。

2026-10-06：新增密码 GET 最小状态与 PasswordChangeRequired 403；内部快照必需 passwordChangeRequired。生成管理面 132 路径/200 操作，内部 3 操作；示例 password-change-state.json。规则及共同部署见 [强制改密](../FORCED-PASSWORD-CHANGE.md)。


2026-10-08：MemberRecord 的 joinedAt/lastLoginAt/updatedAt 为可选只读 UTC 时间点，仅平台详情/编辑成功响应填写；详见 [平台成员时间增量](../PLATFORM-MEMBER-TIMES.md)。路径/操作数保持132/200。
