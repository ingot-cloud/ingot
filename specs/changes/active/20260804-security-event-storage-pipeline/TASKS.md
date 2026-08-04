# Tasks

## 规格审阅

- [x] T0：确认 REQUIREMENTS、DESIGN 与关键决策 D1-D7
  - 依赖：无
  - 验收：README 审阅门禁全部关闭；本文件无待决实现选择；状态由 `review` 改为 `approved`

## Phase 01：统一契约与运行时

- [x] T1：新建 `ingot-security-recording` module 与公共模型/SPI
  - 依赖：T0
  - 内容：`SecurityEventRecord`、`AuditRecord`、Publisher/Store/Query/Transport/RecordQueue/Retention contract、capabilities 与 outcome 类型
  - 验收：模块无 MySQL/Feign/ES 依赖；公共 API 与 DESIGN 一致；Java 类型级 Javadoc 合规

- [x] T2：实现 dispatcher、事务 afterCommit、优先级与路由
  - 依赖：T1
  - 内容：BEST_EFFORT 内存队列、DURABLE 队列 seam、target/shadow、失败策略、优雅关闭
  - 验收：事务提交/回滚、local/center/shadow、队列满和 fail-open 单测通过

- [x] T3：实现自动配置、旧配置映射与可观测
  - 依赖：T2
  - 内容：properties、单 Store 自动选择、多 Store/无目标 fail-fast、旧 mode/async/retention 映射、metrics/health
  - 验收：装配矩阵全覆盖；日志限流；热刷新与需重启属性边界符合 DESIGN

## Phase 02：存储与可靠投递

- [ ] T4：实现 file segment engine 与 DURABLE spool
  - 依赖：T2
  - 内容：append/checksum/claim/ack/nack、segment 滚动、配额、重放、退避、quarantine、恢复工具
  - 验收：重启、截断、损坏、满盘、ack 前崩溃和重复重放测试通过；未 ack 记录不被覆盖

- [ ] T5：实现 `ingot-security-event-store-log`
  - 依赖：T4
  - 内容：结构化 JSONL 最终 Store、滚动、retention、磁盘上限与 capabilities
  - 验收：可作为唯一 local Store；不注册 QueryRepository；文件 retention contract 通过

- [ ] T6：实现 `ingot-security-event-store-mysql`
  - 依赖：T1, T2
  - 内容：canonical entity/mapper、真实 batch、eventId 幂等、semaphore、可选独立 DataSource、事务超时
  - 验收：批量不循环单条 Service；writer 并发连接不超过许可；唯一键重投视为成功

- [ ] T7：实现 MySQL QueryRepository 与 RetentionHandler
  - 依赖：T6
  - 内容：条件查询、cursor page、批量删除、命名锁、时间预算、积压让步
  - 验收：默认/最大分页、31 天范围、30 天/永久 retention、多实例互斥与写入优先测试通过

- [ ] T8：完成 additive 数据库 migration
  - 依赖：T6
  - 内容：中心表新增 `event_id`/`priority`/索引；core/member 新增 canonical `security_event`；回滚说明与基线草案
  - 验收：新旧版本兼容读取；历史 NULL eventId 合法；migration 可重复执行；不删除旧表

## Phase 03：消费者与中心迁移

- [ ] T9：迁移安全中心 ingest
  - 依赖：T3-T8
  - 内容：DTO 扩展、校验、admission queue/spool、异步 Store worker、批次上限与可重试响应
  - 验收：HTTP 线程不执行 Store INSERT；DURABLE 接纳/拒绝和 BEST_EFFORT 丢弃语义通过集成测试

- [ ] T10：迁移账号域 PMS/Member
  - 依赖：T9
  - 内容：Account event 映射、Publisher 适配、target 路由、旧 Composite 兼容、Nacos 配置
  - 验收：同一制品 local/center 均可装配；事件失败不回滚账号事务；旧 mode 行为兼容

- [ ] T11：迁移 Gateway ACCESS Reporter
  - 依赖：T9
  - 内容：统一 Publisher、优先级、Feign Transport；移除独立 AsyncSecurityEventReporter 运行路径
  - 验收：限流/封禁不被上报阻塞；ACCESS eventId/priority/extension 正确

- [ ] T12：执行 shadow 双写与字段对账
  - 依赖：T10, T11
  - 内容：local 与 center shadow、计数/字段/延迟/drop/shadow failure 指标和对账脚本
  - 验收：约定流量窗口内无未解释字段差异；shadow failure 为零或有已接受处置记录

## Phase 04：验证与切换

- [ ] V1：资源隔离与攻击压测
  - 依赖：T12
  - 验收：队列满、慢库、连接耗尽和持续攻击下，事件 writer 连接不超过许可，正常业务连接仍可获取

- [ ] V2：故障恢复与幂等验证
  - 依赖：T12
  - 验收：中心/DB 停机恢复、进程 kill、ack 前崩溃、spool 损坏与重放均符合 DESIGN，无 eventId 重复行

- [ ] V3：Retention 与查询验证
  - 依赖：T7, T12
  - 验收：cursor 查询、查询上限、多实例互斥、积压让步、30 天/永久与文件清理通过

- [ ] V4：旧配置与回滚演练
  - 依赖：T12
  - 验收：旧配置矩阵、新配置优先、target 回退、旧 Composite 回退与 spool 保留流程通过

- [ ] V5：切换单一权威存储
  - 依赖：V1-V4
  - 内容：清空 `shadow-targets`，按部署选择 local 或 center，停止旧 `account_security_event` 新写入
  - 验收：观察窗口内只有权威 Store 增长；旧表只读/retention 正常；无持续 spool 积压

- [ ] V6：编译、自动化与手工 E2E
  - 依赖：V5
  - 验收：相关 Gradle modules 编译/测试通过；ADMIN/Member 登录成功失败、账号/凭证事件、Gateway ACCESS E2E 通过

## 完成检查

- [ ] 实现与 DESIGN 一致，偏差已先回写并重新确认
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] `specs/current/security/security-event-center/` 已按最终实现更新
- [ ] 新增 recording framework current capability（若最终模块边界保持）
- [ ] 旧表/旧 mode 的后续破坏性清理已单独建 change 或明确记录
- [ ] README 完成记录已填写，Change 状态为 `completed` 并归档

## 任务依赖图

```text
T0 -> T1 -> T2 -> T3
             \-> T4 -> T5
T1,T2 ----------> T6 -> T7
                         \-> T8
T3-T8 -> T9 -> T10,T11 -> T12 -> V1-V4 -> V5 -> V6 -> current/archive
```
