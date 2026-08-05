# Requirements

## 用户场景

### S1 攻击流量下保护业务数据库

- 使用者：PMS、Member、Gateway 与安全中心的正常业务请求。
- 触发：恶意登录、批量扫描或访问违规持续产生大量安全事件，或事件数据库变慢/不可用。
- 期望：事件写入不占满业务连接池、不长期阻塞业务线程，也不因事件持久化失败回滚账号状态或正常业务事务；丢弃、积压和降级均可观测。

### S2 独立部署使用本地 MySQL

- 使用者：未部署安全中心的 PMS/Member 环境。
- 触发：制品依赖 MySQL Store 且配置 `target=local`。
- 期望：安全事件通过统一 Publisher 写入本服务 `security_event`，并可通过统一 QueryRepository 查询；无需 Feign 或 `mode` 判断存储技术。

### S3 中心化部署使用中心权威库

- 使用者：已部署安全中心的环境。
- 触发：同一 PMS/Member 制品配置 `target=center`。
- 期望：事件经 Transport 送达安全中心，由安全中心依赖的 MySQL Store 保存；PMS/Member 不再默认写本地事件表，中心不可用时关键事件进入本地 spool 并在恢复后重放。

### S4 替换最终存储实现

- 使用者：部署与架构维护者。
- 触发：未来安全中心从 MySQL Store 替换为 ES Store。
- 期望：业务 Publisher、PMS/Member、Feign payload 和安全事件领域模型不变，仅替换 Store module 与对应部署配置。

### S5 显式关闭与测试 NoOp

- 使用者：测试、轻量部署或临时运维。
- 触发：`enabled=false` 或显式选择 `noop`。
- 期望：事件被明确标记为禁用；生产配置在 `enabled=true` 且无 Store/Transport 时启动失败，不允许静默 NoOp。

### S6 分级可靠投递

- 使用者：安全运营与系统维护者。
- 触发：事件队列满、远端不可用、进程重启或投递结果不确定。
- 期望：高频 best-effort 事件可受控丢弃；账号、凭证、封禁和阈值超限等 durable 事件先写 file spool、按 at-least-once 重放，并由 `eventId` 消除传输重试重复。

### S7 统一查询与 Retention

- 使用者：后续安全查询 API 与运维任务。
- 触发：MySQL Store 被选为权威存储。
- 期望：可按时间、类型、租户、用户、来源、trace 游标分页查询；事件默认保留 30 天，分批清理不与写入争抢数据库资源；`days=0` 永久保留。

### S8 未来业务审计复用底座

- 使用者：后续业务审计 change。
- 触发：接入管理员操作、策略变更或敏感操作审计。
- 期望：审计使用独立模型、Store 与 QueryRepository，但复用队列、file spool、幂等、retention、失败策略和指标；默认保留 180 天。

## 业务规则

### P0 架构与装配

1. Publisher、Transport、Store、QueryRepository 与 RecordQueue 必须分层，`remote` 不是 Store，MySQL/ES 不是 Transport。
2. Module 依赖决定可用实现；仅有一个 Store 时自动选择，同时存在多个 Store 时必须显式配置 `primary-store`，否则启动失败。
3. PMS/Member 标准制品同时具备 MySQL Store 与 Feign Transport，通过 `target=local|center` 选择拓扑。
4. 同时存在 Store 与 Transport 不得自动双写；只有 `shadow-targets` 可开启迁移双写。
5. 稳定态每条记录只有一个权威存储；本地日志/fallback 不属于第二查询事实源。
6. `enabled=true` 且目标不可装配时必须 fail-fast；NoOp 只能显式启用。

### P0 主链路隔离

