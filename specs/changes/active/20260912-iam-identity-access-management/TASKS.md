# Tasks

> 状态：implementing。2026-09-19 核对：下列 T 项为开发与验收合并的父任务；2026-09-15/16 的多数修正已落地，不能再按“全部未实施”理解。已开发子项见本文“开发完成标记”，验收证据见 IMPLEMENTATION/MANUAL-VERIFICATION。父任务未满足完整退出条件仍不勾选。

## 范围与执行顺序

本次按全新系统交付，以权限角色及其必要关联接入为主。框架复用和清理规则见 REMEDIATION。先完成后端研发及必要编译/针对性回归，再集中做 MySQL、跨服务、多实例及性能验收；不重写无关成熟框架，不修改前端代码。

顺序：T01 职责与契约盘点 → T18 复用边界及调用链梳理 → T04/T05 基础闭环 → T06–T12 业务修正 → T03/T13 契约与登录交付 → T16 后端综合验收 → T17 前端联调。T18 清理随替换逐步进行，未接通必要功能前不能删除唯一实现；本任务并非要求多 Agent 执行。

## 当前任务与验收

| 状态 | 编号 | 工作及具体退出条件 | 关联 |
|---|---|---|---|
| [ ] | T01（重开） | 以163入口映射核对完整保留功能，补齐账号/本人/辅助/RPC/Security；逐项记录权威模块、DTO、ACTION、范围和证据；确认资源操作目录及DDL职责，不以55表/132操作为完成标准 | C09、C16、C17 / A19、A22、A23 |
| [ ] | T03（重开） | 补充平台无租户登录、租户登录、对象能力、真实草稿预览、错误与全部保留能力夹具；生成快照与实际控制器一致 | C10、C14、C19 / A24、A28 |
| [ ] | T04 | 完成IAM命名、配置/部署及Auth/Member/Security必要调用链；新库查询新模型，保留框架与Member原语义；镜像真实启动 | C09 / A18、A23 |
| [ ] | T05 | 完成新账号接入、成员/部门/身份、空库初始化、平台登录不返回租户、所有者治理转交及并发控制；安全状态复用现有端口 | C07、C08、C16、C19 / A01–A03、A09、A21、A26、A28 |
| [ ] | T06 | 目录/开通/套餐/人群实际生效，部门下级展开、治理入口保护、菜单域及应用过滤；无隐式业务授权 | C12、C15 / A02、A08、A19 |
| [ ] | T07 | 共享/独立自定义/差异角色全部可用；校验资源能力与参数；三方预览；升级只更新本租户当前角色选中授权，逐条重验原子提交 | C01、C11 / A04、A05、A16、A25 |
| [ ] | T08 | 区分治理和受限分配，绑定操作者单一委派；组变化影响检查、完整部门展开及持续接收/版本/范围/期限约束 | C02、C03、C12 / A08、A11、A12 |
| [ ] | T09 | 完整授权求值、一致快照、精确到期、缓存命中期限检查、提交后失效；关键写最新授权；批量规则加载 | C03、C06、C12、C18 / A07、A12、A15、A16、A20 |
| [ ] | T10 | 全部必要资源/安全入口接入功能和对象边界，SQL范围、写两端及并发；完整多实例导出与重验 | C07、C09、C13 / A06、A09、A10、A18、A19、A27 |
| [ ] | T11 | 通讯录精确允许/禁止/默认/本人规则，统一搜索树详情候选count；字段固定默认/平台上限及原值推断防护 | C04、C05 / A13、A14、A20 |
| [ ] | T12 | 草稿预览/诊断/批量对象能力真实求值；披露边界；审计前后摘要、版本及授权/委派关联完整；bootstrap菜单一致 | C14、C15、C18 / A16、A17、A19、A24 |
| [ ] | T13 | IAM局部HTTP错误映射，不改全局既有协议；发布完整API/登录/前端契约，逐入口验证OpenAPI与真实请求响应；候选purpose/筛选/导出状态明确 | C09、C10、C14、C19 / A23、A24、A28 |
| [ ] | T18（新增） | 复用LockStatePort及既有安全用例；移除重复IamAccountLockState Entity/Mapper及同类冗余；替换调用链后删除旧IAM/PMS授权实现和双读；保留其他模块仍需代码 | C17 / A22、A23 |
| [ ] | T16 | T01/T03–T13/T18研发完成后，执行本轮全部A系列；真实MySQL并发/约束、HTTP、镜像/RPC、多实例缓存与导出、性能及框架接入回归 | A01–A28（含子项） |
| [ ] | T17 | T13/T16后接收前端F系列证据，验证平台直接进入、租户独立认证、迟到响应隔离及业务页面；未通过不声明产品可启用 | F01–F09 |

