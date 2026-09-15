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

## 4. 完成与证据规则

- 每项同时核对真实入口、调用链、SQL/框架归属及失败行为；不以“132 控制器已接入”替代验收。
- 有成熟用例的保留功能只写适配差异及针对性回归，不重做全套安全功能。新增、修改公共 DTO 前先同步 API 与前端说明。
- 契约结构验证、单元测试、MySQL 事务/范围测试、真实 HTTP、跨服务/多实例各有独立证据，不互相替代。未执行标记待验证。
- 迁移工具与文档退出本次范围，保留历史；后端完成以本轮 A 系列为准，产品启用还需 F 系列。current 只在验收后更新。
