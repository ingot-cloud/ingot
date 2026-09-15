# 实施核对记录

> 2026-09-15 复评说明：本文既有段落是历史实施证据，不是当前完成判定。业务缺陷、重复框架持久化、平台登录与全新系统范围已在 REMEDIATION/TASKS 重开；本轮仅修改 Spec，没有修复代码或补跑测试。

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

T01/T03 已勾选。T04 缺 A18 镜像启动；T05 缺 A02/A08 完整引擎及人工联调。其余任务按 TASKS 跟踪。人工项见 MANUAL-VERIFICATION.md。D01 已确认，不再因平台主体模型暂停实施。

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


## 继续实施：主要响应契约与应用序列化

- 新增 bootstrap/capabilities、成员/组织/目录/组/角色/授权等响应类型，以及 ResourceDetail、PageResponse、Preview 泛型，升级冲突、受限诊断与安全审计结构。
- 使用应用 InModule 验证 ID/版本仍为字符串，统计字段通过专用 IamCountSerializer 输出数字；测试发现 NumberSerializer 无默认构造器，已修正。分页每条记录保留字段访问和能力结果。
- 新增 7 份响应夹具，验证隐藏字段省略、脱敏值保留、受限影响统计省略、稳定冲突码和审计差异键白名单。未以 DTO 测试冒充运行时脱敏或鉴权验证。
- 导出 62 个 components（含具体泛型及 R 信封），12 份夹具；29 项 Java 契约测试通过，git diff --check 通过。本批未修改 DDL，未重复运行数据库测试；上批 18 项 MySQL 约束验证仍为对应结构证据。
- T01 仍缺策略配置详情、具体预览结果、完整请求校验与端点 OpenAPI；T02–T17 未标完成。没有新增待用户确认项，未修改 current 或提交 Git。


## 继续实施：策略请求、版本命令与阶段 OpenAPI

- 新增 21 个类型，覆盖通讯录/字段策略草稿与整体替换、类型化预览、诊断身份、成员/配置状态、组/人群、授权/委派更新、所有者转交及分配预览结果。校验默认范围选择器、草稿种类唯一性、诊断身份二选一、状态禁止直接移出；实际归属和权限由后续服务负责。
- 新增 3 份策略 JSON 夹具；37 项 Java 契约测试全部通过，导出 93 个模型组件及 15 份示例。
- `tools/iam/build_contract.py` 从 Java 模型快照与显式 routes 清单生成阶段性 OpenAPI：9 条路径、11 个操作。包括 R 信封、稳定错误、固定域及执行要求；运行时标记均为 false。
- `tools/iam/test_contract.py` 的 5 项检查通过：引用和悬空检测、可重复生成、固定域路径、必填版本/无客户端 allowed、具体响应及错误信封。这是结构检查，不冒充完整 OpenAPI 规范或控制器集成验证。
- 本轮无 DDL 变化，无实际数据库连接或 Git 提交。T01 仍需其他管理请求、角色发布/升级及全量端点契约；没有新增待确认项，保持 implementing。


## 持续实施：细化独立的命名迁移依赖

T04 的实际输入是 DESIGN 第 1 节已批准的模块、包、RPC、服务注册和网关命名，不依赖尚待补齐的其他资源 DTO。TASKS 据此细化 T04 前置项，以推进构建与调用链迁移；T01 保持未完成，不能因更名编译通过宣称新授权运行时或全量契约完成。此调整仅改变实施顺序，不改变身份模型、接口兼容策略或发布验收门禁。


## 持续实施结果：T04 更名与 T05 身份持久化基础