T01/T03 原完成仅覆盖当时局部模型及结构快照，现因遗漏功能和登录契约重开。T04–T12 原有源码工作继续保留；具体缺陷未修复不能仅把剩余工作写成“人工窗口/联调”。

## 本轮实施进展（2026-09-16）

已实施：C17（T18 首项，复用 `LockStatePort` 并移除同表实体/Mapper，框架 DDL 归属入库文档）、C10（T13 的 IAM 局部错误映射）、C19（T05/T13 的显式登录域与平台空 `tenantAllows`）、C16（T05 的正式冷启动，SQL 目录种子加默认关闭的账号启动器）、C06（T09 的快照期限精确截断与写路径不吃热缓存）、C12（T08/T09 的组与人群按部门任职实时展开，含 `includeDescendants`）、C02/C03（T08/T09 的治理权与受限委派分离准入、单一委派绑定、范围上限字面包含及求值期持续校验）、C01/C11（T07 的角色形态定型、资源能力与参数强校验、升级限定本租户当前角色并逐条重验条件更新）、C07/C08（T05 的成员写路径条件更新与所有者治理转交）、C04/C05（T11 的通讯录 ALLOW/DENY/默认/本人与字段固定默认/平台上限及原值推断防护）、C14/C15（T12 的对象能力、字段草稿预览、诊断对象校验与菜单域/应用过滤）、C13/C18（T10/T09/T12 的完整多实例导出与审计关联、规则批量加载）、C09（T04/T05/T10/T13 的平台账号、本人资料、辅助入口、RPC 去旧表回退与 Security ACTION）、C17 清理（T18 删除已替代 HTTP/旧引擎/双读，快照与写入门闩走求值器）、T13 契约重发（账号/me/辅助/purpose/筛选/导出状态写入 OpenAPI，与 `/v1` 控制器对账）、T16 首轮（隔离 MySQL 结构/种子与自动化回归，未勾选）。落点与证据见 REMEDIATION §3.1 与 IMPLEMENTATION 同日各节。

对应任务仍未勾选：T05 代码已落地；人工 HTTP 已确认 A01/A01a/A01b/A03/A09.1/A09.2/A18.2/A26/A28/A28a，仍缺 A21。T06 已确认 A02，仍缺 A08。T07 已确认 A04，仍缺 A05/A16/A25。T11 仍缺 A13、A14、A20；T12 仍缺 A17、A19、A24；T10 仍缺 A10、A27；T18 仍缺 A22、A23 进程与 RPC；T13 仍缺 A23/A24 全入口。T16 不勾选。

待验证顺序：测试数据 D01–D05 独立测试数据准备支持 A21 空库冷启动 → A18.1 镜像 → 其余 A 项（A08、升级、多实例导出、字段/通讯录、诊断全入口）与 F/L 系列。数据准备任务不代替对应业务验收。

## 已退出范围的任务（保留记录，不勾选为完成）

| 编号 | 状态 | 已有工作与处置 |
|---|---|---|
| T02 | cancelled（2026-09-15） | 已有源schema预检和夹具；因用户选择全新系统，取消真实源库快照及迁移预检后续工作 |
| T14 | cancelled（2026-09-15） | 已有JSON目标目录导入/verify工具；取消SQL历史数据导入研发，不声称真实迁移已完成 |
| T15 | cancelled（2026-09-15） | 取消真实快照切换演练；不作为T16/T17依赖 |

MIGRATION/SOURCE-TABLES、迁移工具及相关历史记录保留，整个change仍active；需要再次处理迁移时另行对齐范围。迁移运行时入口不纳入本次交付。

## 既有 MyBatis Plus 工作的证据边界

MP01–MP06 曾记录实体/Mapper、身份/目录/授权/策略访问迁移、构造注入及生产JDBC清零，相关编译/H2证据保留在 IMPLEMENTATION。该历史完成不证明领域归属合理、锁定态不重复、并发/业务权限正确或本轮已经验收。后续按T18复用框架、按T16验证；不得为维持机械“全部映射”而保留重复Mapper。公开类型注释、枚举、构造注入和统一缓存门禁继续适用。

## 完成检查

- [ ] 本轮所有在范围内任务及A系列完成，有对应证据；取消任务有原因，不伪造完成。
- [ ] 旧能力无遗漏、被替代实现已清理；安全框架唯一权威、无无关功能重构。
- [ ] API、前端登录文档、实际HTTP/JSON一致，平台无租户列表；未验证快照不宣称就绪。
- [ ] 未验证项不勾选，不以H2或结构测试代替真实HTTP/MySQL/多实例。
- [ ] 验收后更新current及被替代PMS引用，再归档本change；无提交请求不创建commit。

