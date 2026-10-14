# 需求

优化 `/v1/platform` 下 members、roles、roles/{id}/members、assignments、delegations、roles/{id}、roles/{id}/grants、roles/{id}/revisions、roles/{roleId}/version-applications 及其 distribution 十个 GET。

保持响应、分页总数、能力和拒绝原因、身份域、对象/字段/治理与委派边界一致。普通授权最长 30 秒读取窗口不变，实时资格不能使用该窗口替代。写入、事务内重验、worker、授权 RPC 和缓存 loader 不复用只读请求结果。故障 fail-closed。

解决重复求值、人员能力 N+1、版本历史 N+1 及逐任务逐状态计数。使用同一 SQL 可见范围，不加载全集、不缓存任务进度。

验收：正常 Run、本地相同数据及原 5799 入口，每接口预热 20 次、采样 100 次、3 轮，串行及三路并发每轮 P95 < 200ms；关闭 Debug agent、自动 EXPLAIN、全量 SQL 输出，冷态单独记录。功能和权限回归、1/20 行查询规模验证全部通过。

无 DDL、前端升级、新跨请求缓存、授权窗口调整。回滚恢复原 IAM 后端版本。