- 迁移 ingot-pms / api / provider 到 ingot-iam，Java 包 com.ingot.cloud.iam，7 个 RemoteIam RPC、服务常量、调用方与配置同步变更。旧目录只可能保留被忽略的本机 build/bin 产物，构建不再引用它们；未改 Git 暂存区。
- Gateway 内部 /iam/** 对应前端代理剥离 /api 后的外部 /api/iam。Nacos 三环境、Compose/Swarm、env、CI、镜像入口一起迁移；修正 run-iam 的错误 MEMBER_VERSION 引用。IAM CI 改为 Java 21 及存在的 shiftDockerfileProd 任务，通用 Dockerfile 改为 Java 21 并正确展开 JAVA_OPTS。
- IAM_DATABASE 必须显式提供独立目标库，不使用旧源库默认名；连接与数据库会话强制 UTC。Compose 两种模式均只执行 config --quiet 检查，通过，未运行部署命令。
- 新增 ActiveIdentity / JdbcIdentityRepository / ActiveIdentityService，直接查询新模型并失败关闭。10 项 JDBC 测试覆盖同账号三身份、跨域/跨租户/跨账号拒绝、单成员暂停或移出、全局禁用/软删除、租户停用、实时版本和数据库故障。
- 新增 TenantInitializationPlan / JdbcTenantInitializer，最小初始化与安全审计同事务；6 项 JDBC 测试验证固定引用、不复制目录、无凭证写入、审计失败整体回滚、重复创建、错误治理版本/默认类型/基础应用、停用账号拒绝。
- IAM provider 全部 47 项测试通过（含 16 项新身份/初始化测试），commons 37 项契约测试通过；Auth/Member/Security/Gateway/BFF 编译通过，IAM bootJar 打包通过。命名/配置 5 项检查通过。隔离 MySQL 20 项测试通过，其中新增 2 项直接执行 Java 身份查询文本；H2 初始化事务测试不冒充 MySQL 全流程演练。
- T04 的实际镜像启动和服务间运行验证仍待完成；T05 的认证、控制器、生命周期和服务器初始化计划生成尚未接入。旧业务服务仍有后续替换工作，不能把可打包视为可切换。current 与 archive 未修改，未创建 commit、未发布镜像、未连接或迁移现有业务数据库。


## 持续实施：T02 脱敏元数据预检

- 加入 `tools/iam/migration/preflight.py`，只读 JSON 元数据，不连接数据库、不执行 SQL。结构目录来自 28 张仓库源表 CREATE TABLE，不复制 INSERT 数据。
- 校验未知/缺失表、列漂移、事实数量、账号凭证核查标志、成员关系、所有者显式映射、部门孤儿/跨租户/循环、旧权限到精确 ACTION 的映射及独立平台成员映射。部门图采用线性遍历，万节点链不依赖递归。
- 13 项合成夹具测试通过；命令行验证生成 PREFLIGHTED 报告，readyForImport/readyForCutover 始终 false。拒绝凭证额外字段、覆盖源输入和变更清单复用已有报告；报告不回显自由文本理由或原权限码。
- 实际快照导出、凭证格式核查、辅助表内容、范围等价及导入尚未实现，不将 T02 或 M 系列标记完成。源表与迁移规则已稳定，工具开发与其余接口契约并行推进；未连接实际业务库。


## 2026-09-14 CI 产物链路核对

实际执行 ingotAssemble 与 shiftDockerfileProd，仅生成本地文件，确认产物目录是 output/ingot-iam-provider（不含旧模块名或版本目录），其中 JAR 入口为 com.ingot.cloud.iam.InIamApplication，Dockerfile 与 prod 模板一致。

修正 IAM CI 对 Auth 模块/版本的遗留复制引用，以及旧 registry/image 名称与实际 ingot/iam 不一致的问题。IAM assemble 通过 artifacts 交付当前流水线产物，docker-build 显式 needs assemble 并使用已验证目录，deploy 依赖本流水线的 IAM docker-build；IAM 作业禁用旧 output 缓存，避免手工跳过构建而使用旧产物。现有其他服务流水线不在本次 IAM 修复范围内。命名/部署配置检查增至 6 项并通过。未执行 docker build/push/run 或业务环境发布。


## 2026-09-14 研发顺序调整与身份执行基础

用户明确要求整体研发完成后再集中执行分布式测试。研发阶段编写回归用例并编译，不按每个小步骤反复启动容器或做多服务测试。集中验证后再交付人工测试文档；当前没有生成伪装成已验收结果的测试报告。

本轮新增：
- 显式域成员选择与当前安全上下文恢复，拒绝旧会话补造成员和 Member 体系混入 IAM。
- 认证 DTO → InUser → OnlineToken → OAuth2 JSON/资源服务器的成员身份传递；账号/租户绑定校验、不可变部门集合、同 sid 身份保持及旧快照隔离。
- 成员状态、移出和任职替换的真实 JDBC 事务；全关系/两端授权回调、成员版本、所有者保护与同事务审计。
- 新回归源代码覆盖上述绑定、域隔离、事务回滚和部门关系。只记录编译检查，不宣称未执行用例通过。

最终编译检查通过：IAM provider、security-common、security-authorization-server 的 compileTestJava，以及 Auth provider 的 compileJava。新增 27 项回归用例仅编译、尚未执行；没有运行 test 任务或启动测试容器。完整认证加载、安全账号适配、平台会话索引、真实 ACTION/范围引擎、HTTP 及其余 TASKS 尚未完成；没有勾选全任务或发布服务。


## 2026-09-14 管理命令契约、目标 OpenAPI 与目录初始化计划

补齐成员/组织/部门/应用/资源/操作/菜单/套餐/开通及角色创建、发布、升级命令；升级冲突必须显式处置。应用目录增加 baseline，仅租户域可标记。目标 OpenAPI 覆盖管理面 76 条路径、131 个操作，全部 x-runtime-implemented=false。JdbcInitializationCatalog 从唯一租户系统角色、最新默认策略和 baseline/套餐应用生成 TenantInitializationPlan，HTTP 仍只接收 TenantCreateInput。

验证：`./gradlew :ingot-framework:ingot-commons:test --tests '*Iam*ContractTest'` 通过；`python3 tools/iam/generate_routes.py`、`build_contract.py` 与 `test_contract.py` 通过（76 路径 / 131 操作）；`JdbcInitializationCatalogTest` 与 `JdbcTenantInitializerTest` 的 H2 用例通过。未运行 MySQL 容器、未启动业务环境、未切换登录加载器或 HTTP。登录加载器、安全账号端口和受保护控制器仍未切换。


## 2026-09-14 新模型登录、ACTION 门闩与受保护 HTTP

登录优先查询 `iam_account`，按请求 tenant（空=平台）选择单一成员，不合并旧授权快照；账号表不可用或未命中时回退 SysUser。账号安全端口与 `/inner/user/{id}` 同样双读。生产 Guard 查询当前成员直接分配中的精确 ACTION，无空实现；不计算部门范围。

已接入 HTTP：`POST /v1/platform/tenants`、`POST /v1/platform/tenants/preview`、平台/租户 `members/{id}/status|remove`、租户 `members/{id}/departments`。OpenAPI 上述 7 个操作 `x-runtime-implemented=true`。H2 身份/组织/授权相关测试本轮执行通过。T01/T03 已勾选；T04/T05 因 A18 与 A02/A08 未勾选。人工清单见 MANUAL-VERIFICATION.md。未改 current、未 commit、未启动业务环境。

## 2026-09-14 字段策略执行、导出重验与角色合成派生缓存

成员列表/详情按 MANAGEMENT 字段策略投影，phone/email 无匹配默认 MASKED；提交不可编辑字段或脱敏占位 `***` 拒绝，不忽略。列表按手机号/邮箱原值筛选在字段非 FULL 时拒绝。`POST /v1/tenant/members/export` 登记任务并写已投影快照；`GET /v1/tenant/members/export/{id}` 再次校验导出 ACTION、对象范围与当前字段策略，任务文件不能绕过权限。角色合成接入 `VersionedDerivedCache`，version 指纹含基础版本、差异版本及授权内容，授权变更事件后全量丢弃。

OpenAPI 现为 77 路径 / 132 操作。H2：`FieldAccessEvaluatorTest`、`RoleSynthesisCacheTest` 及既有 iam-provider 测试本轮通过；`test_contract.py` 通过。未跑 MySQL 容器、镜像启动、多实例缓存失效或人工 A 系列。T14 仍只写 `--target-dir` JSON。未改 current、未 commit、未改 ingot-admin。

## 2026-09-14 MyBatis Plus 持久化迁移进度

已批准的 MP 调整仍为 implementing。当前落点：

- MP01 完成：`persistence` 覆盖目录/角色/组/分配/委派/人群/策略选择器等缺表；新 Mapper `@InterceptorIgnore` 屏蔽旧租户与旧数据权限；身份联表 XML 使用 `#{}`；行锁用具名 `@Select`；测试夹具 `IamMybatisTestAccess` 装配真实 MP/MPJ 并加入 Spring 事务。
- MP02 完成：身份 Repository、账号凭证/写入、初始化目录、组织设置、会话读取、`TenantInitializer`、`MemberLifecycle`、成员列表/详情/创建/资料、部门树与成员导出已去掉 JDBC。范围改为类型化 `ObjectScope`，Repository 用 Wrapper/`EXISTS` 绑定可信 tenantId，Service 不再拼接列名。
- MP03 完成：目录、开通、角色、组、分配、委派服务内部改为 Repository；业务类已去掉 `Jdbc*` 前缀。
- MP04 完成：审计 Writer、会话 Repository、类型化范围、字段求值、直接分配门闩、导出、求值引擎、策略写路径与诊断审计均走 Mapper/Repository。
- MP05 完成：skill 与 AGENTS 门禁已有；新 Repository 使用构造注入；`TransactionTemplate`/`@Qualifier` 保留显式构造器。
- MP06 完成：生产代码已无 `NamedParameterJdbcTemplate`；provider 全量 H2 单测通过。未跑 MySQL 容器或 A 系列。

新 IAM Mapper **不使用** `InTenantLineHandler`。旧拦截器继续服务其他模块；新 Mapper 显式 `@InterceptorIgnore`，由认证后的 `AuthorizationContext` 提供域与租户，Repository 写进条件。平台域 `tenantId` 为空，不能靠线程上下文或 header 隔离。

本轮验证：`./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --tests 'com.ingot.cloud.iam.identity.*' --tests 'com.ingot.cloud.iam.organization.*' --tests 'com.ingot.cloud.iam.policy.FieldAccessEvaluatorTest' --tests 'com.ingot.cloud.iam.authorization.JdbcGrantPresenceAuthorizerTest'` 通过；provider compileJava/compileTestJava 通过。未跑 MySQL 容器或全量回归，未改 current、未 commit。

## 2026-09-15 生产 JDBC 清零

用户要求不再保留 `NamedParameterJdbcTemplate` 逻辑，把剩余生产路径一次迁完。随后按职责去掉业务类的 `Jdbc*` 前缀，HTTP 路径不变。

新增/补齐实体与 Mapper：`iam_resource`、人群三表、角色参数/差异、平台/租户组及成员来源、委派额度与子表、策略选择器与通讯录规则。生成列不映射。新 Mapper 一律 `@InterceptorIgnore(tenantLine, dataPermission)`，租户条件由 Repository 绑定认证后的可信 ID。

服务迁移：

- `JdbcCatalogService` → `CatalogRepository`；`JdbcEntitlementService` → `EntitlementRepository` + `CatalogRepository`。开通有效窗口用具名 `CURRENT_TIMESTAMP` COUNT。
- `JdbcRoleService` / `JdbcGroupService` → `RoleRepository` / `GroupRepository`。目录过滤改为类型化 Wrapper，不再拼接 SQL 谓词。
- `JdbcAssignmentService` / `JdbcDelegationService` → `AssignmentRepository` / `DelegationRepository`。行锁列集与原 SQL 一致。
- `JdbcAuthorizationEvaluator` → `AuthorizationEvaluationRepository`；JOIN 具名 SQL 只选测试夹具也有的列。去掉 DataSource 测试构造器，夹具走 `IamMybatisTestAccess.evaluator`。`AuthorizationView` 与 `@Primary` 未改。
- `JdbcPolicyService` → `PolicyWriteRepository`，继续注入 `DepartmentClosure`。
- `JdbcDiagnoseAuditService` 审计列表用 Wrapper `.select(...)`，避免夹具缺列。

插入不写 `version`（`insert-strategy: not_null`）；条件更新后 `version+1`，不用旧 MP `@Version`。未新增通用 SQL 执行器。

验证：`./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test` 通过；生产 `src/main/java` 检索无 `NamedParameterJdbcTemplate`/`JdbcTemplate`。测试仍可用 JDBC 准备夹具。未跑 MySQL 容器、镜像启动或多实例缓存失效。未改 `specs/current/`、未 commit。

## 2026-09-14 CompositeEnumTypeHandler 递归启动失败

Nacos `in-database.yml` 的 `default-enum-type-handler` 为 `CompositeEnumTypeHandler`。该处理器对无 `IEnum`/`@EnumValue` 的枚举会回退到「默认枚举处理器」，而默认就是它自己，Mapper 初始化时无限递归。旧业务枚举已有 `@EnumValue`；新 IAM commons 枚举按 DDL 存枚举名 VARCHAR，原先是纯 Java enum。

IAM commons 枚举已对齐 `CommonStatusEnum`：`@Getter`/`@RequiredArgsConstructor`，稳定字面量同时标 `@JsonValue`/`@EnumValue`，`@JsonCreator getEnum` 走 `EnumUtils` 索引。名称即值的枚举构造参数写与常量名相同的字符串；`IamAction`/`IamReasonCode`/`MemberFieldKey` 保留独立编码字段。`IamAction` JSON 输出契约码（如 `iam-tenant:directory:read`）而非枚举名。未改 Nacos 全局处理器。规范见 `.agents/skills/java-enum-contract`。

验证：`./gradlew :ingot-framework:ingot-commons:test --tests '*IamEnumPersistenceContractTest' --tests '*Iam*ContractTest' --tests '*EnumUtilsTest' :ingot-service:ingot-iam:ingot-iam-provider:test --tests 'com.ingot.cloud.iam.identity.*' --tests 'com.ingot.cloud.iam.organization.*'` 通过。未启动业务环境。

## 2026-09-15 去掉 Jdbc 前缀

生产访问已不再走 JDBC，业务类按职责去掉 `Jdbc*` 前缀，不再用存储实现当类型名。HTTP 控制器只改注入类型，路径不变。`AuthorizationView` 仍为求值器嵌套类型。测试夹具继续使用 Spring `JdbcTemplate` 准备数据，不改名。

服务与求值：`JdbcCatalogService` → `CatalogService`，`JdbcEntitlementService` → `EntitlementService`，`JdbcRoleService` → `RoleService`，`JdbcGroupService` → `GroupService`，`JdbcAssignmentService` → `AssignmentService`，`JdbcDelegationService` → `DelegationService`，`JdbcPolicyService` → `PolicyService`，`JdbcDiagnoseAuditService` → `DiagnoseAuditService`，`JdbcAuthorizationEvaluator` → `AuthorizationEvaluator`。

身份与组织：`JdbcIdentityRepository` → `IdentityRepository`，`JdbcAccountCredentialRepository` → `AccountCredentialRepository`，`JdbcAccountIdentityService` → `AccountIdentityService`，`JdbcInitializationCatalog` → `InitializationCatalog`，`JdbcTenantInitializer` → `TenantInitializer`，`JdbcMemberLifecycle` → `MemberLifecycle`，`JdbcTenantQueryService` → `TenantQueryService`，`JdbcSessionService` → `SessionService`，`JdbcGrantPresenceAuthorizer` → `GrantPresenceAuthorizer`。

对应测试同步改名。验证：`./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test` 通过；生产 `src/main/java` 无 `Jdbc*` 业务类。未改 `specs/current/`、未 commit。

## 2026-09-15 后端复评与范围修订（仅文档）

依据用户明确要求，移出旧库升级/迁移门禁，保留必要既有功能，限定角色权限及关联接入；已核对安全框架存在 LockStatePort、DefaultLockStatePortAdapter 和 AccountLockStateMapper，IAM 的同表 Entity/Mapper 属重复实现，应在后续实施中移除并复用既有端口。平台登录不再返回允许访问租户，前端不再强制平台用户选择组织。

前轮静态评估确认角色升级归属、委派持续约束、通讯录/字段规则、期限缓存、并发写、所有者转交、旧能力迁入、HTTP 错误、角色形态、组展开、导出、诊断/预览/对象能力和菜单过滤等缺口。REMEDIATION 记录修正目标，TASKS 和 ACCEPTANCE 重新建立门禁；原 T01/T03 的局部结构通过不再表示扩展后的完整契约完成。MP 历史机械迁移检查不证明功能正确或框架职责合理。

本轮未改 Java/SQL/配置、前端、生成的契约 JSON、current 或 Git 提交；未执行运行时测试。
