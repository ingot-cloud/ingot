# 验证记录

2026-11-07：仅完成既有Spec、代码、引用、缓存序列化及工作区只读检查。后端工作区开始前干净；前端存在生成声明及help引用目录的用户既有变更，不纳入本变更覆盖范围。尚未执行本轮编译、测试或运行环境验收。

后续逐阶段补充命令、范围、结果和限制；不得以历史结果宣称本次通过。历史表、数据和数据库升级脚本保持不动。

## P0 基线

逐类盘点902个类型：API127、provider453、commons IAM322；数据库及历史迁移工具153文件已记录SHA-256。源码快照与测试XML保存在本机会话临时目录，用于差异检查。

基线命令：`./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test :ingot-service:ingot-iam:ingot-iam-api:test --offline --console=plain`。provider290项，12失败、2跳过；API任务未执行。失败在本次业务代码修改前已经存在，后续不能以排除这些测试宣称全量通过。

- `Gradle Test Executor 1#failed to execute tests`：org.gradle.api.internal.tasks.testing.TestSuiteExecutionException: Could not complete execution for Gradle Test Executor 1.
- `com.ingot.cloud.iam.catalog.CatalogServiceTest#listGrantCatalogNestsEnabledActionsAndSkipsDisabled()`：org.springframework.jdbc.BadSqlGrammarException:
- `com.ingot.cloud.iam.catalog.CatalogServiceTest#platformResourceRejectsDepartmentScopesWhileTenantCatalogCanDeclareThem()`：org.springframework.jdbc.BadSqlGrammarException:
- `com.ingot.cloud.iam.catalog.CatalogServiceTest#lookupActionsResolvesApplicationAndResource()`：org.springframework.jdbc.BadSqlGrammarException:
- `com.ingot.cloud.iam.catalog.CatalogServiceTest#createApplicationBundlePersistsCatalogAndMenus()`：org.springframework.jdbc.BadSqlGrammarException:
- `com.ingot.cloud.iam.catalog.CatalogServiceTest#purgeApplicationClearsAllAssociations()`：org.springframework.jdbc.BadSqlGrammarException:
- `com.ingot.cloud.iam.catalog.CatalogServiceTest#createApplicationBundleRollsBackWhenMenuActionIsUnknown()`：org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <com.ingot.framework.commons.error.BizException> but was: <org.springframework.jdbc.BadSqlGrammarException>
- `com.ingot.cloud.iam.catalog.CatalogServiceTest#deleteApplicationExplainsOwnedCatalog()`：org.springframework.jdbc.BadSqlGrammarException:
- `com.ingot.cloud.iam.catalog.CatalogServiceTest#getActionCatalogGroupsResourcesAndActions()`：org.springframework.jdbc.BadSqlGrammarException:
- `com.ingot.cloud.iam.catalog.CatalogServiceTest#listMenuActionsReturnsBoundActionsWithResourceNames()`：org.springframework.jdbc.BadSqlGrammarException:
- `com.ingot.cloud.iam.catalog.CatalogServiceTest#listResourcesFiltersByNameAndCode()`：org.springframework.jdbc.BadSqlGrammarException:
- `com.ingot.cloud.iam.extension.RoleFieldPermissionServiceTest#freezesSafeDefaultsAndRejectsUnregisteredFieldsResourcesAndEditing()`：org.opentest4j.AssertionFailedError: Expected com.ingot.framework.commons.error.BizException to be thrown, but nothing was thrown.
- `com.ingot.cloud.iam.catalog.EntitlementServiceTest#tenantActionCatalogReturnsEnabledResourceTree()`：org.springframework.jdbc.BadSqlGrammarException:

## P1

关联IAM/Member/Security及测试编译通过（`/tmp/iam-cleanup-p1.log`）；API全部5项与社交/账号/当前身份定向测试通过（`/tmp/iam-cleanup-p1-tests.log`）。新增内部DTO保留字段、UTC与OSS签发，社交绑定使用全局账号ID。

P2退出旧源码/资源共181文件；包含3个仅覆盖已退役实现的测试。历史迁移退出原因沿用原change的全新系统决策，离线工具与SQL保留。

## P2

