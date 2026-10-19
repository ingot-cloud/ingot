# 2026年10月19日提交检查

用户授权先提交相关内容，再逐条人工验证。两端change保持validating，不把本地提交当作验收完成；未推送、未更新current、未访问实际库。

## 提交范围

- 后端代码：dfe4da45，租户动态拥有者、共享/私有当前角色、稳定分配、资源委派及IAM/SDK/旧快照执行；021/022、一键开发准备和迁移工具。
- 当前HEAD已包含部门模型演进，提交保留必要的PolicyService投影构造对齐。生成路由/schema/OpenAPI同步当前已提交的IAM接口。
- 前端主体734bca6、规格和人工清单f92edf2已存在；本次离线图标261d2bd，提交登记9a3d650。
- 其他成员筛选测试和成员时间说明改动不纳入本次提交。

## 本次重新执行

| 检查 | 结果 | 证据 |
|---|---|---|
| IAM主代码及测试编译 | 通过 | [Gradle日志](./evidence/precommit-20261019-gradle.log) |
| RemoteAuthorizationTest | 7通过 | 同上 |
| AuthorizationSnapshotAccessTest | 2通过 | 同上 |
| 契约/数据库来源/生成器 | 18通过 | [工具日志](./evidence/precommit-20261019-tools.log) |
| 一键开发准备MySQL | 3通过，43.0秒 | [开发准备日志](./evidence/precommit-20261019-dev.log) |
| 增量迁移/备份恢复/逆转换MySQL | 5通过，179.1秒 | [迁移日志](./evidence/precommit-20261019-migration.log) |
| 前端图标JSON、两端接口副本及diff格式 | 通过 | 本次静态检查 |

## 旧结构测试可复跑

迁移与开发准备测试原先从git HEAD读取旧结构；本次提交会改变HEAD，因此将002/003/004固定为pre_021夹具，身份/辅助结构使用当前manifest。测试不再依赖变动的HEAD或额外历史提交。没有变更生产授权语义。

10月14日的全量、平台HTTP、浏览器和性能报告继续作为当日记录；本次仅重跑上表定向检查，没有把历史结果改写成10月19日重新全量执行。人工验收按前端MANUAL-ACCEPTANCE逐项填写。
