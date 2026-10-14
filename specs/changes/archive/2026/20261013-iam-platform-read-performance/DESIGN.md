# 设计

## 请求鉴权

IAM 内部标记十个同步 GET。MVC interceptor 在方法鉴权前启用请求属性上下文，afterCompletion/异步移交清理。每次访问核对认证对象和完整 AuthorizationContext；非 GET、非 REQUEST dispatch、写事务、无认证或身份不同不复用。

上下文独立保存有效身份、改密状态、普通授权视图与实时授权视图。视图到期重载，异常不保存。普通来源继续现有 layered cache；注解、capabilities、UPGRADE 使用实时来源。过滤器在线会话/改密门禁原样；缓存 loader 使用不复用的原始求值。后台与写入原样实时校验。

## 批量查询

平台人员以当前页 ID 对更新/状态/移除分别编译原范围，最多三条 SELECT ID 查询，包含 REMOVED 过滤，保留能力理由和字段投影。租户能力与实际写入不改变。

历史复用 pageRevisions 行，取得页边界上一版本，所有版本的 grants/deltas/parameters 各 IN 批量查询一次；按版本归组并使用原差异逻辑。角色定义读取与摘要能力装配分离，依赖读取不装配丢弃的按钮能力。当前 grants 复用 latest 行，保留空 grants + base 的原合成条件及同版本摘要。

应用域端口新增 findRevisions(ids)。任务页目标版本批量读取一次，明细按 task_id/status 聚合一次；统计复用原 assignment_id 范围谓词，七状态补零、空页不查、总数不变。单任务统计委托批量接口。任务写事务、锁、租约、快照和审计不变。

现有 revision 子表主键及任务索引支持查询，无迁移或新增配置。