旧链退出后IAM身份定向测试及Member/Security编译通过（`/tmp/iam-cleanup-p2.log`）。额外识别并退出仅验证已删除DeptBindingConstants的专用测试。公开快照仍需DataScopeTypeEnum，保留其框架依赖。

## P3

commons框架测试、IAM模型/测试及关联模块编译通过（`/tmp/iam-cleanup-p3.log`）；全部迁移模型契约与身份定向测试通过（`/tmp/iam-cleanup-p3-tests.log`）。Schema导出已迁往provider。基线另有测试执行器OOM，后续通过临时JVM堆及分叉配置验证，不改业务JVM配置。

## P4 首轮回归与历史测试修正

所有关联模块/测试源码编译通过（`/tmp/iam-cleanup-p4.log`）。增加测试堆与分叉后provider完整执行690项，13失败、29跳过。原基线受OOM仅完成290项；扩展暴露的MemberRejoin断言仍期望旧CreateInput，与已上线RejoinInput契约不符。其余失败为两个H2资源表夹具缺少当前DDL已有field_policy，以及共享角色字段权限仍被旧断言当作禁止。

仅更新测试夹具/断言：两个H2表补已有字段；重新加入保留字段投影断言并改为RejoinInput；共享角色增加正向断言，并将不允许配置字段的负向场景明确为SYSTEM。生产授权行为、DDL和历史脚本不变。

P4复测：`/tmp/iam-cleanup-p4-tests.log` 全部通过，完整provider测试690项（29条件跳过）；API5项通过。辅助HTTP实体边界与请求上下文调整后编译及测试源码通过（`/tmp/iam-cleanup-http.log`）。仍需P5职责拆分后的综合复测、结构检查和环境验收。

## P5 职责拆分与前端清理

Catalog、平台角色/分配、租户分配/委派的方法主体按职责拆到应用服务；薄门面只聚合调用。共享规则、锁与绑定校验独立。全部构造依赖静态图152个Bean、408条直接协作者边，无发现环；ObjectProvider及框架装配不由该静态图作运行时保证。永久边界检查含此门禁。

定向授权/事务/对象解析回归通过（`/tmp/iam-cleanup-p5-tests.log`）。前端退出150个退役PMS专用文件，保留IAM正式页面；相关platform/org/admin-common测试分别153/94/185项通过；四个相关模块类型检查通过；公共包/主题与admin、admin-platform两宿主构建通过，分层检查通过。使用本机已有Node22.17.0与pnpm10.12.4，未更改仓库engine约束或安装依赖。

构建自动生成的无关声明差异已恢复，原有用户声明差异保留；只移除指向已删除源文件的声明。前端Spec和逐文件原哈希见ingot-admin关联change。

## P6 最终自动化结果

- `./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test :ingot-service:ingot-iam:ingot-iam-api:test :ingot-framework:ingot-commons:test :ingot-service:ingot-member:ingot-member-provider:compileTestJava :ingot-service:ingot-security:ingot-security-provider:compileTestJava :ingot-service:ingot-auth:ingot-auth-provider:compileTestJava :ingot-service:ingot-bff:compileJava :examples:iam-ops:compileJava -I /tmp/iam-cleanup-tests.gradle --offline --console=plain`：BUILD SUCCESSFUL，provider691项（662通过、29条件跳过），API5、commons12全部通过。临时测试配置仅将测试堆设2g、单并发并每20类分叉；未改业务运行配置。结果见junit-results.json。
- `IAM_MYSQL_HTTP_TEST=true IAM_TENANT_LIVE_MYSQL_HTTP_TEST=true ./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --tests '*RoleWorkspaceMySqlHttpTest' --tests '*TenantLiveMySqlHttpTest' --tests '*AuthorizationCachePayloadCompatibilityTest' -I /tmp/iam-cleanup-tests.gradle --offline --console=plain`：BUILD SUCCESSFUL。26项隔离MySQL/HTTP、1项缓存兼容通过；另2项性能/浏览器条件未启用。临时容器随机命名、仅使用本机已有mysql:8.4镜像，均已按测试流程删除；未连接现有库。结果见mysql-results.json。
- 隔离回归仅修正已有测试夹具：补当前field_policy、委派成员关系id及漏注册TenantRoleWorkspaceMapper；字段上限断言按当前逐资源默认策略插入resource_id。保留原行为断言，生产DDL与授权规则未改。
- 缓存兼容测试使用生产Jackson配置及实际授权缓存装配，旧无类名JSON命中不触发数据库求值；新写保持同一结构、命名空间和30秒TTL。字典实体因JDK/类型元数据更名隔离到`:entity-v2`命名空间，原会话及LKG未操作。事件TYPE、worker配置键和指标前缀保留。
- `python3 -m unittest discover -s tools/iam -p 'test_package_boundaries.py'`：6项通过；`test_contract.py`8项通过；`test_database_sources.py`4项通过。package/path、API无ORM、共享层无provider依赖、分层方向、直接Bean依赖无环、Mapper XML解析均核对。
- Schema/OpenAPI/内部OpenAPI生成与`--check`均通过；路由生成器在临时目录重建后与快照一致。246个管理操作（175路径）、5内部操作保持；原路径/方法/operationId逐项一致，只允许Schema组件及引用按Java类型更名。对比证据见contract-compatibility.json。
- 数据库/历史工具153个基线哈希全相同，无历史表、数据、初始化SQL或升级脚本变更；原902类型的有效目标全部存在，164个删除类型均已退出生产源码；无旧类空壳。两仓库`git diff --check`通过。

