# 统一安全事件中心 SPEC

> 记录当前已验收并在线生效的系统事实。

## 1. 配置（`ingot.security.event`）

各服务独立 Nacos dataId（如 `in-service-pms.yml`、`in-service-member.yml`、`in-service-gateway.yml`、`in-service-security.yml`），结构共用。

### 1.1 上报开关与模式

| 配置 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | **总开关**。`false` 时本地与中心均不上报 |
| `mode` | `local` | 仅 `enabled=true` 时生效：`local` 仅业务库；`remote` 业务库 + 中心 |
| `source-module` | `unknown` | 写入中心 `security_event.source_module`（如 `ingot-pms` / `ingot-member`） |

**语义矩阵**：

| enabled | mode | 本地 `account_security_event` | 中心 `security_event` |
|---|---|---|---|
| `false` | — | 不写 | 不写 |
| `true` | `local` | 写 | 不写 |
| `true` | `remote` | 写 | 异步写（受 `categories.*` 控制） |

网关无本地业务表，仅 `mode=remote` 且 `categories.access=true` 时上报中心。

`categories.*` 在 `mode=local` 时不限制本地写入；在 `mode=remote` 时控制是否转发中心。

### 1.2 类别开关（`categories`）

| 键 | 默认 | 对应 `event_category` |
|---|---|---|
| `auth` | `true` | AUTH |
| `account` | `true` | ACCOUNT |
| `credential` | `true` | CREDENTIAL |
| `access` | `true` | ACCESS（网关） |

### 1.3 异步上报缓冲（`async`，`mode=remote` 时生效）

| 键 | 默认 | 说明 |
|---|---|---|
| `queue-capacity` | `2048` | 有界队列；满时丢弃新事件并限流 warn |
| `batch-size` | `32` | 单次 `reportBatch` 上限 |
| `poll-timeout-ms` | `100` | 消费者攒批 poll 超时 |
| `shutdown-timeout-ms` | `5000` | 优雅关闭排空超时 |

实现类：`AsyncSecurityEventReporter`（account-adapter 与 gateway 共用）。

### 1.4 Retention（与上报开关独立）

| 键 | 默认（代码） | 说明 |
|---|---|---|
| `retention.enabled` | `true` | 关闭则不物理删除 |
| `retention.days` | `30` | `0` = 永久保留 |
| `retention.batch-size` | `500` | 单批删除条数 |
| `retention.max-rounds` | `100` | 单次任务最多批次数 |

**推荐 Nacos 样例**：PMS/Member 本地 **90 天**；ingot-security 中心 **30 天**（可独立调大）。

| 服务 | 清理表 | 时间列 | 任务 cron |
|---|---|---|---|
| PMS / Member | `account_security_event` | `created_at` | `0 0 3 * * ?` |
| ingot-security | `security_event` | `received_at` | `0 30 3 * * ?` |

## 2. 统一契约（`ingot-security-api`）

### 2.1 P0 事件类型

```text
AUTH:        LOGIN_SUCCESS, LOGIN_FAILURE
ACCOUNT:     ACCOUNT_CREATED, ACCOUNT_ENABLED, ACCOUNT_DISABLED,
             ACCOUNT_LOCKED, ACCOUNT_UNLOCKED, ACCOUNT_DELETED
CREDENTIAL:  PASSWORD_CHANGED, PASSWORD_RESET, FORCE_CHANGE_PASSWORD
ACCESS:      BLACKLIST_BLOCK, RATE_LIMIT_VIOLATION,
             LOGIN_FAIL_IP_EXCEED, LOGIN_FAIL_DEVICE_EXCEED,
             LOGIN_FAIL_CLIENT_EXCEED, LOGIN_FAIL_ACCOUNT_IP_EXCEED
```

后四种由 L4 登录失败保护在达阈值封禁时上报（见 [access-protection](../access-protection/SPEC.md)）。

enum SoT：`SecurityEventType`、`SecurityEventCategory`（account-domain 既有 enum 通过 mapper code 直传）。

### 2.2 Feign 接口

```text
POST /inner/security/event/report       — 单条入库
POST /inner/security/event/report/batch — 批量入库
```

Feign：`RemoteSecurityEventService`；内网实现：`InnerSecurityEventAPI`（`@Permit(INNER)`）。

- 校验 `eventType` / `eventCategory` / `sourceModule` 非空。
- `occurredAt` 缺省时 `received_at = now()`。
- 本期不做 dedup；同事件可多条。

## 3. 中心数据模型

表：`ingot_security.security_event`（migration `010_unified_security_event.sql`）。

核心列：`event_type`、`event_category`、`occurred_at`、`received_at`、`tenant_id`、`user_id`、`user_type`（`ADMIN`/`APP`）、`client_ip`、`source_module`、`extension`（JSON）等。

索引：`received_at`、`(event_type, received_at)`、`(tenant_id, user_id)`、`trace_id`。

**网关旧表**：`gateway_blacklist_event` 历史只读；新事件不再 INSERT。`InnerSecurityPolicyAPI.reportBlacklist` 转调统一入库。

## 4. 账号域上报链路

```
UseCase → SecurityEventPort (CompositeSecurityEventPort)
            ├─ DefaultSecurityEventPortAdapter   同步 INSERT account_security_event
            └─ RemoteSecurityEventPortAdapter    有界队列 → AsyncSecurityEventReporter → Feign
```

- `SecurityEventPort` 唯一 Bean 入口；`DefaultSecurityEventPortAdapter` 不可单独注册为 Bean。
- `remotePort` 可空（无 Feign 时 `mode=remote` 仅写本地）。
- 本地 INSERT 失败仍抛异常；Feign 失败仅 warn，不阻塞 UseCase。

映射：`AccountSecurityEventReportMapper`（`eventType.code`、`userType.name()`、`extraData` → `extension`）。

## 5. 网关 ACCESS 上报

`BlacklistEventReporter`：`BlacklistReportDTO` → `SecurityEventReportDTO`（`BlacklistReportEventMapper`）。

- `sourceModule=ingot-gateway`，`eventCategory=ACCESS`。
- 自动限流触发：`RATE_LIMIT_VIOLATION`；封禁动作 B/U/R：`BLACKLIST_BLOCK` + `extension.action`。
- `RemoteSecurityEventService` 经 `ObjectProvider` 懒解析，避免 Sentinel 过滤链循环依赖。

## 6. 失败处理与降级

| 场景 | 行为 |
|---|---|
| `enabled=false` | 不上报（本地与中心均不写） |
| `mode=local` | 仅本地表 |
| security 未部署 / Feign 不可用 | 本地正常；中心 debug/warn 跳过 |
| 远程队列满 | 丢弃并 warn；不阻塞业务线程 |
| 中心 DB / RPC 失败 | warn；不重试阻塞 |
| retention 任务失败 | 不影响主链路 |

Nacos 热刷新：`@ConfigurationProperties` + refresh；每次上报读取当前 Properties。

## 7. 已知限制

- 无 Platform 读侧 API；验收与运维依赖 DB 直查。
- 无历史从 `account_security_event` / `gateway_blacklist_event` 回填。
- 远程上报队列满时事件丢弃，无持久化 dead-letter。
- P2 单元测试（Composite 分支、映射器）未在本 change 全量补齐；以集成 / 手工验收为准。

## 8. 迁移

- 执行顺序：migration `010` → 上线 ingot-security → PMS/Member（默认 `mode=local`）→ 灰度 `mode=remote` → Gateway Reporter。
- 回滚：Nacos `enabled=false` 或 `mode=local`；可选 `rollback_010.sql`（无生产数据时）。
