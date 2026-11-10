# 隔离性能基线

## 测量条件

2026-11-09，在生产源码提交 `ab8f429db8540f0b7460cc49ba86de066f86632d` 测量。Corretto 21、隔离 Docker MySQL 8.4，初始化使用仓库 `databases/ingot_iam.sql`。复用 `TenantLiveMySqlHttpTest` 的 Tomcat、真实 IAM 控制器和 Mapper、UTC JDBC 与合成身份，补入两个正式操作目录控制器。该夹具通过专用测试过滤器注入身份，因此端到端耗时是本机 HTTP 与服务读路径测量，生产网关、登录和平台授权热缓存的成本需在目标环境另测。

普通成员在 1 个和 16 个启用、已开通且人群命中的应用下分别持有每应用一项操作。16 应用场景继续测拥有者、持有有效委派来源的管理员、租户角色页及平台和租户的操作目录。两个目录使用同一个内置 `iam-tenant` 应用。每组串行预热 30 次、采样 200 次，合计 2,200 个成功样本。延迟由 HTTP 客户端计时，按最近秩取百分位。SELECT 数用隔离库 `Com_select` 在采样前后取差，表中为单请求平均值，包含准入与业务 SQL。

## 实测结果

| 场景 | SELECT/请求 | P50（ms） | P95（ms） | P99（ms） |
|---|---:|---:|---:|---:|
| 普通成员 `bootstrap`，1 应用 | 20 | 12.84 | 16.55 | 19.08 |
| 普通成员 `capabilities`，1 应用 | 13 | 7.83 | 9.64 | 12.01 |
| 普通成员 `bootstrap`，16 应用 | 50 | 29.20 | 33.73 | 35.37 |
| 普通成员 `capabilities`，16 应用 | 28 | 16.27 | 19.86 | 21.86 |
| 委派管理员 `bootstrap` | 32 | 18.11 | 21.50 | 22.67 |
| 委派管理员 `capabilities` | 10 | 5.47 | 6.72 | 8.14 |
| 拥有者 `bootstrap` | 12 | 6.53 | 7.90 | 8.73 |
| 拥有者 `capabilities` | 6 | 3.23 | 4.53 | 5.79 |
| 拥有者租户角色页 | 8 | 4.35 | 5.49 | 6.76 |
| 租户 `action-catalog` | 33.01 | 18.25 | 20.74 | 22.76 |
| 平台 `action-catalog` | 9 | 4.27 | 5.55 | 6.40 |

普通成员增加 15 个应用后，`bootstrap` 增加 30 条 SELECT，`capabilities` 增加 15 条。两条链路分别对应权限求值加应用列表装配，以及单独权限求值。租户目录的 33.01 条为隔离库计数平均值。目录加载本身涉及应用、资源、操作三次查询，表中总数还包括鉴权及开通检查。

## 复测

本地基线使用临时 Gradle init 和 `IamReadPerfTest`，`IAM_TENANT_LIVE_MYSQL_HTTP_TEST=true`，仅运行 `IamReadPerfTest.baseline`。本次临时目录为 `/private/tmp/ingot-iam-read-perf.vhDKWw`，包含 `benchmark.gradle`、夹具源码、`baseline.log` 和逐场景 `baseline.jsonl`。测试任务成功，结束时由夹具停止临时容器。

实施后以相同数据、身份、预热和采样口径复测。分别记录直接查库、冷读和热读目录。针对精确的批量 SQL 条数及目录缓存收益，用 Mapper 执行记录交叉核对全库计数。单轮本地 P95/P99 作为比较基准，发布时应在目标环境复测。
