# 功能测试清单：安全事件 Legacy 清理

> change: `20260806-security-event-legacy-cleanup`  
> 环境：DEV/TEST 全栈（Nacos + MySQL + Redis + Gateway + PMS + Member + Security）

## 0. 前置检查

- [ ] 已执行 migration `012`（`ingot_core` / `ingot_member` / `ingot_security` 均有 canonical `security_event` 表）
- [ ] Nacos 已刷新：`ingot.security.event.target` 生效（PMS/Member/Security=`local`，Gateway=`center`）
- [ ] 各服务启动无 Bean 冲突；`GET /actuator/health` 均为 UP
- [ ] `GET /actuator/securityrecording` 可访问（需 actuator 暴露）

---

## 1. 配置解析

| # | 步骤 | 期望 |
|---|---|---|
| 1.1 | PMS 启动后读 `securityrecording` | `primaryTarget=LOCAL`，无 `legacyModeUsed` 字段 |
| 1.2 | Gateway 读 `securityrecording` | `primaryTarget=CENTER` |
| 1.3 | Security 中心读 `securityrecording` | `primaryTarget=LOCAL`，`primary-store=mysql` |
| 1.4 | 故意配置 `target=invalid` | 服务启动失败并报 Unknown recording target |

---

## 2. PMS 账号事件（target=local）

| # | 操作 | 验证 SQL / 观测 |
|---|---|---|
| 2.1 | 错误密码登录 1 次 | `ingot_core.security_event` 新增 `LOGIN_FAILURE`（AUTH），**`account_security_event` 无新行** |
| 2.2 | 正确密码登录 | `security_event` 新增 `LOGIN_SUCCESS` |
| 2.3 | 连续失败触发锁定 | `ACCOUNT_LOCKED`（ACCOUNT，DURABLE）写入 `security_event` |
| 2.4 | 管理员解锁账号 | `ACCOUNT_UNLOCKED` 写入 |
| 2.5 | 重置/修改密码 | `PASSWORD_RESET` 或 `FORCE_CHANGE_PASSWORD`（CREDENTIAL）写入 |
| 2.6 | 创建/删除用户 | `ACCOUNT_CREATED` / `ACCOUNT_DELETED` 写入 |
| 2.7 | 检查 `event_id` | 非空、同事件重试不重复（幂等） |
| 2.8 | 检查 `source_module` | 值为 `PMS`（或 Nacos 配置值） |

---

## 3. Member 账号事件（target=local）

重复 §2 全部用例，库改为 `ingot_member.security_event`，`source_module=MEMBER`，`user_type=APP`。

---

## 4. 网关 ACCESS 事件（target=center）

| # | 操作 | 验证 |
|---|---|---|
| 4.1 | 触发限流/违规封禁（Sentinel 达阈值） | `ingot_security.security_event` 新增 ACCESS 类事件（如 `BLACKLIST_BLOCK`） |
| 4.2 | 检查 `source_module` | `GATEWAY` |
| 4.3 | 网关本地库 | 无 `security_event` 表写入（仅 Feign 上报中心） |
| 4.4 | `categories.access=false` 后重试 | 不产生新 ACCESS 事件 |

---

## 5. 中心 Admission

| # | 操作 | 验证 |
|---|---|---|
| 5.1 | 直接 POST `/inner/security/event/report`（合法 DTO） | HTTP 200；异步写入 `security_event` |
| 5.2 | 缺必填字段 | 4xx + `SEC_EVENT_*` 错误码 |
| 5.3 | 重复 `eventId` | 幂等：不重复行 |
| 5.4 | `InnerSecurityPolicyAPI` `/inner/security/blacklist/report` | 与 §4 等价的 ACCESS 事件经 admission 入库（不再直写旧 Service） |

---

## 6. enabled=false

| # | 操作 | 验证 |
|---|---|---|
| 6.1 | Nacos 设 `ingot.security.event.enabled=false`，重启 PMS | 登录/改密后 **`security_event` 无新行** |
| 6.2 | 恢复 enabled=true | 事件恢复写入 |

---

## 7. 优先级与队列

| # | 操作 | 验证 |
|---|---|---|
| 7.1 | 登录失败（BEST_EFFORT） | `priority=BEST_EFFORT` |
| 7.2 | 改密/锁定（DURABLE） | `priority=DURABLE` |
| 7.3 | 压测大量 LOGIN_FAILURE | `securityrecording.dropped` 可能增长；业务线程不阻塞 |
| 7.4 | DURABLE 在 spool 配置下 | 中心不可用时本地 spool 有文件；恢复后 `replayed` 增长 |

---

## 8. Retention

| # | 操作 | 验证 |
|---|---|---|
| 8.1 | 插入 `received_at` 早于 retention.days 的测试行 | 手动触发或等待 `PurgeCanonicalSecurityEventTask`（03:30）后删除 |
| 8.2 | 确认无 `AccountSecurityEventRetentionTask` 日志 | 旧 account 表 retention 任务已移除 |
| 8.3 | 写入队列积压 >50% 时触发 retention | handler 让步，不删或少量删（见日志） |

---

## 9. 观测指标

| # | 端点/指标 | 期望 |
|---|---|---|
| 9.1 | `GET /actuator/securityrecording` | 含 `published/accepted/persisted/dropped/failed` |
| 9.2 | Micrometer `ingot.security.event.*` | 与端点计数一致趋势 |
| 9.3 | 无 `legacyModeUsed` 字段 | 已删除 legacy 映射 |

---

## 10. 回归：账号保护（与事件解耦）

| # | 操作 | 验证 |
|---|---|---|
| 10.1 | 锁定/解锁 | `account_lock_state` 仍正确更新 |
| 10.2 | 登录失败计数 | 窗口期内计数准确，与事件写入无关 |

---

## 11. 负向 / Legacy 已删除

| # | 检查项 | 期望 |
|---|---|---|
| 11.1 | 配置 `mode: remote` | **不再生效**（须用 `target: center`） |
| 11.2 | `account_security_event` | 新操作不产生行 |
| 11.3 | 代码库 | 无 `AsyncSecurityEventReporter`、`RemoteSecurityEventPortAdapter` 运行路径 |
| 11.4 | 中心旧 `SecurityEventRetentionTask`（03:30 前旧实现） | 不存在；仅 `PurgeCanonicalSecurityEventTask` |

---

## 12. 自动化测试（CI）

```bash
./gradlew :ingot-framework:ingot-security:ingot-security-recording:test \
  :ingot-framework:ingot-security:ingot-security-event-store-mysql:test \
  :ingot-framework:ingot-security:ingot-security-event-transport-feign:test \
  :ingot-service:ingot-security:ingot-security-provider:test
```

- [ ] 全部 PASS，含 `RecordingConfigResolverTest`

---

## 签核

| 角色 | 姓名 | 日期 | 结果 |
|---|---|---|---|
| 开发 | | | |
| QA | | | |