### 已知无关失败与未执行项

1. admin-core完整542项单测中540通过、2项既有源码文本断言失败：visual-contract顶栏外边距、InTable背景定位。涉及的测试和生产源文件均与HEAD逐字节相同；前端关联change保留unchanged-ui-failures.json证据。本轮不修改无关UI行为或删除断言。
2. 额外运行未修改的`test_service_naming.py`：4项通过、2项读取`.gitlab-ci.yml`错误。该文件在本轮开始的HEAD与工作区均不存在；本次不虚构CI配置或弱化此旧测试。构建/RPC/部署命名的其他4项通过。
3. 真实登录与社交供应商、目标服务进程/Feign调用、外部OSS和Redis集群、性能/浏览器与用户验收仍归P7及原IAM主change验收。本轮隔离HTTP与SDK测试不替代这些证据。未发布、未更新current或归档；该轮验证时尚未提交代码，后续按用户要求的提交见README。

## P7 初始待验收清单

1. 配套重建并启动IAM及Member/Security/Auth/BFF等消费者，核对正式入口、当前身份/登录、社交绑定、字典和发号维护。
2. 以平台管理员、租户拥有者、普通成员及受限管理员核对角色/分配/委派、字段和组织隔离；检验fresh写入、到期/撤权和失败关闭。
3. 核对Redis跨节点通知、worker持久租约/重启恢复；切换时使用配套制品与字典实体缓存命名空间，禁止全库清Redis或变更历史表。
4. 用户确认后更新current，保留原IAM主change未完成的验收项，再归档本change。

## 启动入口验收修正（2026-11-07）

用户发现 InIamApplication 被批量迁移误放到 `com.ingot.cloud.iam.application.com.ingot.cloud.iam`。该类没有显式扫描范围，位置错误会收窄 Spring Boot 默认扫描，前述业务测试/隔离HTTP装配不能证明完整服务入口扫描正确。原静态门禁仅检查已有根级文件且误写类名 IamApplication，未要求根级启动文件存在，造成漏检。

已将文件及package恢复为 `com.ingot.cloud.iam.InIamApplication`，修正台账为保留根包入口，并移除错误迁移映射。静态门禁现在要求根级文件存在、启动类唯一且位于根包；修复前两项断言确实失败，修复后7项全部通过。新增 InIamApplicationTest 用真实Spring扫描器从默认根包扫描，确认应用服务、配置、持久化装配与HTTP组件均可发现。

验证命令：`./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --tests 'com.ingot.cloud.iam.InIamApplicationTest' :ingot-service:ingot-iam:ingot-iam-provider:bootJar --offline --console=plain`，BUILD SUCCESSFUL（`/tmp/iam-startup-root-verification.log`）。JAR清单 Start-Class 为 `com.ingot.cloud.iam.InIamApplication`，仅包含正确位置的启动类；所有生产Java包声明与路径一致，无重复IAM根包，数据库/历史工具153项哈希不变。结果见 startup-root-validation.json。此次验证不启动服务或连接真实数据库，P7仍待完成。

