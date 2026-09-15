# Tasks

> 状态：2026-09-15 后端管理面、求值/范围、字段策略与导出下载已接入，仍为 implementing。T01/T03 已勾选。MyBatis Plus 迁移 MP01–MP06 已完成：生产 `src/main/java` 不再使用 `NamedParameterJdbcTemplate`；目录/角色/组/分配/委派/求值/策略/诊断均走 Mapper/Repository。勾选仅表示满足任务全部验收条件。人工项见 [MANUAL-VERIFICATION.md](./MANUAL-VERIFICATION.md)。未创建前端代码。

## 执行顺序补充（2026-09-14）

用户要求先整体完成后端研发，再集中执行分布式测试；研发中保留必要编译检查，测试用例随代码编写但不按小批次反复启动测试环境。完成研发后统一执行自动化回归、数据库/多实例/RPC/缓存失效联调，再交付需人工执行的测试文档，包含环境、数据、身份、步骤、预期与证据。此顺序不取消验收门禁；未运行的新用例不能记录为通过。实际快照和前端联调仍保留独立外部依赖。

## 阶段 1：契约与预检

- [x] T01 校对资源操作目录、公共 DTO/OpenAPI、DDL 及迁移规则
  - 进度：已完成契约验收。目标结构 55 张表；163 个旧入口均有处置；公共类型覆盖管理命令、发布/升级、预览结果。应用增加 baseline 标记。导出 schema 组件与示例。目标 OpenAPI 覆盖管理面 77 条路径、132 个操作，控制器已接入后 `x-runtime-implemented=true`。
  - 剩余：运行时范围/字段与联调属 T10–T18，不纳入本任务验收。
  - 依赖：change 获得批准；依据 DESIGN/API，不恢复旧接口。
  - 验收：每个既有受保护入口有域/资源/ACTION/范围执行映射；类型和错误完整；已定义业务语义无实现者自行选择项。
- [ ] T02 实现源 schema 预检、逐表清单及权限映射报告
  - 依赖：T01 的源表、目标结构及迁移规则核对（已完成）；具体授权等价验证依赖后续引擎。
  - 进度：只读脱敏清单预检及 28 张源表元数据目录仍在；`import_tool.py` 已提供 dry-run/import/verify/report，幂等映射、源指纹变化拒绝、未批准 EXPANDED 阻塞；3 项夹具通过。readyForImport/readyForCutover 仍为 false。
  - 剩余：实际快照导出、辅助表内容/凭证格式核查、完整角色范围等价及 M02/M03/M06/M08/M09 对真实库验证；元数据通过不允许切换。
  - 验收：M02/M03/M06/M08/M09 夹具；未知表及所有者歧义可见，无生产连接假定。
- [x] T03 建立前后端契约测试夹具
  - 依赖：T01。
  - 进度：`contracts/examples/` 覆盖 bootstrap/成员只读、role-delta、role-upgrade/upgrade-conflict、delegation/assignment、field-policy/field-readonly、decision-restricted/audit 等；`tools/iam/test_contract.py` 校验示例与 OpenAPI。FRONTEND.md 可独立阅读，本仓库不写页面。
  - 验收：只读、差异、升级、委派、字段和诊断请求响应示例与 OpenAPI 一致；FRONTEND 可独立读取。

## 阶段 2：IAM 基础

- [ ] T04 更名模块、包、RPC、运行配置和部署
  - 依赖：T01 的命名与兼容边界核对（已完成）；不等待其他端点 DTO。其余 T01 验收仍保留，服务发布仍须全量契约、迁移和联调通过。
  - 进度：代码更名完成；模块、包、7 个 RPC、调用方、Nacos/Gateway、CI、镜像及部署脚本已迁移。IAM 47 项测试及 Auth/Member/Security/Gateway/BFF 编译通过，bootJar 可打包。6 项命名/配置检查通过；CI 实际组装产物路径、JAR 入口及 prod Dockerfile 已验证；A18 实际镜像启动及服务联调仍待验证，不勾选全任务。
  - 部署修正：IAM_DATABASE 必须显式选择目标库，连接及会话使用 UTC；IAM CI 使用 Java 21 和实际存在的 Dockerfile 任务。未发布镜像或启动业务环境。
  - 验收：A18；Gradle 编译和相关集成通过；历史归档不重写。
- [ ] T05 实现账号/成员/部门关系与最小组织初始化
  - 依赖：T04 源码与构建命名迁移（已完成）；运行链路最终验收仍受 A18 约束。
  - 进度：JDBC 身份、最小组织初始化、成员生命周期、列表/创建/详情/资料 HTTP 已落地。登录优先 `iam_account`；社交绑定命中后按账号 ID 双读新模型，未命中回退 SysUser。平台会话 Redis 索引将空 tenantId 规范为 0。生产 Guard 走 `@Primary` 求值器并校验对象范围。
  - 验证状态：本轮 H2 身份/初始化/授权门闩/成员命令/求值范围回归已执行通过；MySQL 容器、镜像启动、真实登录未跑。
  - 剩余：A02 人工自授权演练；A18 联调。不能因 HTTP 入口存在勾选全任务。
  - 验收：A01/A02/A03/A08/A09；账号凭证与组织资料隔离，初始化原子。
