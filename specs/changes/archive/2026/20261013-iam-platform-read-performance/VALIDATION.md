# 验证

## 自动化

2026-10-13：IAM 全量 83 类、474 个测试通过，0 失败、0 跳过。包括真实 MVC + 方法安全代理、请求隔离/身份/到期/故障/写事务/原始 loader、字段投影、平台治理与委派、租户回归，以及隔离 MySQL HTTP 的 23 个用例（任务执行、租约、审计和撤权）。

命令：`IAM_MYSQL_HTTP_TEST=true ./gradlew -I /private/tmp/iam-perf-test-init.gradle :ingot-service:ingot-iam:ingot-iam-provider:test --console=plain`。临时 init 只调整测试 JVM（768m、每 6 类换进程、单进程），避免既有静态 MyBatis 夹具在默认堆中累积；未修改构建或部署配置。

真实 JDBC 查询计数：人员 1/20 行能力查询数一致且最多 3（空操作范围不查）；历史 1/20 行含页边界上一版本，grants/deltas/parameters 各 1 次；任务 1/20 行计数各 1 条 GROUP BY，范围内每状态 1、全部范围每状态 2，缺失状态补零，后续状态变更实时反映。任务列表 20 行仅一次目标版本批量和一次批量计数，空页均不查。

## 性能

原 Debug 环境包含 Debug agent、自动 EXPLAIN 和 SQL 控制台捕获，不作为 Run 验收。2026-10-13 临时目录曾编译 HEAD 原 IAM 类作为 Run 基线，Agent 当日未重启服务或改变配置。

保留复测工具 [benchmark.py](benchmark.py)：基线与优化版同一环境、数据、账号、5799 入口，20 预热 + 100 采样 + 3 轮，串行及三路并发；报告仅含耗时、响应 SHA256 和行数，无凭证或响应正文。脚本拒绝 401/错误业务响应，不能以鉴权失败的快速响应冒充达标。

2026-10-13，Agent 因平台会话过期未完成原入口性能测量，未生成基线/优化版 P95 报告。

## 人工验收与结项

2026-10-14，用户明确“测试完成，归档当前spec，并且提交代码”，确认本 change 验收完成并授权归档及提交。以该确认完成原先待人工验收的 V2；未提供逐接口数值或采样报告，不将其描述为 Agent 独立压测结果，不声明已独立验证每轮 P95 < 200ms。

本次按最终代码更新平台读取与角色版本应用 current 基线，状态 validating → completed，并保留全部工件归档。其余 active change 的状态和验收不变。
