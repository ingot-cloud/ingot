# 实施与验收记录

日期：2026-10-14。状态：validating，等待用户产品验收。未创建 commit/PR、未更新 current、未切换实际业务数据库。下列证据来自本地隔离环境，不能作为生产迁移放行记录。

## 实现结果

- 拥有者以当前组织 ownerMemberId、有效成员/账户判定；新组织不建立固定治理分配。有效开通应用及未来操作动态获得，转交保留普通分配。
- 平台共享角色和组织私有角色保存完整当前定义；租户分配只引用角色 ID。技术锁、内部不可变快照及审计保留，租户业务版本/升级/共享定制退出。
- 委派按角色白名单和资源上限持续求交；同资源新操作跟随，新资源/参数未配置时关闭对应贡献。仅本人有效来源提供受限分配入口，派生权限不提供再次委派或角色写入资格。
- TENANT 在 IAM、远程 SDK 和旧快照入口按新请求读取事实；请求内复用和批量加载，依赖失败关闭。平台固定版本、字段和角色工作台保留原路径。
- 当前角色/分配/委派前端完成，保存后刷新权限；租户可见页按期限且最多30秒调度，隐藏停止、错误退避、切身份阻止迟到恢复。

## 已执行自动化

| 范围 | 结果 | 入口/证据 |
|---|---|---|
| IAM 全量 | 483项，456通过、27条件跳过，0失败 | `./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test -I tools/iam/tenant_live/verification.gradle --offline`；`evidence/backend-full.log` |
| 授权 SDK | 31通过 | `:ingot-framework:ingot-access-control:test`；含旧缓存/远端不可用/平台缓存路径 |
| 旧快照与范围框架 | 15通过 | `:ingot-framework:ingot-data:ingot-data-mybatis-scope:test`；租户逐次在线/平台缓存隔离 |
| 公共模型/契约 | 70通过 | `:ingot-framework:ingot-commons:test`；Java导出schema及严格当前输入 |
| MySQL结构 | 27通过 | `databases/iam/test_identity_schema.py`；当前头所属、私有跨租户及初始化事务约束 |
| 迁移纯工具 | 9通过 | `tools/iam/tenant_live/test_migrate.py`；物化、最新权限、冲突、类型、时区、SQL字面量 |
| MySQL迁移/回滚 | 5通过 | `tools/iam/tenant_live/test_sql.py`；增量/新库字段及索引一致、正逆幂等、来源变化阻断、新写入保留、完整备份还原 |
| 生成工具 | 17通过 | `test_contract`、`test_database_sources`、`test_generate_bootstrap`；149路径/219操作及5内部操作 |
| 前端公共授权组件 | 139通过 | 当前角色只读/409/迟到响应、稳定参数清理、来源切换/roleId提交等 |
| 组织插件 | 35通过 | `@ingot/org-plugin test` |
| 平台插件 | 179通过 | `@ingot/platform-plugin test`；自有角色版本/字段/任务保持 |
| 核心库 | 466通过，1既有失败 | 导航样式断言，见后述；新增拥有者与30秒/503/隐藏/平台隔离调度测试通过 |
| 构建/类型/边界 | 通过 | admin-core/admin-common、org/platform类型检查、apps/admin与apps/admin-platform构建、改动文件ESLint及分层边界 |

完整 IAM 执行使用单测试类隔离 JVM（1GiB），排除了原有需外部服务的 `com.ingot.cloud.iam.UnitTest`。27个条件测试不冒充已执行；另用显式开关单独运行租户和平台真实 MySQL/HTTP 夹具。

## 真实 HTTP / SDK / 浏览器

`TenantLiveMySqlHttpTest` 在一次性 mysql:8.4 与两个真实 Tomcat 节点运行生产 Mapper、事务、授权服务、控制器和内部授权端点。测试身份过滤器仅在夹具中存在；消费节点通过生产 RemoteAuthorizationClient/AuthorizationAccess 请求真实内部 HTTP，失效通知刻意不传播，缓存入口不提供租户执行结果。

覆盖共享角色两组织下一请求增删权、共享只读403、旧字段400/旧路由410、私有跨组织404、本人候选名单、角色ALL经部门资源上限收缩、同资源跟随/新资源关闭、来源撤销、拥有者人群豁免、普通字段脱敏/拥有者FULL及平台强制上限、撤权403、基础设施故障503与恢复。平台 `RoleWorkspaceMySqlHttpTest` 以 IAM_MYSQL_HTTP_TEST=true 单独复验固定版本工作台和字段执行。

`ingot-admin/scripts/verify-tenant-live-browser.mjs` 使用正式构建组件、样式与真实接口，完成共享创建/当前权限预览/保存、租户共享只读、拥有者标识、两个租户成员下一请求撤权。目录适配器和根页面是测试夹具；未用Mock替代保存或授权控制器。浏览器截图见 `evidence/browser-owner.png`。

## 性能

