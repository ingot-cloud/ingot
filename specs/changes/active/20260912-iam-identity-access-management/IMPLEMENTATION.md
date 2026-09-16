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

## 2026-09-15 基础隔离层修正（C17、C10、C19）

按依赖顺序先做与其他条目无耦合的基础层，为后续授权语义修正让出干净的地基。

C17 锁定态：删除 IAM 侧同表 `IamAccountLockState` 实体与 Mapper，改为消费安全框架的 `LockStatePort`，失败计数、自动解锁与密码事件仍由框架用例负责，IAM 不再复制实现。`databases/iam/005_auxiliary.sql` 同步移出 `account_lock_state`，并在 `databases/iam/README.md` 明确 `account_lock_state`、`password_history`、`password_expiration` 的权威 DDL 与持久化归属都在框架，表可以与 IAM 同库部署但代码所有权不转移。

C10 错误映射：新增 `web/IamErrorHandler`，把 IAM 内部 `BizException` 按 `IamReasonCode` 映射到 400/401/403/404/409/503，只作用于 IAM 控制器包，不改其他服务经全局处理器的错误协议。

C19 平台登录：`UserDetailsRequest` 增加显式 `AuthorizationDomain`，不再用 `tenant == null` 隐式判定域。Auth 按登录入口填域，IAM 按域分流身份解析；平台分支 `tenantAllows` 返回空数组且不返回可选租户，租户与 Member 分支的原认证规则不变。

## 2026-09-15 C16 正式冷启动

冷启动按“无凭证目录走 SQL、账号走框架用例”两段落地，两段都可重复执行。

目录段：新增 `tools/iam/generate_bootstrap.py`，从 change 的 `contracts/routes.json` 生成 `databases/iam/006_bootstrap.sql`，菜单树按 `FRONTEND.md` 的导航映射表产出。种子含 2 个治理应用、30 个资源、107 个精确 ACTION、24 个菜单及菜单-操作关联、每域一个 SYSTEM 治理角色及其固定版本与 107 条授权、2 个默认策略版本、发号标签 `iam` 的高水位 1000000。幂等实现为 `INSERT ... SELECT ... WHERE NOT EXISTS`，父行一律按自然键解析而非固定标识，因此库里已存在同 code 的应用时子行会挂到既有标识上而不是撞外键；保留标识全部小于发号起点。种子不含任何账号或凭证。

账号段：`ingot.iam.bootstrap.enabled` 默认关闭，`IamBootstrapConfiguration` 只在开启时装配 `PlatformBootstrapService` 与 `ApplicationRunner`，关闭时不实例化任何冷启动 Bean。启动器复用安全框架的 `RegisterUserUseCase`（来源 `ADMIN_CREATE`）与 `InitialPasswordService`，口令策略、首登强制改密、有效期与密码历史都沿用框架语义，源码与 SQL 中没有硬编码口令，初始口令只在启动日志 WARN 打印一次。登录名与显示名由 `ingot.iam.bootstrap.username`（默认 `platform`）、`.display-name`（默认「平台治理」）配置，phone/email 可选。已存在平台成员时整体跳过；治理角色缺失或不唯一时明确失败，不猜测目标版本。

配套改动：`AccountWriteRepository` 增加 `insert`，`IamUserAccountPortAdapter.save()` 改写 `iam_account` 而不是 `sys_user`（属 C09/A23 前置，经用户确认本轮一并做）；`InitializationCatalogRepository.tenantSystemRoles()` 泛化为 `systemRoles(AuthorizationDomain)` 以便平台域复用同一唯一性判定。

验证：`python3 databases/iam/test_bootstrap_seed.py` 9 项通过（完整性、幂等且保留人工改动、组织初始化前置条件、治理授权只覆盖本域、保留标识边界、菜单可达性与注册键、既有应用标识挂接、人工种子叠加）；`test_identity_schema.py` 仍只加载 001–005 结构文件，把种子排除在外。`PlatformBootstrapServiceTest` 6 项通过，IAM provider 全量 111 项通过。`databases/iam/seed-manual-verification.sql` 收敛为只补两个可登录账号、一个平台成员和一条按自然键解析的治理授权，目录与治理角色不再有第二份来源。

