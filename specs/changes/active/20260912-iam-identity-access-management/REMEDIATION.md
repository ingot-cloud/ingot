# 后端复评、职责边界与整改清单

> 2026-09-15，用户要求修订 Spec。下列问题来自当前工作区静态评估，均为待实施/待验证，不代表运行环境复现或已修复。仅评估后端及其交付契约，不评价前端代码。

## 1. 范围与复用边界

本次核心是角色、权限、身份隔离及相关功能接入。按全新系统建设，保留必要业务功能，不保留被替代实现；不以服务改名或新数据库为由重写成熟框架。

| 能力 | 权威归属 | IAM 可调整内容 | 禁止扩展 |
|---|---|---|---|
| 账号锁定状态、失败计数、锁定期限/原因 | security-account 的 LockStatePort、DefaultLockStatePortAdapter、AccountLockStateMapper | 按 accountId/userType 复用公开端口及锁定/解锁用例；配置已有适配器数据源 | 复制 IamAccountLockStateEntity/Mapper；直接 upsert 锁定态；重写阈值/自动解锁/计数逻辑 |
| 账号基础存储 | IAM Account + UserAccountPort 适配 | 新账号存储、身份映射与必要状态字段 | 把锁定态第二次写入当作账号字段同步；端口循环调用；故障回退 SysUser |
| 密码/凭证策略、历史/到期、安全事件 | 既有 security-credential/account/event 能力 | 复用既有用例接到新 Account；保持初始改密、校验与事件闭环 | 新建 IAM 密码策略/历史 Mapper 或绕过用例直接更改密码 |
| OAuth2/OIDC、会话、挑战、传输加密、防重放 | Auth 与现有 security 模块 | 传递已验证成员上下文；平台分支不返回租户；必要授权接入 | 重写协议或通用登录流程；影响 Member(APP) 用户体系 |
| 策略缓存、LKG、地板、事件总线 | 既有 framework | IAM 消费侧正确使用 TTL、期限检查与失效 | 为 IAM 改变其他消费者的降级或事件语义 |
| Security 管理面 | ingot-security 原有服务/用例 | 平台 ACTION、对象边界及必要 RPC 适配 | 把安全业务复制到 IAM 或把租户资料开放给平台 |
| 字典、发号、社会化、上传等 | 原有领域实现 | 必要域/账号引用、授权入口、服务名适配 | 因为旧类名或旧表名就删除仍有用的成熟实现 |

已核对的框架入口：

- [LockStatePort](../../../../ingot-framework/ingot-security/ingot-security-account/ingot-security-account-core/src/main/java/com/ingot/framework/security/account/domain/port/outbound/LockStatePort.java)
- [DefaultLockStatePortAdapter](../../../../ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/java/com/ingot/framework/security/account/adapter/port/DefaultLockStatePortAdapter.java)
- [AccountLockStateMapper](../../../../ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/java/com/ingot/framework/security/account/adapter/mapper/AccountLockStateMapper.java)

安全表可以随新环境部署到 IAM 所用数据库，代码所有权仍归框架；复用框架适配器，不要求跨网络调用 Security 服务来访问本地锁定态。对类似 Entity/Mapper/缓存/校验器先盘点既有实现；只有本次确有缺口才扩展其公开接入点。无法证明必要的框架修改不进入本次实施。

## 2. 必要功能保留清单

以 endpoint-mapping.json 的 163 个原入口为起点，逐项记录新入口、调用方、权威用例、身份/ACTION/范围、状态和测试证据；仅有目标路径不能判定已保留。多入口合并必须列明搜索、排序、状态、详情等子能力，不能漏项。

| 功能 | 目标职责与交付要求 |
|---|---|
| 全局账号查询/创建/详情/编辑/状态 | 平台 accounts 入口；账号与成员独立，查询限制用途，不泄露跨组织关系；创建真实写新 Account |
| 锁定/解锁、密码重置、本人改密 | 复用现有安全用例、凭证校验和事件；账号操作不能通过成员资料接口完成 |
| 本人资料 | me/profile 与当前身份/账号资料边界明确，不能提交别人 ID；不要求保留旧 DTO |
| 平台成员、租户成员、组织、部门、角色、权限、菜单 | 新 IAM 模型及完整读写/筛选/候选/分配能力；旧越权能力不保留 |
| 字典 | 保留树、分页、按编码取项、排序、启停、CRUD 及内部批量读取 |
| 发号 | 保留管理与内部发号能力，初始化可用、并发唯一，不重写发号算法 |
| 社会化登录/配置、上传 | 保留配置 CRUD、密钥保护、绑定及登录、必要上传能力；只调整本次账号/组织引用 |
| Auth、Member 所用组织/账号 RPC | 查询新 IAM 事实；Member 独立账户和安全语义不变；不能只更名 RemotePms→RemoteIam |
| 内部授权快照及 Security 管理面 | 当前域/成员可信校验；精确 ACTION 与对象约束；替换旧授权数据来源及超管短路，保留原用例 |