7. 事务内产生的事件必须在事务成功提交后发布；事件失败不得回滚业务事务。无事务场景立即发布。
8. MySQL 写入使用单 writer、真实批量 INSERT、短事务与数据库 semaphore，默认并发许可为 1。
9. retention 与事件 writer 共用资源舱壁；积压超过阈值时 retention 必须让步。
10. 安全中心 HTTP ingest 不得在请求线程逐条 INSERT；应答表示已按优先级规则接纳，而非保证查询立即可见。

### P0 投递可靠性

11. `LOGIN_SUCCESS`、`LOGIN_FAILURE`、原始 `RATE_LIMIT_VIOLATION` 默认 `BEST_EFFORT`。
12. ACCOUNT、CREDENTIAL、`BLACKLIST_BLOCK` 与四种 `LOGIN_FAIL_*_EXCEED` 默认 `DURABLE`。
13. BEST_EFFORT 队列满时非阻塞丢弃并计数；DURABLE 事件必须先进入 file spool，再向目标投递。
14. file spool 达到磁盘上限时不得覆盖未确认记录；默认放行业务并严重告警，高风险审计未来可按动作配置 fail-closed。
15. 投递语义为 at-least-once；producer 生成稳定 `eventId`，Store 必须幂等。
16. 不按用户、IP、事件类型或时间窗口做业务 dedup；攻击聚合、采样和告警窗口属于后续风险/告警能力。

### P0 查询、Retention 与审计边界

17. 写入与查询 SPI 分离；MySQL/未来 ES 实现查询，日志与 NoOp 明确声明不支持查询。
18. 查询使用游标分页，禁止无界列表；最大页大小为 200，默认 50。
19. 安全事件 retention 默认 30 天，审计默认 180 天，`days=0` 为永久保留。
20. MySQL retention 必须分批、限时、实例互斥，并使用时间+主键游标；未来 ES 使用 ILM，不能由应用运行大范围 `delete_by_query`。
21. 安全事件记录系统运行事实；审计记录 actor 对 target 的管理操作。两者不得共用一个万能查询模型或一张混合表。
22. extension、审计 before/after 等扩展数据必须脱敏、限制长度和总 payload 大小，不记录密码、令牌、密钥等秘密值。

## 边界与非目标

- 登录失败计数、滑动窗口、账号锁定状态仍由访问保护和账号域负责；本 change 只隔离事件记录带来的数据库压力。
- 本期不要求所有高频安全事件零丢失；容量耗尽时以业务可用性优先，同时必须暴露丢弃数量。
- 本期不实现 ES、Kafka、Platform 查询控制器、导出、安全大盘、风险规则、告警业务或对象存储冷归档。
- 本期不实现业务审计 producer、审计表和审计 UI；只定义契约与默认规则。
- 不在线回填历史 `account_security_event.event_id`，不自动把本地历史数据汇总到中心。
- 不在本 change 删除旧表、旧 `mode` 或旧接口；破坏性清理由后续 change 承担。

## 验收标准

- [ ] S1：攻击/慢库测试中，事件 writer 占用数据库连接不超过配置许可，Publisher 不等待业务连接池。
- [ ] S2：`target=local` 时仅本地 canonical `security_event` 增长，查询 SPI 可读。
- [ ] S3：`target=center` 时仅中心权威表增长；中心恢复后 durable 事件自动重放。
- [ ] S4：Store contract test 证明 MySQL 可被另一 Store 实现替换，业务 Publisher 无实现依赖。
- [ ] S5：显式 NoOp 生效；无目标的生产配置启动失败。
- [ ] S6：队列满、重启、ack 前崩溃和重复投递均符合优先级与幂等规则。
- [ ] S7：查询分页、30 天 retention、`days=0`、任务让步和多实例互斥通过测试。
- [ ] S8：审计模型/SPI 可编译并复用 recording runtime，但无业务审计 producer 被引入。
- [ ] 旧 `enabled/mode/categories/async/retention` 配置兼容测试通过；新配置优先且输出弃用告警。
- [ ] shadow 对账通过后关闭旧本地写入，稳定态只有一个权威 Store。
