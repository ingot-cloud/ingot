# 验证证据

## 自动化结果（2026-10-10）

| 验证 | 结果与覆盖 |
|---|---|
| IAM provider 全套 + commons | 411 + 64 = 475 项通过，零跳过、零失败；包含19项 MySQL/HTTP 用例 |
| 版本与范围 | 名称回退、元数据独立 CAS/只读系统角色、历史租户固定基础、字段权限深拷贝、参数键/类型/关联资源兼容、逐条保留及移除旧参数、历史降级与既有升级门禁 |
| 真实 MySQL 持久任务 | 205条直接/组/未来生效记录，100条分批、快照后新增不纳入、幂等、租约接管/旧节点 fencing、逐条审计；101条任务撤权暂停与显式继续；并发预检零更新、治理边界及计数一致 |
| 017 隔离 MySQL 8.4 | 2 项通过；从旧 DDL 升级、重复执行保留元数据/权限/分配、新增索引及同角色 RUNNING 唯一约束；容器无网络且自动清理 |
| 契约/数据库生成器 | 15项相关 Python 测试通过；数据库生成检查和两端 routes/schemas/openapi 同步 |
| 前端 | admin-common 128、platform 168、org 34，共330项通过；公共包及两个插件类型检查、完整平台管理台构建通过 |
| 页面 | 真实管理台组件 + API夹具浏览器用例通过：元数据编辑不发布、应用旧版本、205条分页、冲突禁止确认、100条成功后暂停与显式继续；复核明暗主题和480px窄屏布局 |

后端命令（离线依赖、测试启动隔离 MySQL）：

```sh
IAM_MYSQL_HTTP_TEST=true ./gradlew -I /private/tmp/ingot-role-test-memory.gradle :ingot-service:ingot-iam:ingot-iam-provider:test :ingot-framework:ingot-commons:test --offline
python3 databases/iam/test_role_revision_application_migration.py
PYTHONPATH=tools/iam python3 -m unittest test_contract test_database_sources test_generate_bootstrap
python3 tools/iam/generate_database.py --check
```

全套 MyBatis 测试的既有默认堆不足，使用临时 Gradle 初始化脚本设置测试堆1536m、forkEvery=10；未修改工程运行配置。无须此脚本即可运行单个相关测试。

前端使用项目要求的 Node22 / pnpm10.12.4，执行关联包 `test:unit`、`type-check`、`pnpm build:admin-platform`，最后公共组件变动后再次构建公共包与平台应用。浏览器源码为前端 `apps/admin-platform/e2e/role-version-application.spec.ts`；使用本机 Chrome 和临时 Playwright 配置连接5799开发服务，所有 `/api/` 写请求由夹具拦截，不修改现有数据库。测试断言与截图一并验证快速暂停竞态，异步结果不被角色详情刷新销毁。

## 验证边界与待验收

本次没有执行目标库迁移、部署或提交代码。数据库测试和HTTP测试使用隔离库；浏览器夹具不替代真实登录身份、委派候选和权限缓存的人工验收。jy 需按迁移README完成数据库→全部后端→前端，再验证实际范围补齐、委派白名单、跨节点任务和其他角色权限合并。

另有整套 Python 工具测试中的既有 ServiceNamingTest 两项依赖缺失的 `.gitlab-ci.yml`，不属于本次变更；相关契约/数据库15项独立运行通过。本次全套 Java、关联前端测试均通过。验收前状态保持 validating，不更新 current 或归档。

最终分层边界、文档检查、触及 TS/Vue 文件的 ESLint（零错误、零警告）及两端 git diff --check 均通过。页面截图留存于本次会话工作区 `role-version-application/role-application-{wide,dark,narrow}.png`。

## 人工验收与归档（2026-10-11）

负责人 jy 在会话中明确确认本次角色版本信息与批量应用功能人工测试完成，并要求归档及提交。该确认作为本能力整体人工验收结论，未另行声称逐项人工测试日志或生产部署记录。

归档前再次核对后端契约及数据库生成检查、相关 Python 15 项测试、两端契约镜像一致、前端文档检查及 Git 差异检查，均通过。此前业务自动化及构建记录保留；本次未修改业务代码。

关联代码提交为后端 `a6339cfe`、前端 `2d5e37e`。两端 current 已整理，任务 P5 完成，change 状态为 completed。角色版本功能归档时，后续角色应用调度优化的独立多节点验收仍待确认，未纳入该次验收。2026-10-11，jy 随后另行确认调度优化验收完成，见其 [独立验收记录](../20261011-iam-role-application-scheduling/EVIDENCE.md)。