未做：A21 需要真实进程与登录证据，已在 `MANUAL-VERIFICATION.md` 补 A21.1／A21.2 两项待人工执行；未改 `specs/current/`，未 commit。

## 2026-09-15 C06 快照期限与写路径缓存边界

按 DESIGN 第 185 条落地，`expiresAt` 不再是固定的现在加 30 秒。

期限来源：授权求值 SQL 增投影 `ra.valid_until` 与来源委派的 `valid_until`，开通查询从只数行改为同时返回命中数与最近开通截止（存在不限期开通时截止为空，避免 `MIN` 把不限期当成边界）。求值器把真正贡献了操作的分配、其来源委派与命中开通的截止收敛成最近边界，`expiresAt` 取热窗口 30 秒与该边界的较早者。数据库会话为 UTC，边界按 UTC 解释。

读取校验：`evaluate` 每次命中都检查 `expiresAt`，过期先 `evict` 再重载，L2 回填 L1 不为过期视图续命。`SessionService` 的 bootstrap/capabilities 与诊断响应直接透出截断后的 `expiresAt`，前端按真实边界刷新。

写路径：commons 新增 `IamActionOperation`，把 ACTION 码末段的操作语义（read/preview/diagnose/export 只读，create/update/delete/status/publish/remove/upgrade/owner-transfer/departments 改写）显式登记，`IamAction` 在类加载时派生该语义，出现未登记词汇即启动失败。求值器对改写操作直接查库求值，不接受任何热缓存快照放行；准入与范围由同一份快照给出，避免混合版本。

验证：`AuthorizationEvaluatorTest` 新增 7 项（无边界用满热窗口、分配/委派/开通三类边界各自截断、不限期开通不被 `MIN` 误判、过期命中不放行且重载、写操作绕过陈旧快照而读操作仍可命中），IAM provider 全量 118 项通过。夹具用 `INIT=SET TIME ZONE 'UTC'` 让 H2 会话与生产一致，不依赖运行机器时区。

未做：A15/A16 的多实例广播失败、远端故障与预览后配置变化仍需真实环境证据；未改 `specs/current/`，未 commit。

## 2026-09-15 C12 组与人群按部门任职展开

原实现只认显式成员行：组分配要求 `iam_tenant_group_member` 有本人，人群开通的部门规则按精确部门匹配且忽略 `include_descendants`，组成员数直接数显式成员表。结果是「按部门授予」的组和人群对下级部门成员不生效，成员换部门也不会改变授权。

统一口径：新增 `IamMembershipSql`，把成员可达部门（本人任职部门沿 `parent_id` 向上闭包，并区分是否为直接任职）与由此推导的组集合固化成一段递归 CTE，供组分配与人群开通共用。部门规则只在直接任职或 `include_descendants` 为真时命中，因此「仅本部门」与「连带下级」两种配置语义不同。组成员数改用另一段递归 CTE 沿部门树向下展开，与显式成员并集后去重。三处落点：`IamRoleAssignmentMapper.listTenantGroup`、`IamTenantAppEntitlementMapper.entitlementForMember`、`IamTenantGroupMemberMapper.countMembership`（经 `GroupRepository.countTenantMembership` 供 `GroupService.detail`）。

不落派生表：成员归属始终实时展开，成员换部门后组与人群随之变化，无需重写组或重新开通。为避免展开代价随授权条数放大，单次求值内同一 ACTION 的目录行与同一应用的开通判定各只查一次。