环境：本机 Java21/Gradle8.12.1、MySQL8.4隔离容器，连接池64；100租户×1000成员，每租户测一个普通身份，直接/组共4角色，每角色100操作，10资源。每并发档运行60秒。

| 并发 | 请求数 | p50 | p95 | 吞吐 | 非预期错误 |
|---|---:|---:|---:|---:|---:|
| 20 | 56,889 | 20.1ms | 31.0ms | 947.6/s | 0 |
| 100 | 63,188 | 91.8ms | 142.4ms | 1,052.0/s | 0 |

原始结果见 `evidence/performance.json`。100并发p95低于200ms目标；最终HTTP/SDK/浏览器/性能联合执行通过（当时4项，无失败或跳过），日志见 `evidence/joint-http-browser-performance.log`。补充写入预算后HTTP/SDK/浏览器4项通过、性能1项条件跳过，见 `evidence/tenant-http.log`。单次capabilities的1角色和4角色都为12次SELECT，采样为隔离MySQL的Com_select差值；消费SDK资源请求为1次内部HTTP求值。角色头、快照权限和参数按请求批量加载；整个组接收范围用单个集合查询校验。共享保存在1与100个有效关联分配下都为4条授权DML，保存前后逐行比对分配内容相同（见evidence/http-metrics.json）；只写当前定义/快照/审计，不循环成员或改写分配行；每个逻辑资源一次SDK求值，结果用于本次请求范围/字段处理。平台读取请求复用和查询预算包含在平台自动化中。

## 已知基线项与验收边界

- admin-core `visual-contract.test.ts:379` 期待 InAppBar 中不存在的 margin。实际 InAppBar.vue 与HEAD摘要相同，本次未修改该样式；记录原有失败，避免为了本变更改动平台视觉行为。
- `tools/iam/test_service_naming.py` 两项依赖仓库缺失的 `.gitlab-ci.yml`，失败原因与本改造无关，单列于 evidence/naming.log。
- 实际数据库来源预检、生产完整备份、部署清单和维护窗口确认由发布负责人执行；本次未访问真实库。P09/P10 已在隔离库演练，不能跳过实际来源冲突审核。
- 当前浏览器证据覆盖关键当前角色路径；完整组织设置转交、成员表单、平台自有角色任务及窄屏产品路径保留在 ACCEPTANCE 的用户验收步骤中。相关后端事务/字段与前端定向回归通过，不宣称已完成这些手工产品验收。

## 复跑入口

仓库中的 `tools/iam/tenant_live/verification.gradle` 固化了单测试类JVM隔离（1GiB）与可选真实夹具开关，不依赖临时/tmp脚本。需要Java21、已有mysql:8.4、Docker；不下载镜像或访问实际库。浏览器需要构建前端公共包、可用Node/Chromium；本机夹具入口记录于测试，换机器需按已安装路径配置。

```sh
./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test :ingot-framework:ingot-access-control:test :ingot-framework:ingot-data:ingot-data-mybatis-scope:test :ingot-framework:ingot-commons:test -I tools/iam/tenant_live/verification.gradle --offline
./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --tests 'com.ingot.cloud.iam.live.TenantLiveMySqlHttpTest' -I tools/iam/tenant_live/verification.gradle -PiamLiveHttp=true -PiamLiveBrowser=true -PiamLivePerformance=true --offline
./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --tests '*RoleWorkspaceMySqlHttpTest' -I tools/iam/tenant_live/verification.gradle -PiamPlatformHttp=true --offline
python3 databases/iam/test_identity_schema.py
python3 -m unittest discover -s tools/iam/tenant_live -p test_migrate.py
python3 -m unittest discover -s tools/iam/tenant_live -p test_sql.py
PYTHONPATH=tools/iam python3 -m unittest test_contract test_database_sources test_generate_bootstrap
```

## 验收对应

B01–B06：初始化/身份/转交事务、授权引擎、通讯录/字段执行及真实SDK；B07–B14：当前角色、分配/委派、HTTP、MySQL引用约束及组件测试；B15–B16：双节点无消息、实际SDK、旧快照在线读取及故障注入；B17–B20/B24：迁移、完整备份恢复、逆转换、改码和来源检查；B21/B22：组件、浏览器关键路径、平台回归与双管理台构建；B23：性能记录及批量读取实现。用户验收前保留原矩阵，不将手工项或生产切换标成完成。

## 未上线开发准备增量

用户补充确认租户授权无需保留、021已执行而022失败，交付 [一键开发准备](./DEV-TEST-PREPARATION.md) 和 `databases/iam/dev/prepare_tenant_live_test.sql`。3项隔离MySQL验收通过，保留平台事实与共享最新范围，停用旧租户授权并自动安装022，重跑不重置新测试数据，结构缺失先拒绝。证据见 [dev-prepare.log](./evidence/dev-prepare.log)。原生产迁移/逆转换仍保留；本增量未访问实际库。