本清单不增加原系统没有的通用邀请系统、审批流或额外业务模块。旧入口标记 RETIRED 的越权/测试能力不因为“功能保留”恢复。

## 3. 问题、修正目标与验收映射

| 编号 | 静态评估发现 | 必须达到的修正目标 | 任务 / 验收 |
|---|---|---|---|
| C01 | RoleService.upgrade 按任意 assignmentId 更新 | 限定当前租户、当前角色和有效版本；锁定/条件更新，校验范围参数、来源委派、期限；不存在/越界/冲突整批失败 | T07 / A05、A16、A25 |
| C02 | 委派只在请求带 ID 时检查，未绑定操作者及完整额度 | 治理权与受限准入分开，受限方必须使用自己的单一委派；不可省略来源、借用他人或拼接约束 | T08 / A11、A12 |
| C03 | 组影响只检查清空；运行时不持续核对接收人/版本 | 组扩大、任职变化、委派收缩、角色升级持续检查同一委派全部维度；超限成员不受益 | T08/T09 / A12 |
| C04 | 通讯录默认 SELF、ALLOW 追加、规则顺序相关、本人未恢复 | 固定默认版本；匹配 ALLOW 替代默认；最后扣 DENY 并恢复本人基础信息；树/列表/搜索/详情/候选/count 一致且 SQL 过滤 | T11 / A13、A20 |
| C05 | 原值搜索只检查本人字段权限；默认/平台上限未消费 | 对可能匹配目标执行字段披露边界；不能从隐藏值筛选、排序或 count 推断；固定默认与平台约束实际生效 | T11 / A14 |
| C06 | expiresAt 固定现在+30秒，缓存命中未检查期限 | 每次检查 expiresAt；截止最近授权/委派/开通边界；L2→L1 不续命；关键写不用旧缓存授权放行 | T09 / A15、A16 |
| C07 | 成员资料范围校验与写入分离，UPDATE 无 expectedVersion 条件 | 同事务控制身份/归属/版本竞态；锁定或原子条件更新，检查行数；并发冲突不报告成功 | T05/T10 / A09、A10、A16 |
| C08 | 所有者转交只修改 owner_member_id | 转移所有者来源治理授权、保留独立授权、成员可用性与版本检查、同事务审计、提交后失效 | T05 / A26 |
| C09 | accounts/me/辅助能力仅有映射，RPC 仍读旧表，Security 仍用旧准入 | 按第 2 节完整接通；保留成熟业务用例，最终无旧用户/租户授权链依赖 | T04/T05/T10/T13 / A18、A19、A23 |
| C10 | IAM BizException 经全局处理成为 HTTP 500 | IAM 范围内映射 400/401/403/404/409/503；不改变其他服务错误协议；真实 HTTP 测试 | T13 / A24 |
| C11 | 所有 TENANT_CUSTOM 强制共享基础 | base 为空允许完整 grants；有 base 保存差异；校验操作域、资源能力、参数声明与绑定 | T07 / A04、A05 |
| C12 | 组授权只展开显式成员，人群未处理部门下级 | 统一展开直接成员+部门及 includeDescendants，去重并随任职变化；人数与实际集合一致 | T06/T08/T09 / A08、A12 |
| C13 | 导出固定第一页200条，临时文件单机、下载忽略快照 | 完整导出；任务状态/失败/存储/清理明确，多实例可取；执行及下载重验身份、操作、范围和字段 | T10 / A10、A27 |
| C14 | capabilities 为空，字段草稿不参与预览，诊断忽略对象 | 批量返回对象能力；草稿和提交同引擎；诊断校验目标身份、application/action/target 及披露权限；影响未知不能写0 | T12/T13 / A17、A24 |
| C15 | OPEN 菜单跨域/应用边界直接可见 | 先域、应用启停/开通/人群，再菜单 ACTION；祖先按导航规则处理，无子页不展示空父目录 | T06/T12 / A19、F01 |
| C16 | 有建表与人工种子，无完整正式冷启动 | 正式完整目录、治理身份、默认策略与发号初始化；无迁移前提、无硬编码测试口令；幂等且不覆盖业务修改 | T05 / A21 |
| C17 | 旧控制器/引擎/双读与重复 IamAccountLockStateMapper 并存 | 替换调用链后清理旧实现；复用安全公开端口；不复制成熟能力，不删除其他服务仍需框架 | T18 / A22、A23 |
| C18 | 审计摘要/关联不完整，循环查询及内存分页 | 审计完整可追踪但脱敏；规则批量加载、SQL 分页/count，验证查询量和索引 | T09/T12/T16 / A17、A20 |
| C19 | 新账号登录仍填 tenantAllows，平台选择流程未区分 | PLATFORM tenantId=null，不返回租户列表；共享 DTO allows=[]；平台直接 bootstrap，租户/Member 原认证规则不受误改 | T05/T13 / A28、F08、F09 |