验证：`AuthorizationEvaluatorTest` 新增 5 项（祖先部门规则仅在连带下级时命中、成员换部门后组分配随之失效、人群部门规则同样区分连带下级、人群按组命中时组本身也可由部门推导、跨租户不串），`GroupRepositoryTest` 新增 5 项（仅显式成员、连带下级开关、两种来源去重、任职变化不重写组即生效、跨租户不计）。IAM provider 全量 129 项通过。MySQL 8.4 容器内用同一组 DDL 手工核对两段 CTE，结果与 H2 一致（不连带 0 组、连带 1 组、展开去重后成员数 2），排除方言差异。

未做：A08/A11 的运行时证据仍待人工；委派接收人群的同口径展开与运行时持续校验属 C02/C03，随该条目一并处理；`GrantPresenceAuthorizer` 仍是只认直接分配的写入门闩，按设计不参与完整求值，随 T18 清理。未改 `specs/current/`，未 commit。

## 2026-09-15 C02/C03 治理权与受限委派分离准入

原实现只在写入时看一眼委派：分配落库后，即使委派被移出角色版本白名单、接收人名单不再包含该成员，求值仍继续放行；受限管理员与完整治理者走同一条准入，凭空创建授权也能通过。

运行时持续校验：`IamMembershipSql` 增加 `AND_REVISION_STILL_ALLOWED`、`AND_TENANT_RECIPIENT_REACHED` 与 `AND_PLATFORM_RECIPIENT_REACHED` 三段条件，直接并入 `IamRoleAssignmentMapper` 的四条求值查询（平台/租户 × 直接/按组）。委派派生的分配每次求值都要同时满足委派处于 ACTIVE、在有效期内、当前角色版本仍在白名单、接收成员仍被名单或接收部门覆盖；任一维度失配即当次不产出操作，不依赖任何撤销后台任务。接收部门与 C12 同口径：本人直接任职总是命中，祖先部门只在该规则连带下级时命中，先前忽略 `include_descendants` 的写法一并修正。

治理资格：`IamActionAuthorizer.admit` 返回 `Admission(governed)`，`governed` 只在该 ACTION 能由非委派分配单独支撑时为真。求值器为此在快照里多留一份 `governedCodes`（`delegation_grant_id` 为空的分配才计入），`GrantPresenceAuthorizer` 的兜底计数同样支持 `governedOnly`。`IamAccess.admit` 把结论以 `IamAdmission` 交给服务层，`require` 保持原语义不变。

写入准入（按用户口径确定）：受限方（`governed` 为假）必须为每条分配声明来源委派，且一次提交只能使用同一条委派；无论是否具备完整治理资格，声明的委派都必须由当前操作者本人持有；委派若未给出角色版本内某个操作的范围上限，按失败关闭拒绝，避免落一条运行时形同废纸的授权；范围包含按字面比较（`ScopeBinder.covers`），显式部门/对象 ID 必须逐个出现在同一条上限条款内，连带下级只能由同样连带下级的上限覆盖，写入路径不展开部门树。

组编辑连带：`GroupService` 按待保存内容展开组的有效成员（显式成员并入部门任职，连带下级沿部门树展开），逐条核对组上每条委派派生授权的全部成员仍在该委派接收范围内，不成立即 `POLICY_CONFLICT`，预览给出同样结论。接收人判定抽到 `DelegationRecipientRepository`，与授权写入共用一份口径。

验证：`AuthorizationEvaluatorTest` 新增 5 项（版本离开白名单、成员离开接收名单、扩组不惠及名单外成员、接收部门区分连带下级、委派来源不计入治理资格），新增 `AssignmentServiceTest` 10 项（治理者免委派、受限方缺来源被拒、借用他人委派被拒、批次拼接两条委派被拒、上限内接受、超上限被拒、缺上限被拒、接收人外被拒、接收部门连带下级、预览与写入同结论且不写库），新增 `GroupServiceTest` 6 项（收缩成立、连带下级破坏派生授权、加入名单外成员被拒、空选择不承载派生授权、预览解释冲突、无派生授权时不受限）。IAM provider 全量 150 项通过。