## 空包目录清理（2026-11-07）

按用户要求清理迁移后遗留的107个空目录：API主源码25、provider主源码47、provider测试35。仅对完全空的目录执行rmdir，自底向上处理；保留源码/资源根目录，不处理build产物或其他模块。清理前后核对910个源码及资源文件，路径与内容哈希完全一致，扫描范围内无剩余空目录。仅目录清理，无需重复编译或业务测试。

## 用户重启与登录验证反馈（2026-11-07）

用户反馈：重新启动服务后，平台端和租户端登录及部分接口测试通过。此为用户实测反馈，未提供具体环境、服务清单及接口明细；据此确认已报告的启动、双端登录及部分接口链路正常，不外推为全部功能或跨节点链路已验收。

结合已有自动化、隔离HTTP及启动包修正验证，当前已验证范围未发现本次重构问题。P7继续保持未完成，待补充关键写入、权限授予/撤销与隔离、相关跨服务及持久任务链路的实际覆盖后，再确认完整验收并更新current、归档。

## P7 补验（2026-11-10）

### 本机开发环境

- Auth、IAM、BFF 健康检查均返回 HTTP 200。平台登录会话刷新后能读取租户列表。用户重新登录测试组织后，使用真实拥有者会话验证角色向导。空权限被拦截，选择单项「查看部门」后预览通过。提交后临时角色出现在角色列表，未分配给成员。经用户确认通过业务入口删除，按唯一名称复查无结果。角色分配和授权管理员列表可读。
- 用当前 `e3d03af7` 配套制品在独立回环端口启动 Member 和 Security，禁止 Nacos 注册，两者健康检查均返回 HTTP 200。Member 的发号预加载与 OSS 清理同时出现缺表错误。只读核对本机 `ingot_member`，发现旧表 `biz_leaf_alloc`，缺少 `fw_id_leaf_alloc` 和 `fw_oss_asset_upload`。在临时 MySQL 8.4 库导入当前 `databases/ingot_member.sql`，确认两张表存在，重启同一 Member 制品后健康检查返回 HTTP 200，启动日志未出现上述 SQL 错误。临时进程和容器已退出，开发库仅做元数据查询。
- 平台字典页和开发者平台跳转发生动态导入错误。字典与开发者入口的 Vue 模块本身返回 HTTP 200，引用的 Element Plus 预构建依赖返回 `504 Outdated Optimize Dep`。字典、发号和社交链路的浏览器实测仍待补齐。

### 隔离验证

- 离线重建 IAM、Member、Security、Auth、BFF 的 `bootJar`，核对 5 个清单的 `Start-Class`。使用 `tools/iam/tenant_live/verification.gradle` 启用 `iamLiveHttp` 和 `iamPlatformHttp`，执行 `RoleWorkspaceMySqlHttpTest`、`TenantLiveMySqlHttpTest`、`AuthorizationCachePayloadCompatibilityTest`、`InIamApplicationTest`。共 40 项，35 通过、5 项条件跳过、0 失败。包括真实隔离 MySQL 和 HTTP 的角色字段、受限委派、跨租户隔离、消费者 SDK、持久任务租约接管、重启续跑及重复审计防护。隔离 HTTP 使用测试身份。
- 从本次 IAM 制品加载 `RedisInvalidationBus` 和正式事件类型，启动两个独立 JVM 和一次性回环 Redis。订阅端成功收到 `authorization.invalidate` 与 `iam.role-application.wakeup`，校验来源、时间戳及业务载荷。该测试覆盖传输，目标环境多节点的提交后广播、接收端缓存失效和 worker 接管仍需实测。

### 本机环境补齐后复测

2026-11-10 按用户授权补齐本机测试库。先备份 Member 的三张旧表和 Security 的事件旧表，核对字段、索引及依赖，再按现有框架表映射原地更名。Member 保留 1 条账号锁定记录，Security 保留 4 条事件及旧库附加的 `idx_event_time` 索引。Member 用模块现有 DDL 创建 `fw_oss_asset_upload`。复核两库共五张框架表及行数。Member 和 Security 的配套制品分别在独立回环端口复启，健康检查均为 HTTP 200，未再出现缺表错误。