## 3.1 已实施条目（2026-09-15）

下列条目已按第 3 节的修正目标落地，落点与验证记录见 `IMPLEMENTATION.md` 同日两节。标注「待人工」的部分仍需真实环境证据，不得据此勾选 A 系列。

| 编号 | 落点 | 自动化证据 | 仍缺 |
|---|---|---|---|
| C10 | `web/IamErrorHandler` 按 `IamReasonCode` 局部映射 HTTP 状态；`IamErrorHandlerTest` 覆盖全部 IAM 码与外来码保持 500 | IAM provider 全量测试 | 真实 HTTP 状态核验（A24） |
| C17 | 删除同表实体/Mapper，复用 `LockStatePort`；框架 DDL 归属写入 `databases/iam/README.md` | IAM provider 全量测试 | 失败计数/自动解锁不退化的运行时证据（A22） |
| C19 | `UserDetailsRequest` 显式 `AuthorizationDomain`；平台分支 `tenantAllows` 为空 | IAM provider 全量测试 | 平台/租户/Member 分支隔离的真实登录（A28、F08、F09） |
| C16 | `tools/iam/generate_bootstrap.py` → `databases/iam/006_bootstrap.sql`；`ingot.iam.bootstrap.enabled` 默认关闭的启动器走 `RegisterUserUseCase` + `InitialPasswordService` | `test_bootstrap_seed.py` 9 项、`PlatformBootstrapServiceTest` 6 项 | 待人工：A21.1／A21.2 真实进程冷启动与首登改密 |
| C06 | 快照 `expiresAt` 取热窗口与最近授权/委派/开通边界的较早者；命中即校验期限，过期先 evict 再重载；`IamActionOperation` 判定改写操作，写路径不吃热缓存 | `AuthorizationEvaluatorTest` 期限截断、过期不放行、写操作绕缓存共 7 项 | 到期精确截断、广播失败与远端故障的真实多实例证据（A15、A16） |
| C12 | `IamMembershipSql` 递归 CTE 统一「显式成员 + 部门任职 + `includeDescendants` 下级」；组分配、人群开通、组成员数三处共用 | `AuthorizationEvaluatorTest` 组/人群展开 5 项、`GroupRepositoryTest` 5 项、MySQL 8.4 手工核对同结果 | 任职调整后授权随之变化的真实运行证据（A08、A11） |
| C02、C03 | 求值 SQL 持续校验委派状态/期限/版本白名单/接收人；`Admission.governed` 标识非委派来源；`AssignmentService` 要求受限方绑定本人单一委派、逐操作核对范围上限；`GroupService` 用同一 `DelegationRecipientRepository` 复核组变更 | `AuthorizationEvaluatorTest` 持续校验 5 项、`AssignmentServiceTest` 10 项、`GroupServiceTest` 6 项 | 待人工：A11／A12 的真实 HTTP、并发与撤销传播证据 |
| C01、C11 | 新增 `RoleGrantValidator` 按操作启停、应用域、资源范围能力与参数绑定失败关闭校验，发布/预览/升级共用；角色形态在首个版本固定（无基础即完整自定义，有基础只存差异）；`RoleService.upgrade` 逐条锁定授权，限定本租户与当前角色、活跃状态、参数已绑定，受限来源复用 `DelegationAdmission` 重验后条件更新 | `RoleServiceTest` 16 项、当时 IAM provider 全量 166 项 | 待人工：A05／A16／A25 的真实并发升级与原子回滚证据 |
| C07 | 成员资料/资格/任职与组织设置的 UPDATE 带 `version` 条件并检查受影响行数；`MemberQueryService.patch` 与 `MemberLifecycle` 在同一事务内先锁行再校验范围与版本；并发冲突报 `REVISION_CONFLICT` 不报告成功 | `MemberLifecycleTest` 增失效与无操作不失效、IAM provider 全量 172 项 | 待人工：A09／A10／A16 的真实并发 HTTP 证据 |
| C08 | `TenantQueryService.transferOwner` 锁组织及新旧所有者，撤销旧所有者 `INITIALIZATION`+`SYSTEM` 治理授权并赋予新所有者；已有同版本有效分配则复用；独立授权保留；同事务审计 `OWNER_TRANSFER` 并 `markAll` | `TenantQueryServiceTest` 5 项、IAM provider 全量 172 项 | 待人工：A26 真实转交、并发移出新所有者与失效证据 |
| C04 | `DirectoryVisibilityEvaluator`：固定默认版本（空 JSON=ALL）；匹配 ALLOW 并集替代默认；全部 DENY 后恢复本人；列表/搜索/详情/count SQL 过滤；部门树祖先 `navigationOnly` | `DirectoryVisibilityEvaluatorTest` 4 项、`DefaultPolicyDefinitionsTest` 3 项、IAM provider 全量 181 项 | 待人工：A13／A20 真实 HTTP 与 SQL 计划证据 |
| C05 | 字段基线读固定版本 `fields`，平台上限读最新版本 `ceiling`（缺省不额外收紧）；原值筛选按查询范围内可能匹配目标披露，不能只看本人 | `FieldAccessEvaluatorTest` 增本人 FULL 不能搜他人、上限收紧 FULL 规则、IAM provider 全量 181 项 | 待人工：A14 真实检索/导出披露证据 |
| C14 | `ObjectCapabilities` 按对象批量计算成员/部门展示能力；字段草稿预览与提交同引擎；诊断校验应用/操作归属、开通人群与 target 范围，来源披露受操作者权限限制 | `ObjectCapabilitiesTest`、`DiagnoseAuditServiceTest` 3 项、`FieldAccessEvaluatorTest` 增草稿快照、IAM provider 全量 190 项 | 待人工：A17／A24 真实 HTTP 对象能力与诊断披露 |
| C15 | bootstrap 先域/启停/开通/人群，再按 OPEN/ACTION 过滤页面；目录只保留有可见子页的祖先 | `SessionMenuAssemblerTest` 4 项、IAM provider 全量 190 项 | 待人工：A19／F01 真实 bootstrap 菜单与一级目录 |
| C13 | `iam_member_export` 共享任务；快照遍历全部授权页只存成员 ID；下载重验 ACTION/范围/字段并与快照求交；失败/过期不报成功；24h 清理；GET `/export/{id}/status` 返回 `ExportTask` | `MemberExportServiceTest` 5 项、IAM provider 全量 192 项 | 待人工：A10／A27 真实超过 200 条、多实例下载与过期证据 |
| C18 | 审计写入并列出 `delegation_id`/`assignment_id`/`trace_id`；操作者显示名批量投影；规则选择器批量加载 | `DiagnoseAuditServiceTest` 增 2 项、IAM provider 全量 196 项 | 待人工：A17／A20 真实审计披露与 SQL 计划证据 |
| C09 | 平台 accounts、`/v1/me` 资料改密、字典/发号/社交新路径；RPC/凭证端口只读写 `iam_account`/`iam_tenant`；登录 JWT 填 ACTION；Security 去掉超管短路 | `AccountServiceTest` 4 项、`CurrentAccountServiceTest` 2 项、`IamUserCredentialPortAdapterTest`、`test_bootstrap_seed.py` 9 项、IAM provider 全量 203 项 | 待人工：A18／A19／A23 真实镜像、RPC 与 HTTP |
| C17（清理） | 删除已替代 HTTP、旧 PMS 授权引擎、`GrantPresenceAuthorizer` 与 `sys_user`/`sys_tenant` 双读；进程内快照走 `AuthorizationEvaluator` | IAM provider 全量 187 项 | 待人工：A22／A23 真实进程、RPC 与框架接入 |
| T13 | 重发 96/161 OpenAPI；purpose/筛选/导出状态写入 schema；`/v1` 控制器映射对账；IAM 局部错误映射单测 | `test_contract.py` 7 项、IAM provider 全量 192 项 | 待人工：A23／A24／A28 真实 HTTP 与登录 |