未做：A11/A12 的真实 HTTP 与并发证据仍待人工；委派本身的创建/撤销面（`DelegationService`）不在本条目范围；未改 `specs/current/`，未 commit。

## 2026-09-15 C01/C11 角色形态定型与升级重验

原实现两处放行过宽：`TENANT_CUSTOM` 一律要求共享基础，纯自有授权的角色无法表达；`RoleService.upgrade` 按调用方给的 `assignmentId` 直接改版本，既不校验该授权是否属于本租户与当前角色，也不重验新版本下的范围参数与来源委派，且发布/预览完全不看操作是否存在、是否与角色同域、资源是否支持该数据范围。

角色形态（按用户口径固定在创建时）：首个版本没有共享基础即为完整自定义，只存 `grants`、不接受 `deltas`，且此后不能再升级到共享基础；首个版本带基础的角色只存 `deltas`，后续发布沿用当前基础。形态由 `currentBase` 从最新版本读出，`publishRevision` 与预览都据此校验提交形状，形状不符按 `INVALID_ARGUMENT` 拒绝。

强校验（失败关闭）：新增 `RoleGrantValidator`，对合成后的每条授权核对操作存在且启用、所属应用与资源启用、应用授权域与角色管理域一致、范围种类在资源 `scope_capabilities` 内；`MANAGED_DEPARTMENTS`／`OBJECT_SET` 必须声明 `parameterKey`，且该参数在本版本 `parameters` 中已声明且绑定种类匹配。发布、预览、升级共用同一校验，预览按原样返回各自的 reason code 而不再统一压成 `POLICY_CONFLICT`。

升级（C01）：`upgrade` 只受理有基础的角色，逐条 `SELECT ... FOR UPDATE` 锁定选中授权，核对其属于当前租户、指向本角色的某个版本、处于 ACTIVE；再按新版本参数核对范围绑定无未绑定项；来源委派的授权走 `DelegationAdmission` 重验（持有者、期限、版本白名单、接收人、逐操作范围上限），最后带 `version` 条件更新，任一条不成立整批回滚。委派校验从 `AssignmentService` 私有方法抽成 `DelegationAdmission`，两侧共用，避免 `RoleService` 反向依赖授权服务。

验证：新增 `RoleServiceTest` 16 项（完整自定义只存 grants、带基础只存 deltas、两种形状交叉提交被拒、共享基础必须是 SHARED 版本、跨域操作被拒、停用与不存在的操作被拒、超出资源能力的范围被拒、管理范围缺参数声明被拒、再次发布沿用原形态、预览按真实 reason code 报错且不写库、升级只动选中授权、越租户/越角色/非活跃被拒、委派不允许新版本被拒而允许时通过、新版本需要未绑定参数被拒、完整自定义角色不可升级）。IAM provider 全量 166 项通过。另对 `revisionBelongsToRole` 做了一次变异核对，确认越角色升级的用例确实由该条件拦下而非偶然通过。

未做：A05／A16／A25 的真实并发升级与原子回滚证据仍待人工；角色删除/停用面与共享角色的跨租户可见性不在本条目范围；未改 `specs/current/`，未 commit。

## 2026-09-15 C07/C08 成员并发写与所有者治理转交

限额中断时 C07 半成品会编不过：`MemberLifecycle.replaceDepartments` 已调用 `requireApplied` / `changes.markAll()`，但类上既无 helper 也无失效依赖。本轮收束并补完 C08。

C07：成员资料 `UPDATE` 带读取时的 `version` 并检查受影响行数；`patch` 在事务内 `FOR UPDATE` 后再做范围/字段/版本校验。资格变更与任职替换同样条件更新；暂停/恢复/移出/调部门在提交后 `markAll()`，无实际变化的写路径不失效。组织设置更新同样带版本条件，冲突报 `REVISION_CONFLICT`，不报告成功。