强制重建平台 Vite 预构建缓存后，真实平台会话可读取字典、业务 ID 发号、社会化登录配置、App 用户和凭证策略页面。字典和社会化配置返回空列表，业务 ID 发号和 App 用户各显示 1 条。IAM 的三个对应管理 API 均记录 HTTP 200。单独重载 App 用户与凭证策略页面，Member 和 Security 的业务接口各新增 1 次 HTTP 200，IAM 内部授权求值新增 2 次 HTTP 200。

### 写入与模拟微信验收

- 用户确认微信验收以模拟登录覆盖应用内流程，第三方接口调用结果不作为验收条件。本机 IAM 的微信配置为零。用一次性本机 OAuth 客户端提交 `social` 授权及合成微信码，Auth 返回 HTTP 400，Member 用户查询记录 HTTP 500，IAM 的社交配置 RPC 新增一次 HTTP 200，未签发令牌。Member 小程序注册用合成手机号授权码直连和经网关各收到 HTTP 500，均返回通用错误信封。临时 OAuth 客户端已清理。`MiniProgramAccountBindingTest` 两项通过，覆盖用合成 OpenID 写入全局账号绑定，以及查询已绑定和未绑定账号。
- 平台管理员通过正常页面新增字典类型及字典项，修改展示文本并复查返回值。独立发号标签从步长 5 更新为 7，列表读取与写入一致。经用户确认清理两条字典记录和一条发号配置，复查无可见字典记录，发号配置物理行数为零。
- 测试组织拥有者创建无成员、无部门来源的临时组。使用现有测试角色、指定对象范围完成预览和实际分配，随后撤销。复核组内成员为零、有效分配为零、撤销记录一条。用户确认后请求删除空组，接口因撤销记录保留引用而拒绝删除。另复核委派向导能读取候选管理员，并拦截缺少管理员、角色与接收范围的表单。
- 本轮运行 `MiniProgramAccountBindingTest`、`RoleWorkspaceMySqlHttpTest`、`TenantLiveMySqlHttpTest`、`FieldWriteTransactionTest`、`DelegationDurationAdmissionTest` 和 `RoleVersionApplicationWakeupNotifierTest`。46 项中 41 通过、5 项条件跳过、0 失败。隔离覆盖受限委派、字段写入事务、到期状态、两执行器争抢、租约转移和重启续跑。
- 启用可选浏览器回归后，首次运行因测试夹具仍提供分页目录，且按旧权限名称查找复选框而失败。前端夹具已适配当前数组契约，按正式操作编码识别权限。单独复跑 `actualBrowserComponentsAgainstLiveControllers` 通过，覆盖真实组件共享角色创建、预览、保存，租户只读和两个租户成员在下一次请求撤权。HTTP 身份与 MySQL 数据均由隔离夹具提供。

源码检查发现，`WxMaServiceHelper.getActiveService()` 在默认小程序 AppId 非空时抛出「未配置」错误。该条件自 `3e14db1ee` 存在。此次空配置模拟无法覆盖已配置 AppId 的手机号授权分支。

### 完整制品验收与字段回显差异

2026-11-10 在本机启动两个独立 IAM `bootJar` 和 Auth `bootJar`。使用一次性 MySQL 8.4 和 Redis，按仓库现有 SQL 初始化，临时 JVM 仅监听回环，关闭服务注册。Auth 通过正式 password grant 签发平台、拥有者、受限管理员、接收人和名单外成员的令牌，五个账号的登录记录均由 IAM 持久化。

