# 验证证据

## 自动化结果（2026-10-11）

| 验证 | 结果 |
|---|---|
| 完整 IAM provider 回归 | 430项通过，零失败、零跳过；包含23项隔离MySQL/HTTP测试 |
| commons 回归 | 64项通过，公开契约未变化 |
| 新增调度/通知/装配测试 | 15项通过；最终配置与队列Gauge修正后再次运行通过 |
| 契约/数据库生成检查 | 152路径、222操作及IAM初始化包检查通过，未增加接口或DDL |
| 相关Python契约/数据库测试 | 15项通过 |
| git diff --check | 通过 |

虚拟时钟覆盖两个节点空闲10分钟（首节点0秒启动11次查询、次节点30秒错峰10次），期间无按ID执行；本地唤醒立即运行，250毫秒续批且不查全局集合；重复提示去重、多任务交替、执行结束时到达的新提示、容量溢出后扫描恢复、远端错峰、领取竞争停止、60/120/240/300秒失败退避、成功后恢复60秒、关闭取消新调度。

通知测试覆盖先本地入队再异步广播、网络异常隔离、没有总线、本节点worker关闭仍广播、合法ID校验、订阅取消及发送队列溢出。真实Spring装配测试覆盖默认配置、关闭worker、事件自动接线，以及非法Duration/容量阻止启动。

新增四项MySQL用例使用生产持久化、锁、任务服务及真实事务：

- 确认外层事务未提交/回滚不唤醒，提交后仅一次事件，幂等重放无新事件。
- 205条分配从提交提示开始按ID执行100/100/5，零集合发现调用、205条审计。
- 两节点真实并发争抢同一任务，仅一个完成首批100；强制数据库租约到期后接管剩余5，旧节点不重复更新，105条审计。
- 将剩余批次放在未提交的长事务中，另一节点即使读到过期的已提交租约也须等待原锁；提交后不能重复更新，105条审计。

原有重启接续、撤权暂停、历史降级、预检零更新及治理范围回归同时通过。MySQL仅使用自动清理的隔离容器，不连接现有IAM库。

## 命令及执行环境

```sh
IAM_MYSQL_HTTP_TEST=true ./gradlew -I /private/tmp/ingot-role-test-memory.gradle :ingot-service:ingot-iam:ingot-iam-provider:test :ingot-framework:ingot-commons:test --offline
./gradlew -I /private/tmp/ingot-role-test-memory.gradle :ingot-service:ingot-iam:ingot-iam-provider:test --tests '*RoleApplication*Test' --offline
python3 tools/iam/build_contract.py --check
python3 tools/iam/generate_database.py --check
PYTHONPATH=tools/iam python3 -m unittest test_contract test_database_sources test_generate_bootstrap
git diff --check
```

使用Java21及已有离线Gradle依赖；延续原MyBatis全套测试的临时测试堆1536m/forkEvery=10，不修改工程或生产JVM设置。完整回归日志 `/private/tmp/ingot-role-scheduling-regression.log`（1m13s）、最终调度专项 `/private/tmp/ingot-role-scheduling-unit-final.log`；初次MySQL专项 `/private/tmp/ingot-role-scheduling-mysql.log`。

## 验收边界

实施及自动化结束时状态为 validating，等待实际环境验收。运行配置及双节点验收步骤见 [运行说明](OPERATIONS.md)。Agent 未启动/重启实际 IAM 节点或执行目标库操作；没有前端修改或新 SQL。调度实现已在后续代码提交 `a6339cfe` 保存。

## 人工验收与归档（2026-10-11）

负责人 jy 在会话中明确确认当前调度 Spec 已验收完成，并要求归档及提交。以该确认为本 change 的人工验收结论，不另行声称 Agent 执行了实际节点故障演练或掌握逐项人工日志。

本次只整理文档和基线，未修改业务代码；保留上述 494 项完整回归、15 项调度专项及隔离 MySQL 证据。归档前检查 Markdown 相对链接、目录与任务状态以及 Git 差异格式。current 已补齐调度配置、唤醒、分页发现、多节点租约、故障退避和运行指标，任务 P6 完成，状态为 completed。