C08：`transferOwner` 先锁组织行，再按 ID 顺序锁旧/新所有者。新所有者必须为当前租户 ACTIVE 成员，否则 `OBJECT_NOT_FOUND`。旧所有者名下 `source=INITIALIZATION` 且 `revision_kind=SYSTEM` 的 ACTIVE 成员分配视为治理来源：逐条锁定后条件撤销，再写给新所有者一条同样来源的分配；新所有者已持有同一 `revision_id` 的 ACTIVE 分配则复用。`MANUAL` 等独立授权不随转交删除。无治理分配可转时 `POLICY_CONFLICT` 失败关闭。转交审计为 `OWNER_TRANSFER`，分配侧分别记撤销与创建，最后失效热缓存。分配行锁补上 `source` 列，否则锁定后无法核验来源。

验证：`MemberLifecycleTest` 增 1 项（资格/任职提交后失效、无操作与保护所有者不失效）；新增 `TenantQueryServiceTest` 5 项（治理授权随转交、独立授权保留、复用已有分配、停用新所有者与陈旧版本拒绝、缺失治理分配失败关闭、转给自己无写入）。IAM provider 全量 172 项通过。

未做：A09／A10／A16／A26 的真实 HTTP 与并发证据仍待人工；通讯录/字段规则属 C04/C05，不在本条目；未改 `specs/current/`，未 commit。

## 2026-09-15 C04/C05 通讯录可见范围与字段默认/上限

C04 原实现把未覆盖的通讯录默认当成 SELF，匹配 ALLOW 往默认集合上追加，并按书写顺序立刻扣 DENY，本人也可以被禁止隐藏；成员列表在内存分页，部门树不过滤。现改为：无本地覆盖时读取固定默认版本 JSON（空对象即全组织）；任一匹配 ALLOW 的目标并集替代默认；再扣除全部匹配 DENY；最后把查看者本人加回。列表/搜索/详情/count 共用 `DirectoryVisibility` SQL 谓词（全域走 `NOT IN` 禁止集，不先把全员 ID 拉进内存）；部门树只保留有可见成员的节点，祖先标 `navigationOnly`。预览与操作者可见范围求交，并据此设置 `restricted`，不再用可见人数当影响摘要。

C05 原实现字段基线写死在求值器里，原值筛选只检查查看者自己的字段权限。现消费 `iam_default_policy_revision.definition`：`fields` 为租户引用的固定默认（手机邮箱脱敏），`ceiling` 为最新平台版本的最终上限（缺省不额外收紧，租户规则仍可授予 FULL）。匹配规则按 HIDDEN>MASKED>FULL 合并后再与上限取更严。原值筛选对查询范围内可能匹配的目标做披露校验：仅本人 FULL 或存在子集限制时拒绝，避免用隐藏原值过滤/计数推断。

验证：`DefaultPolicyDefinitionsTest` 3 项、`DirectoryVisibilityEvaluatorTest` 4 项（默认全组织、ALLOW 替代、DENY 与顺序无关并恢复本人、祖先骨架）、`FieldAccessEvaluatorTest` 增本人 FULL 不能原值搜他人、平台上限收紧 FULL 规则。IAM provider 全量 181 项通过。

未做：A13／A14／A20 的真实 HTTP、SQL 计划与披露边界证据仍待人工；未改 `specs/current/`，未 commit。

## 2026-09-15 C14/C15 对象能力、草稿预览与菜单边界

C14 原实现列表/详情 `capabilities` 恒为空映射，字段草稿预览仍读已保存策略，诊断只看目标身份的 `actionCodes` 是否包含操作码，忽略 `applicationId`/`targetId`。现新增 `ObjectCapabilities`：同一授权视图上按对象批量计算展示能力（租户成员：update/status/remove/departments；平台成员：update/status/remove；部门：update/delete），缺操作报 `ACTION_DENIED`，有操作但对象越界报 `DATA_SCOPE_DENIED`，提交仍重新鉴权。字段预览走 `FieldAccessEvaluator.snapshot(draft)`，与提交后同一合并规则；未知影响人数继续省略，不用 `0` 伪造。诊断先校验操作者可见目标成员、应用域与操作归属，再核开通/人群与对象范围；无权看来源时 `sources` 为空。

