# Phase 04 验证与切换指南

## 自动化验证（V1–V4）

| 任务 | 测试位置 | 覆盖点 |
|---|---|---|
| V1 资源隔离 | `SecurityEventWriteSemaphoreTest` | writer 许可=1、第二写入阻塞 |
| V1 背压 | `SecurityEventRecordingDispatcherTest` | 队列满 DROPPED、DURABLE fail-open |
| V2 幂等 | `MySqlSecurityEventStoreTest` | 同 eventId 重复批次走 UPSERT |
| V2 spool | `FileSpoolRecordQueueTest` | enqueue/claim/ack/nack |
| V3 retention | `MySqlSecurityEventRetentionHandlerTest` | 锁失败、积压让步、分批删除 |
| V3 查询 | `MySqlSecurityEventQueryRepositoryTest` | 游标分页、page 上限 200 |
| V4 兼容 | `LegacySecurityEventConfigMapperTest` | mode/target 映射 |

```bash
./gradlew :ingot-framework:ingot-security:ingot-security-recording:test \
  :ingot-framework:ingot-security:ingot-security-event-store-mysql:test \
  :ingot-framework:ingot-security:ingot-security-event-transport-feign:test \
  :ingot-service:ingot-security:ingot-security-provider:test
```

## 手工 E2E（V6）

1. PMS 登录成功/失败 → 中心 + shadow 本地 canonical 表有记录（legacy remote 时 legacy 表也有）
2. 改密/锁定 → DURABLE；spool 有 segment 滚动
3. 网关限流封禁 → ACCESS 事件、主链路不阻塞
4. `GET /actuator/securityrecording` → `shadowFailures=0`，队列使用率 < 50%

## 单一权威切换（V5）

### 中心化（推荐有 security 服务）

**PMS/Member Nacos**：

```yaml
ingot:
  security:
    event:
      enabled: true
      target: center
      shadow-targets: []
      source-module: ingot-pms  # 或 ingot-member
```

### 独立部署（无 security 中心）

```yaml
ingot:
  security:
    event:
      enabled: true
      target: local
      shadow-targets: []
      primary-store: mysql
      source-module: ingot-pms
```

### 验收

- 观察 24–72h：仅权威 `security_event` 表增长
- `account_security_event` 无新行（显式 target 后）
- spool 无持续积压；Actuator `failed`/`dropped` 无 unexplained 增长
- 执行 `databases/scripts/security_event_shadow_reconcile.sql`

## 回滚

1. 恢复 `mode=remote`（自动 shadow + legacy 表双写）或重新设置 `shadow-targets: [local]`
2. 禁止删除已写入 canonical 数据
3. 以 `eventId`/时间窗对账

## 后续 breaking cleanup（独立 change）

- 移除 legacy `mode`、`AsyncSecurityEventReporter`、`RemoteSecurityEventPortAdapter`
- `account_security_event` 表只读满 retention 后归档/下线
