# 实施与验证证据

## 实现范围

2026-10-11 完成共用流程与平台规则分离。生产仅新增内部 `RoleApplicationDomainSupport` 和 `PlatformRoleApplicationDomainSupport`，没有注册中心、业务域选择器、通用任务模块或新持久化上下文。

共用服务保留状态、租约和事务；worker 通过 `RoleApplicationWork` 注入。平台适配器沿用原查询、门禁及授权→角色锁顺序，读取目标版本仍只有一次原查询，工作线程每次恢复身份和校验当前资格。另有内存身份域检查，不增加数据库访问。

测试增加 TSS Spring 的 testImplementation，使 IAM 装配测试能够使用真实 TSS 自动配置；没有增加生产依赖或 XXL-JOB 依赖。补充 [IAM 运行说明](../../../../../docs/modules/iam/role-application-worker.md) 和 [TSS 架构边界](../../../../../docs/modules/task-scheduler-system/tss-architecture.md)。

## 自动化结果

| 检查 | 结果 |
|---|---|
| 角色应用专项单元与装配 | 27 项，通过；包含原 15 项及新增 12 项 |
| IAM 完整回归 | 442 项，失败/错误/跳过均为 0；启用隔离 MySQL/HTTP 场景 |
| TSS Spring 回归 | 2 项，失败/错误/跳过均为 0 |
| 契约生成检查 | 152 路径、222 操作，生成内容未变 |
| 数据库生成检查 | 初始化包一致，无 DDL 或迁移变更 |
| Python 契约/数据库回归 | 15 项，通过 |
| 静态边界、Markdown 链接及 git diff --check | 通过 |

新增平台适配器测试覆盖非治理授权、非平台身份、身份不可用、当前治理资格撤销、普通读与 fresh 范围、角色 READ 边界、非自定义角色、不同角色目标、锁顺序及不增加身份/角色查询。

共用流程使用测试替代适配器验证计划不固定平台角色种类，批次使用适配器恢复身份和构造版本引用；撤权回滚执行事务、保存暂停且不调用分配写入。该替代只在测试中使用，不代表实现了租户接口或租户任务上下文。

实际 Spring 装配验证唯一平台适配器及工作接口实现。TSS 共存测试确认没有注册角色 Cron 任务，关闭 TSS 调度池后角色唤醒仍使用独立执行线程；TSS 类型配置不切换 worker。`xxljob` 配置用例只验证配置边界，未启动 XXL-JOB Admin 或真实执行器。

既有 MySQL/HTTP 23 项通过，复用生产适配器与持久化：包含 205 条按 ID 分批、两节点真实争抢及过期租约接续、长事务阻止过期节点重复写入、提交/回滚唤醒边界、范围与委派、权限降级、撤权暂停及重启恢复。仅使用自动清理的隔离测试容器，不连接现有 IAM 库。

## 命令与环境

```sh
./gradlew -I /private/tmp/ingot-role-test-memory.gradle :ingot-service:ingot-iam:ingot-iam-provider:test --tests '*RoleApplication*Test' --offline
IAM_MYSQL_HTTP_TEST=true ./gradlew -I /private/tmp/ingot-role-test-memory.gradle :ingot-service:ingot-iam:ingot-iam-provider:test :ingot-framework:ingot-tss:ingot-tss-spring:test --offline
python3 tools/iam/build_contract.py --check
python3 tools/iam/generate_database.py --check
PYTHONPATH=tools/iam python3 -m unittest test_contract test_database_sources test_generate_bootstrap
git diff --check
```

使用 Java 21 和已有离线 Gradle 依赖。沿用既有 MyBatis 全量回归的临时测试堆 1536m、forkEvery=10，不修改生产 JVM 设置。专项日志 `/private/tmp/ingot-role-domain-unit.log`；完整回归日志 `/private/tmp/ingot-role-domain-regression.log`，完整回归耗时 1 分 12 秒。

静态核对：共用服务无 PLATFORM 常量、角色种类判断及平台服务/Repository 依赖，`IamAccess` 仅用于发号；生产 Repository、调度器、通知器和配置属性未修改。适配层不新增事务或缓存。性能结论限定为查询及调度路径保持一致，没有进行生产负载基准测试。

## 人工验收与完成

2026-10-11，用户报告“更新角色权限范围，应用到当前匹配角色”人工测试成功。在确认内部扩展及 TSS 边界后，用户明确要求归档；结合上述自动化结果，本次验收完成。已更新 `iam/role-version-application` current，change 状态为 completed 并移入 archive，未创建 commit。

该人工结果覆盖角色范围更新并应用的主链路；多节点、租约接续、撤权暂停、历史任务及 TSS 共存的证据来自自动化回归，不声明另行完成人工全量测试。Agent 没有部署或重启实际 IAM 节点，没有真实 XXL-JOB Admin 集成或生产负载基准测试。

归档后用户明确要求提交相关代码，实现、测试、运行说明及本次 Spec 一并提交，关联提交标题见 README。提交准备仅更新归档元数据，没有改变已验收代码。