C15 原实现 `OPEN` 菜单不看应用边界，启用菜单跨域直接进入 bootstrap。现 bootstrap 应用列表先按域、启停、租户开通与人群过滤；菜单再按 OPEN/ACTION 判定页面，目录只作为有可见子页的祖先出现，空父目录不展示。

验证：`SessionMenuAssemblerTest` 4 项、`ObjectCapabilitiesTest` 1 项、`DiagnoseAuditServiceTest` 3 项、`FieldAccessEvaluatorTest` 增草稿快照不写库。IAM provider 全量 190 项通过。

未做：A17／A19／A24、F01 的真实 HTTP、bootstrap 菜单与对象能力联调证据仍待人工；角色/组/应用等其它列表的对象能力未在本条铺开；未改 `specs/current/`，未 commit。

## 2026-09-15 C13/C18 完整导出与审计关联

C13 原实现导出只拉第一页 200 条，任务写本机临时目录，下载忽略快照再查实时列表。现改为共享表 `iam_member_export`：登记 `PENDING` 并同事务审计，提交后再异步遍历全部授权页写入成员 ID 快照；`RUNNING` 条件更新避免多实例重复执行。下载重验导出 ACTION、对象范围与当前字段策略，只返回快照与当前可见集合的交集；`PENDING`/`RUNNING` 报 `AUTHORIZATION_UNAVAILABLE` 可重试，失败/过期/错租户报 `OBJECT_NOT_FOUND`，不把失败当成功。任务 24 小时过期并清除快照。GET 仍返回完整 `PageResponse`（不受列表 200 上限），任务状态枚举留给 T13 契约。

C18 审计写入补 `delegation_id`/`assignment_id`/`trace_id`（追踪取 MDC `traceId`）；分配与委派写路径填关联，列表 SQL 分页选出这些列。操作者显示名批量加载后按字段策略投影，无成员读权限或越界则省略。通讯录/字段规则的选择器改为一次批量查询，不再按规则循环打库。

验证：`MemberExportServiceTest` 4 项（跨页完整下载、进行中 503、失败与过期 404）、`DiagnoseAuditServiceTest` 增关联字段与显示名披露 2 项、`IamEnumPersistenceContractTest` 纳入 `ExportTaskStatus`。IAM provider 全量 196 项通过。

未做：A10／A17／A20／A27 的真实 HTTP、多实例与 SQL 计划证据仍待人工；导出任务状态尚未作为独立 HTTP 契约发布（T13）；未改 `specs/current/`，未 commit。

## 2026-09-16 C09 保留功能接入

C09 原实现平台账号、本人资料、字典/发号/社交仍只有旧控制器；Auth/Member 内部 RPC 回退 `SysUser`/`SysTenant`；Security 管理面用 `@AdminOrHasAnyAuthority` 超管短路；登录 JWT `scopes` 只有强制改密标记。现按 API §1.2 接通新入口，权威安全用例仍归框架：