- 消费者 RPC 通过。独立 JVM 使用制品内的正式 Feign 契约、Spring MVC 参数绑定和生产内部请求拦截器，核对租户详情、租户批量、账号关联租户、账号详情和批量、字典项、原始节点和批量、空社交配置和不存在的应用配置。发号接口连续返回两个不同的 `in_` 标识。Member、Security 的转发授权已在前轮由真实会话验证。
- 受限委派和撤权通过。受限管理员无法读取组织角色，委派候选仅含允许的接收人，名单外对象和超出时长的分配被拒绝。允许的分配提交至 IAM B 后，两节点均允许接收人读取本租户资料，拒绝读取其他租户成员。按实际 UTC 期限等待自然到期后，同一登录令牌在两节点都返回 HTTP 403，拥有者列表显示 `EXPIRED`。另建的短时分配在 IAM A 撤销委派后，IAM B 立即返回 HTTP 403。
- 租户字段策略的脱敏和服务端写入门禁通过。为接收人配置禁止编辑手机号的租户策略后，两节点返回 `MASKED` 和 `editable=false`，均拒绝手机号写入，数据库原值未变。
- 双节点接管通过。经正式版本应用 API 准备并确认 205 条分配。IAM A 提交首批 100 条后停止进程，IAM B 在原 30 秒租约有效时没有更新，租约自然到期后换用新租约并处理余下 105 条。任务为 `SUCCEEDED`，205 条分配全部切换，205 条更新审计对应 205 个不同的分配。重启 IAM A 后审计总数仍为 205。临时进程、容器和凭据均已清理。

字段回显仍有一项未通过。角色快照明确设置手机号 `editable=false`、租户没有附加编辑规则时，两节点的成员详情 `fieldAccess.phone` 都回显 `MASKED` 和 `editable=true`。同一受限账号修改手机号收到 HTTP 403，数据库原值未变，同版本修改允许编辑的显示名成功。前端成员详情抽屉使用该回显决定手机号输入框是否可编辑。`MemberQueryService` 的详情投影在重构前已使用租户字段规则和成员更新操作计算回显，实际写入还校验角色字段上限。

该差异作为 P7 最后一项验收缺口进入补充设计。

### 字段回显复验

2026-11-10 用修复后的同一 IAM `bootJar` 启动两个独立进程，共用一次性 MySQL 8.4 与 Redis。另启动现有 Auth 制品，用临时 OAuth 客户端取得拥有者、委派管理员和接收人的正式令牌。角色设置手机号 `MASKED`、`editable=false`，租户附加操作规则为空。验收请求经正式 HTTP API。

- IAM A 和 IAM B 的 GET 均返回脱敏手机号与 `editable=false`，显示名仍可编辑。提交手机号时两节点均返回 HTTP 403，MySQL 中手机号与成员版本保持原值。
- 两节点均用相同版本保存允许编辑的显示名，版本递增，保存响应保持手机号脱敏且只读。复测使用同一接收人 OAuth 会话。
- 修复制品 SHA-256 为 `984000f439e57e57190fd17cbf6d3fcd68360265d10632b23006fc5d21a8eb32`。临时 OAuth 客户端补齐 x509 令牌设置后复跑通过，临时容器、进程和凭据已清理。

### P7 最终自动化

- `TenantLiveMySqlHttpTest` 启用隔离 MySQL HTTP 测试后，11 项通过、5 项可选环境用例跳过。新增回归覆盖无更新资格、实际部门范围、角色字段限制、拥有者、可编辑的脱敏字段和预览到期。在预览到期时仍可合法保存显示名，响应从事务写授权取得编辑提示。
- 初次全量运行在 MyBatis 夹具初始化期间耗尽测试 JVM 堆。复跑使用仓库 `tools/iam/tenant_live/verification.gradle`，设定 IAM 测试 JVM 上限 2 GiB，每 12 个类重建进程。
- 补跑发现两份框架测试引用迁入 provider 的 IAM 内部类型，导致测试编译失败。通用断言改用独立夹具，两项成员绑定契约断言迁回 IAM 契约测试，原断言保留。
- 最终 `ingot-iam-provider:test` 通过 1,475 项、跳过 29 项条件用例、失败 0 项。`ingot-access-control:test` 通过 31 项。包边界检查通过 10 项，`bootJar` 构建成功，启动类为 `com.ingot.cloud.iam.InIamApplication`。本次修正未修改数据库或离线工具文件。

P7 和 A09 按本机验收口径完成。消费者 RPC、受限委派、到期撤权、双节点接管及模拟微信使用本记录前述证据。