- [ ] T06 实现应用资源目录、显式开通及可用人群
  - 依赖：T05。
  - 进度：应用/资源/操作/菜单/套餐与开通/人群 HTTP 已接入，持久化走 Mapper/Repository；创建应用不写开通；启停与开通/人群提交后 `markAll`。H2 覆盖创建应用无隐式开通。
  - 剩余：A18 联调；套餐开通转换的真实窗口演练。
  - 验收：A02/A19；基础治理可达，普通应用无隐式开通或操作。
- [ ] T07 实现共享版本、租户差异、自定义角色及升级
  - 依赖：T06。
  - 进度：共享/租户/自定义角色 HTTP、发布/升级/合成顺序（REMOVE→REPLACE_SCOPE→ADD）已落地；发布不自动升级授权。H2 覆盖合成冲突。
  - 剩余：A04/A16 多实例升级演练。
  - 验收：A03/A04/A05/A16；最小存储、固定版本、三方冲突及授权更新原子性。

## 阶段 3：授权执行

- [ ] T08 实现静态用户组、角色分配、受限委派
  - 依赖：T07。
  - 进度：组/分配/委派 HTTP 已接入，持久化走 Mapper/Repository；平台禁止部门选择器；撤销委派派生授权失效；组扩大不突破委派上限。
  - 剩余：A11/A12 多实例与人工窗口。
  - 验收：A08/A11/A12，组修改不会间接扩权，委派来源可回收。
- [ ] T09 实现完整授权求值和快照、失效、版本
  - 依赖：T08。
  - 进度：求值器按分配绑定范围并与委派上限求交；分层缓存 L1/L2 TTL≤30s，关闭 Resilient/LKG/地板；写路径事务后 evictAll。角色合成使用 VersionedDerivedCache，source 为远端、version 为基础+差异来源及内容指纹。H2 覆盖组展开、无开通拒绝、ALL 开通、MEMBER_DEPARTMENTS 绑定及合成缓存复用。
  - 剩余：A07/A15 多实例缓存失效联调。
  - 验收：A07/A15/A16；逐授权合成后并集，统一缓存无过期放行；使用 layered-cache skill 核对接入。
- [ ] T10 接入全部 IAM 资源与相关安全 API 的功能/范围/字段边界
  - 依赖：T09。
  - 进度：管理面 132 个操作均有控制器；成员/部门列表详情与写路径按 SQL 范围谓词执行，空范围读空写拒绝。成员资料按字段策略投影并拒绝不可编辑/脱敏占位写入；列表原值筛选在字段非 FULL 时拒绝。导出 POST 登记任务，GET 下载再次校验操作与范围。
  - 剩余：A18 联调；A10/A14 人工窗口。
  - 验收：A01/A06/A09/A10/A19/A20；数据库执行、关联归属、写两端与异步重验，不仅接 demo。

## 阶段 4：策略与交付契约

- [ ] T11 实现普通通讯录及字段策略、统一候选选择器
  - 依赖：T10。
  - 进度：通讯录/字段策略 GET/PUT/preview 与目录查询已接入；phone/email 无匹配默认 MASKED；后台列表/详情带 fieldAccess。
  - 剩余：A14 人工多策略与平台上限演练。
  - 验收：A13/A14；搜索/计数/详情/导出一致，无原值泄露。
- [ ] T12 实现预览、对象能力、诊断、审计
  - 依赖：T11。
  - 进度：bootstrap/capabilities、诊断走真实求值器、审计 JSON 读取、组织设置/所有者转交 HTTP 已接入。
  - 剩余：A17 人工诊断披露边界。
  - 验收：A16/A17；预览无副作用、诊断受权限限制、审计可靠且脱敏。
- [ ] T13 校验并发布前端可消费的接口与交互契约
  - 依赖：T03/T12。
  - 进度：OpenAPI 已按 77 路径/132 操作重生，`test_contract.py` 通过。FRONTEND 仍只交付文档。
  - 剩余：前端独立实现与 F 系列。
  - 验收：API/OpenAPI/FRONTEND 一致，平台与租户各不超过四个一级目录；F 系列有测试输入。只交付文档，不在相邻仓库编写页面。

## 阶段 5：迁移与验证