- 平台 `/v1/platform/accounts` 读写 `iam_account`：列表/详情/lookup/创建/资料/删除/启停/锁定解锁/重置密码。创建复用 `RegisterUserUseCase` + `InitialPasswordService`，返回 `{id,version}` 不回明文口令；重置密码才一次性返回 `AccountSecret`。`MEMBER_CREATE` lookup 只返回 id 与登录名。删除时仍有成员资格报 `OBJECT_IN_USE`。对象范围走 `ResourceAccess.objects` / `ObjectScopeSql.restrictAccounts`。
- `/v1/me/profile`、`PUT /v1/me/password`（`@InCryptoHybridContext`）只操作当前认证账号；普通改密缺旧密码失败关闭，强制改密走框架用例。
- `/v1/platform/dictionaries`（`view=tree|page|items`）、`id-allocations`、`social-configs` 包一层 `IamAccess.require` 精确 ACTION，不改字典/发号/社交存储。
- `IamUserAccountPortAdapter` / `IamUserCredentialPortAdapter`、`InnerUserDetailsAPI`、`UsernameIdentityResolver`、`TenantDetailsServiceImpl` 只读 `iam_account`/`iam_tenant`，Feign 仍把组织映射到既有 `SysTenant` DTO。社交绑定仅当 `user_id` 能命中新账号。
- 已建立成员上下文的登录把 evaluator `actionCodes` 写入 JWT `scopes`；成员选择阶段仍不授予 ACTION。`InnerAuthorizationAPI` 用同一求值器，平台 `tenantId` 对外为 `0`。
- Security 平台策略/会话入口改为 `@HasAnyAuthority({IamAction.VALUE_*})`，不再用超管短路。
- Bootstrap 增补账号、字典、发号、社交与安全策略资源：2 应用、40 资源、145 ACTION、145 治理授权。`IamActionOperation` 登记 lookup/enable/disable/lock/unlock/reset-password/revoke。

旧 `SystemUserAPI`、平台字典/发号/社交控制器仍保留，随 T18 清理。未勾选 T04/T05/T10/T13。

验证：`AccountServiceTest` 4 项、`CurrentAccountServiceTest` 2 项、`IamUserCredentialPortAdapterTest` 1 项、`AccountIdentityServiceTest` 覆盖 JWT ACTION 与选择阶段空 scopes、`test_bootstrap_seed.py` 9 项、`IamEnumPersistenceContractTest` 纳入 `IamActionOperation`/`AccountLookupPurpose`。IAM provider 全量 203 项通过；`ingot-security-provider` 编译通过。

未做：A18／A19／A23 的真实镜像、RPC 与 HTTP 证据仍待人工；T13 契约重发未开始；未改 `specs/current/`，未 commit。

## 2026-09-16 T18 旧实现清理（C17 余项）

C17 在锁定态复用之后，旧 HTTP、PMS 授权引擎与 `sys_user` 双读仍与新入口并存。现按替换完成的调用链删除被替代实现：

- 删除已映射或 RETIRED 的旧控制器：`SystemUserAPI`/`SystemDeptAPI`/`SystemRoleAPI`、`Org*`、`AdminTenantAPI`、`AuthUserAPI`、`TestAPI`、旧 `config`/`dev` 目录与应用/菜单/权限/角色/字典/发号/社交/审计入口。
- 删除 `GrantPresenceAuthorizer` 及直接分配计数 SQL；写入门闩只保留 `@Primary` 的 `AuthorizationEvaluator`。`GrantPresenceMemberGuard` 仍作为求值器上的成员写守卫。
- `LocalAuthorizationSnapshotLoader` 与 `InnerAuthorizationAPI` 共用求值器快照，不再组装旧权限码/资源规则。删除 `AuthorizationSnapshotAssembler`、`EffectiveAuthorizationService`、`GrantCeilingService`、`ApplicationAuthorizationResolver` 及旧 `Biz*User/Role/Dept/Org/Auth` 编排。
- 社交解析不再 `sysUserService.getById`；`sys_user_social.user_id` 指向 `iam_account`。删除无调用方的 `SysUserService`/`SysTenantService` 与旧只读审计 `AuthorizationDataAuditService`。
- 保留内部 RPC、字典/发号/社交 *服务*、OSS、`SysUser`/`SysTenant` Feign 外形、`PermissionMatcher` 与迁移分析器，以及无 HTTP 的旧目录 MyBatis 域服务（不在本条清表）。

验证：IAM provider 全量 187 项通过（随旧引擎测试删除，较 C09 的 203 项减少）。未勾选 T18。