C12 只改读取口径，不落任何派生成员表：组与人群的成员始终由部门任职实时展开，成员换部门后无需重写组即生效。为避免展开代价随授权条数放大，单次求值内对同一 ACTION 与同一应用的开通判定只查一次。

C01/C11 的两处口径按用户确认固化：角色形态在创建时定型，完整自定义角色不再升级到共享基础；委派准入抽成 `DelegationAdmission`，授权写入与角色升级走同一套持有者、期限、版本白名单、接收人与范围上限校验，避免 `RoleService` 反向依赖 `AssignmentService`。

C02/C03 的准入口径按用户确认固化：声明的委派必须由操作者本人持有；受限方一次提交只能来自同一条委派；委派缺少角色版本内某个操作的范围上限时按失败关闭拒绝；范围包含按字面比较，写入路径不展开部门树。

C07/C08 的口径按限额中断前的推荐项落地：治理授权判定为 `source=INITIALIZATION` 且 `revision_kind=SYSTEM` 且主体为旧所有者；新所有者已有同一版本的 ACTIVE 分配则复用，不插第二条；提交后失效覆盖所有者转交以及成员暂停/恢复/移出/调部门；资料 patch 在事务内行锁后带 version 条件更新并检查行数。

C04/C05 的口径：通讯录固定默认版本空 JSON 视为全组织；字段 `ceiling` 缺省表示平台不额外收紧（基线仍脱敏手机邮箱，租户规则可授予 FULL）；原值筛选必须对查询范围内可能匹配的目标完整可见，不能用查看者本人字段权限代替。