## 双入口 BFF 增量任务（2026-09-19 校准：B01–B05 已实现，B06 待验收）

本节为双入口 BFF 实施范围；规格已确认（Host→appId、Nacos 回跳、不传 URL）。主 change implementing 的既有 IAM 证据保留，本增量任务独立勾选。

| 状态 | 编号 | 工作及退出条件 | 依赖 / 验收 |
|---|---|---|---|
| [x] | B01 | 确认应用注册、四 host、两 OAuth client、固定域接口与错误/CSRF契约，完成配置验证规则 | 增量确认 / L11 |
| [x] | B02 | 独立 LoginTransaction、浏览器绑定、10分钟TTL、原子阶段、60秒ticket与未交接Token清理 | B01 / L04–L06 |
| [x] | B03 | 平台/租户入口与 Auth RPC 参数贯通；平台无tenant授权、租户零单多候选与资格重验 | B01/B02 / L01/L02/L07/L08/L12 |
| [x] | B04 | 目标管理台complete建立正式会话、设备指纹、host-only Cookie、当前应用退出及Gateway域/tenant校验 | B02/B03 / L03/L04/L06/L09 |
| [x] | B05 | returnTo严格校验、CSRF/Origin、no-store/日志脱敏、旧无域入口退出、失败与取消清理 | B02–B04 / L05/L08/L09/L12 |
| [ ] | B06 | 发布BFF契约与四站配置，单元/集成/真实浏览器验证，接收前端全量IAM证据 | B01–B05 / L01–L12、F01–F09 |

顺序：B01 → B02/B03 → B04/B05 → B06。B06 是 T16/T17 新增依赖；B01–B05 已在本轮落地（应用注册、双入口、事务/CSRF、host-only Cookie、Gateway Host 注入）。B05 增量：complete 后绑定 TTL 跟随 `session-ttl`；logout CSRF 失败仍清本机会话；authorize/token 窗口失效映射 `BFF_TRANSACTION_EXPIRED`。B06 浏览器与全量 IAM 证据未做，不勾选。

## 2026-09-19 开发完成标记（不代替父任务验收）

本节按当前源码与 IMPLEMENTATION 的已有记录补标。`[x]` 仅确认本行限定的开发内容。开发状态：已实现=本行源码范围闭合；部分实现=源码存在但范围不足。验证分自动化 / 真实接口 / 端到端；历史日期不视为本轮重跑。T01/T03–T13/T18仍需按各自原退出条件核验全范围。

| 开发 | 子项 / 父任务 | 开发状态 | 自动化 | 真实接口 | 端到端 | 已落地内容与源码定位 | 剩余事项 |
|---|---|---|---|---|---|---|---|
| [x] | T04.dev | 已实现 | 历史编译记录 | 未执行镜像HTTP | 未执行 | IAM模块/服务名及新控制器/RPC命名接入 | A18/A23 |
| [x] | T05.identity | 已实现 | 相关单测存在 | 2026-09-16 A01/A01a/A01b/A28/A28a | 未执行 | ActiveIdentityService、AccountIdentityService、MemberLifecycle | 完整竞态 |
| [x] | T05.bootstrap | 部分实现 | 006种子测试 | 2026-09-16 A03 | 未执行 | PlatformBootstrapService、TenantInitializer与正式006种子 | A21空库进程首启 |
| [x] | T05.owner | 已实现 | 转交相关测试 | 2026-09-16 A26（含409） | 未执行 | TenantQueryService.transferOwner与条件更新 | 并发移出新所有者 |
| [x] | T06.catalog | 已实现 | 相关单测存在 | 2026-09-16 A02 | 未执行 | CatalogService、EntitlementService及组/部门人群展开 | A08、A19 |
| [x] | T07.role | 已实现 | RoleServiceTest | 2026-09-16 A04 | 未执行 | RoleService、RoleGrantValidator | A05/A16/A25 |
| [x] | T08.delegation | 已实现 | AssignmentServiceTest/GroupServiceTest | 未执行完整HTTP | 未执行 | AssignmentService、DelegationService、DelegationAdmission、GroupService | A11/A12 |
| [x] | T08.group-name | 已实现 | GroupServiceTest.listFiltersByName、MemberQueryRepositoryTest | 未执行 | 未执行 | 组列表 `name` 包含匹配；平台成员列表可选 `ids` 回显 | 不补勾 T08 |
| [x] | T08.group-members | 已实现 | MemberQueryRepositoryTest.pagePlatformByGroupFiltersMembershipAndName | 未执行 | 未执行 | `GET /v1/platform/groups/{id}/members` 按组成员关系分页，可选 `name` | 不补勾 T08 |
| [x] | T09.evaluation | 已实现 | 求值测试记录 | 未执行多实例 | 未执行 | AuthorizationEvaluator、ScopeBinder | A15/A16 |
| [x] | T10.scope | 部分实现 | 成员生命周期测试 | 部分HTTP | 未执行 | 成员/部门范围、MemberMutationGuard与ObjectCapabilities | A06/A07/A10/A19全入口 |
| [x] | T10.export | 已实现 | MemberExportServiceTest | 未执行≥200/多实例 | 未执行 | MemberExportService、007_member_export | A27 |
| [x] | T11.policy | 已实现 | 相关单测记录 | 未执行真实披露 | 未执行 | DirectoryVisibilityEvaluator、FieldAccessEvaluator、PolicyService | A13/A14/A20 |
| [x] | T12.session | 部分实现 | 菜单/能力/诊断单测 | 未执行 | 未执行 | SessionMenuAssembler、ObjectCapabilities、DiagnoseAuditService | 非成员资源能力铺开；A17/A19/A24 |
| [x] | T13.contract | 已实现 | 2026-09-16 test_contract.py 7项 | 未执行逐入口HTTP | 未执行 | 账号/me/辅助/purpose/导出状态API与96/161重发 | A23/A24、前端消费 |
| [x] | T18.reuse | 已实现 | C17静态+锁定用例 | 未执行进程RPC | 未执行 | 复用LockStatePort、移除重复锁定实体及已替代HTTP/旧引擎/双读 | 旧目录域服务未整包删除；A22/A23 |
| [x] | B03.member-context | 已实现 | 2026-09-18 5项测试 | 未执行BFF浏览器 | 未执行 | AuthenticatedMemberBinder | 不补勾B06 |