- [ ] T14 实现幂等导入、旧新映射、授权差异与校验工具
  - 依赖：T02/T12。
  - 进度：`tools/iam/migration/import_tool.py` 实现 dry-run/import/verify/report；幂等 ID 映射、源变化拒绝复用批次、未批准扩大阻塞 verify。写入目标为显式 `--target-dir` JSON，不连接业务库。
  - 剩余：真实目标库 SQL 导入、辅助表与凭证格式、M 系列对实际快照。
  - 验收：M01–M09 夹具通过；源只读、目标隔离、摘要不进日志、未知项阻塞。
- [ ] T15 执行实际快照演练及处置全部切换阻塞项
  - 依赖：T14；执行时提供实际快照、环境及处置输入。
  - 验收：M 系列实际报告、ID/状态/权限比较及受控登录证据；不以仓库 SQL 代替生产演练。
- [ ] T16 完成后端综合测试与性能验证
  - 依赖：T13/T14。
  - 验收：A 系列通过，相关模块构建、契约和数据库集成通过；只因新变更/失败扩展测试，不重复无关检查。
- [ ] T17 接收前端独立实现的联调证据并执行切换检查
  - 依赖：T13/T15/T16，以及外部前端实施完成。
  - 验收：F 系列通过；停写、最终导入、写入开放前回滚窗口可执行。未满足时不宣称可上线。

## 编码门禁

- [ ] 新增/修改 Java 类型及公共契约按 java-class-javadoc skill 与仓库规范处理。
- [ ] 业务语义枚举与编译期常量统一，无散落魔法值。
- [ ] 远端参考数据使用统一分层缓存，授权不使用过期降级；派生缓存有 source+version。
- [ ] 无用户明确提交请求不创建 commit；需要提交时使用 conventional-commits skill。

## 完成检查

- [ ] REQUIREMENTS 与 ACCEPTANCE 全部满足，前端联调未被误标成后端已交付。
- [ ] 实际接口、数据模型、迁移规则与 DESIGN/API 一致；偏离先重新确认。
- [ ] 更新 IAM current 基线，处理旧 PMS 基线及关联文档，不提前写线上事实。
- [ ] SQL 规模优化 draft 已明确如何后续重评，不删除或假装完成。
- [ ] README 记录结果、关联证据及差异，再将本 change 归档。


## 已批准追加：MyBatis Plus 与构造注入统一

- [x] MP01：实体、具名 Mapper、类型处理与真实 MyBatis 测试装配基础。
  - 进度：`persistence` 覆盖目录/角色/组/分配/委派/人群/策略选择器等缺表；新 Mapper `@InterceptorIgnore` 屏蔽旧租户与旧数据权限；身份联表 XML 使用 `#{}`；行锁用具名 `@Select`；`IamMybatisTestAccess` 装配真实 MP/MPJ 并加入 Spring 事务。
- [x] MP02：身份、账号适配、初始化、成员和组织数据库访问迁移。
  - 进度：身份查询、账号凭证/写入适配、初始化目录、组织设置、会话读取、组织初始化写路径、成员生命周期、成员列表/详情/创建/资料、部门树与成员导出已迁到 Mapper/Repository。范围改为类型化 `ObjectScope`，Service 不再拼接 SQL。
- [x] MP03：目录、角色、组、分配、委派数据库访问迁移。
  - 进度：`CatalogService`/`EntitlementService`/`RoleService`/`GroupService`/`AssignmentService`/`DelegationService` 注入 Repository；普通 CRUD 用 Wrapper，复杂 JOIN/EXISTS/`CURRENT_TIMESTAMP`/`FOR UPDATE` 用具名 `@Select`。已去掉 `Jdbc*` 前缀。
- [x] MP04：授权求值、类型化范围、策略、审计、会话和导出迁移。
  - 进度：会话组装、审计写入、类型化范围、字段求值、直接分配门闩与导出已用 Mapper；`AuthorizationEvaluator`、`PolicyService` 与 `DiagnoseAuditService` 走 Mapper/Repository。`AuthorizationView` 与 `@Primary` 未改。
- [x] MP05：本次范围构造注入统一；新增 skill 与 AGENTS 门禁。
  - 进度：skill 与 AGENTS 门禁已落地；新 Repository 使用 `@RequiredArgsConstructor`；`TransactionTemplate` 与带 `@Qualifier` 的求值器缓存入口保留显式构造器。
- [x] MP06：全部生产直接 JDBC 清零、编译及集中回归；更新人工验证文档。
  - 进度：`ingot-iam-provider` 生产 `src/main/java` 已无 `NamedParameterJdbcTemplate`/`JdbcTemplate`/`DataSource` 数据访问。测试夹具仍可用 JDBC 准备数据。provider 全量 H2 单测通过。未跑 MySQL 容器或 A 系列人工项。

上述任务不能以创建实体或移动 SQL 代替完整调用链迁移，不能新增通用 SQL 字符串执行器。普通 CRUD 用 Lambda，必要复杂查询用 XML；保持原 API/DDL/业务边界，不自动修复不相关业务问题。