C14/C15 的口径：成员/部门列表批量返回写操作展示能力，其它资源列表本条不铺开；字段草稿预览按通讯录场景投影样例成员；诊断必须核对应用与操作归属及可选 target 对象范围，不能只看 `actionCodes`；OPEN 菜单仍受应用域、启停、开通与人群约束，无可见子页的目录不出现在 bootstrap。

C13 的口径：导出任务落共享库而不是本机临时文件；快照只保存成员 ID，下载时按当前身份重投影，不能用过期文件绕过字段或范围；进行中返回 503 可重试，失败与过期按对象不存在处理。客户端通过 GET 状态轮询同一共享行，不得从下载 503/404 反推成功。完整结果走导出下载信封，不放宽普通列表 200 上限。

C18 的口径：审计关联在写入时填委派/授权 ID 与 MDC 追踪，列表不再写死空值；操作者显示名受成员读范围和字段策略约束。规则选择器一次批量加载，审计列表继续 SQL 分页/count。

C16 的两条硬约束在实现中固化：种子不含任何账号与凭证，初始口令只由框架用例生成并在启动日志出现一次；种子按自然键存在即跳过，重复执行不覆盖人工或业务修改。`seed-manual-verification.sql` 不再重建目录，避免与正式冷启动形成两份来源。

C09 的口径：新入口读写 `iam_account`，不返回组织关系；`MEMBER_CREATE` lookup 只给 id 与登录名；创建返回版本信封，明文初始密码只在重置密码；锁定/改密/启停复用框架用例，凭证端口不再写 `sys_user`；登录 JWT 在已建立成员上下文后填入 ACTION，选择阶段不授予；Security 管理面去掉超管短路。旧账号/字典/发号/社交 HTTP 已随 T18 删除。

C17 清理口径：写入门闩与进程内快照一律走 `@Primary` 的 `AuthorizationEvaluator`；社交绑定 `user_id` 指向 `iam_account`，不再回读 `sys_user`；字典/发号/社交 *服务*、内部 RPC、OSS、`SysUser`/`SysTenant` Feign 外形及迁移分析器保留。旧目录表的 MyBatis 域服务若无 HTTP 入口则不在本条继续清表。

## 4. 完成与证据规则

- 每项同时核对真实入口、调用链、SQL/框架归属及失败行为；不以“132 控制器已接入”替代验收。
- 有成熟用例的保留功能只写适配差异及针对性回归，不重做全套安全功能。新增、修改公共 DTO 前先同步 API 与前端说明。
- 契约结构验证、单元测试、MySQL 事务/范围测试、真实 HTTP、跨服务/多实例各有独立证据，不互相替代。未执行标记待验证。
- 迁移工具与文档退出本次范围，保留历史；后端完成以本轮 A 系列为准，产品启用还需 F 系列。current 只在验收后更新。