## 2026-09-19 独立测试数据任务

设计与场景见 [TEST-DATA](./TEST-DATA.md)。下文 **D01–D05 专指测试数据任务**，与 2026-09-13 已确认的 **DESIGN D01（平台域授权主体）** 不是同一编号。D01 工具与 D02 构建器已实施并通过本地 unittest；独立环境导入与 D03–D05 尚未实施，不将开发子项当作产品验收完成。

| 状态 | 编号 | 工作与退出条件 | 依赖 / 覆盖 |
|---|---|---|---|
| [x] | D01 | 建立独立环境配置、prepare/verify/reset、运行清单、环境标识校验及重复运行/漂移处理 | 正式DDL/bootstrap；TD01 |
| [ ] | D02 | 通过真实账号/组织/目录/角色/分配/策略接口构建多身份、多租户、多部门、多组和版本场景 | 测试数据 D01；TD02–TD11/TD15 |
| [ ] | D03 | 补250+成员导出、动态期限、并发/撤权/多实例、四站登录及辅助/外部依赖的执行步骤和数据 | 测试数据 D02；TD12–TD18 |
| [ ] | D04 | 按 TEST-DATA 第5节底稿完成“功能/接口→TD场景→A/F/L→前端页面”打勾，补齐未覆盖的已实现能力 | 测试数据 D02/D03；全部A/F/L及前端逐操作矩阵 |
| [ ] | D05 | 独立环境实际准备、重复准备、重建与verify；输出脱敏报告、Bruno环境和前端使用说明 | 测试数据 D04；支持T16/T17/B06，不自动证明通过 |

- [x] D02.dev.builder：`build` 通过 `/iam/v1` 创建账号/A-B 组织/部门/成员/组/共享角色/分配，清单对象复用，口令只写 secrets 文件；mock HTTP unittest 已通过。创建前 lookup 已有登录名；组织按名称、所有者按 `ownerMemberId`、成员按显示名复用；共享角色引用 `iam-tenant` 成员读写；重置口令后回写版本，停用/暂停前刷新并在 409 重试。独立环境未导入，父任务 D02 不勾选。
- [x] D02.dev.guide：已补 [VERIFICATION-GUIDE](./VERIFICATION-GUIDE.md)（功能导读、建库/导入逐步操作、四站登录与按页步骤）。真实导入与浏览器证据仍属 D02/D05 与 F/L，不勾选父任务。

- [x] DOC01：2026-09-19 已校准开发/验证状态并补充测试数据规格；续补场景卡、编号区分与验证分栏。未修改实现、未导入数据、未执行产品验收。
- [ ] DOC02：测试数据 D01–D05和前端全部业务流程完成后，补真实证据，再按T16/T17/B06及current/archive门禁收尾。