未做：A22／A23 真实进程与 RPC 证据仍待人工；旧目录表 Mapper/域服务未整包删除；T13 契约重发未开始；未改 `specs/current/`，未 commit。

## 2026-09-16 T13 契约重发

T13 在 C10 局部错误映射已经落地的前提下，把管理面与保留能力契约与控制器对齐，不改全局 `BizException`，不臆造密码协议：

- 导出增加 GET `/v1/tenant/members/export/{id}/status`，返回 `ExportTask`（`PENDING`/`RUNNING`/`SUCCEEDED`/`FAILED`/`EXPIRED`）；`FAILED` 才带 `failureCode`。下载语义不变：进行中 503，失败或过期 404。
- 通讯录与租户部门树强制 `purpose`（`DIRECTORY` / `MANAGED_DEPARTMENT`）；错配 `InvalidArgument`。租户成员与通讯录列表可选精确 `phone`/`email`；管理面分页 `page`/`pageSize`（默认 20，最大 200）。字典保留 `view`/`code` 与 MyBatis `current`/`size`。
- 账号、`/v1/me/profile`、`/v1/me/password`、字典/发号/社交包装入口写入 `routes.json`/`openapi.json`。字典等既有领域请求体不以 IAM DTO 重写（`RJson`/`RVoid`）。OSS 与 inner RPC 不进管理面 OpenAPI。
- `IamAuthorizationContractTest` 导出账号/导出任务信封；`test_contract.py` 校验 purpose、筛选、导出状态，并解析 `/v1` `@RequestMapping` 与 routes 对账（排除 OSS）。

验证：commons `IamAuthorizationContractTest`/`IamEnumPersistenceContractTest`（含 `SelectionPurpose`）、`python3 tools/iam/test_contract.py` 7 项、`IamPurposesTest` 2 项、`IamErrorHandlerTest` 2 项、`MemberExportServiceTest` 5 项。IAM provider 全量 192 项通过。快照 96 路径 / 161 操作。未勾选 T13。

未做：A23／A24／A28 真实 HTTP、镜像与登录联调仍待人工；未改 `specs/current/`，未 commit。

## 2026-09-16 T16 后端综合验收（首轮，未完成）

T16 要求执行 A01–A28：真实 MySQL 并发/约束、HTTP、镜像/RPC、多实例缓存与导出。本轮只做 Agent 能独立完成的隔离回归与静态归属核查，**不勾选 T16 或任何 A 项**。

自动化证据：

- 隔离 MySQL 8.4：`test_identity_schema.py` 20 项、`test_bootstrap_seed.py` 9 项（2 应用 / 40 资源 / 145 ACTION，种子不含账号，重复执行不覆盖人工改名）。容器 `--network=none --pull=never`，测后销毁。
- IAM provider H2 全量 192 项；契约 `test_contract.py` 7 项（96/161）。
- A22 静态：生产代码无 `IamAccountLockState`；锁定读写走框架 `LockStatePort` / `AccountLockStateMapper`；`LockAccountUseCaseServiceTest` 与 `UnlockAccountUseCaseServiceTest` 通过。失败计数/自动解锁的真实进程证据仍缺。
- A18 静态：服务名 `in-service-iam`，镜像 `ingot/iam`，`InIamApplication`；部署模板无 `ingot-pms`。未做 docker build、Nacos 注册或经 Gateway 的 HTTP。
- A23 静态：无 `SysUserService` 双读；社交解析只从绑定表填 `SysUser.id`。Auth/Member/Security 真链仍缺。

A 系列仍须人工（见 `MANUAL-VERIFICATION.md`）：0.1 独立库与进程、A18 镜像/Gateway、A21.1 首启改密、A01–A17/A24–A28 真实 HTTP，以及 A15/A20/A27 多实例与执行计划。

未做：未启动业务进程，未改 `specs/current/`，未 commit，未勾选 T16。
