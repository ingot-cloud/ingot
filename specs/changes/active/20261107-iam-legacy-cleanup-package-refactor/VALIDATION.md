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
3. 真实登录与社交供应商、目标服务进程/Feign调用、外部OSS和Redis集群、性能/浏览器与用户验收仍归P7及原IAM主change验收。本轮隔离HTTP与SDK测试不替代这些证据。未发布、未提交代码、未更新current或归档。

## P7 待验收清单

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
