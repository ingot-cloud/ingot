# Design

## 方案摘要

镜像登录失败策略（L4）的 `ingot-cache` 分层链，补齐账号锁定策略 remote。消费侧只改 seam 取参；策略 vs 用户数据分离不变。

决策：

- **D1** 表按 `user_type` 两行，不允许 DELETE。
- **D2** seam 改为 `getLockoutPolicy(UserTypeEnum)`；local 仍返回本进程 Nacos（可带上入参 userType）；remote 从全量快照按类型取行，缺行用快照第一条（地板单元素场景）。
- **D3** remote 装配在 account-adapter；core 不依赖 cache / event-bus。
- **D4** 配置键仍为 `ingot.security.account.*`，新增 `policy.fallback` / `policy.cache`。
- **D5** 远端空列表视为不可用（锁定策略禁止合法空）。
- **D6** 交付 `PLATFORM-API.md` + Platform OpenAPI 注解；不做前端页面。

## 数据模型与接口

### 表 `account_lockout_policy_config`（`ingot_security`）

| 列 | 说明 |
|---|---|
| `user_type` | `0` ADMIN / `1` APP，UNIQUE |
| `enabled` | 是否自动锁定 |
| `max_attempts` | 失败阈值 |
| `lock_duration_minutes` | 锁定分钟，`0`=永久（仅 ADMIN） |
| `attempt_window_minutes` | 失败计数窗口 |
| `hint_after_attempts` | 分级提示起始次数 |
| `remark` | 备注 |

### 公共接口

- Platform：`/platform/security/account/lockout-policies`（GET 列表、GET `/{userType}`、PUT upsert）
- Inner：`GET /inner/security/account/lockout-policies`
- Feign：`RemoteAccountLockoutPolicyService`
- 失效域：`SecurityPolicyDomain.ACCOUNT_LOCKOUT`
- 权限：`platform:security:account:lockout:query` / `update`

### Redis

与锁定信号 `AccountLock` 分开：

- L2 `in:sec:account:policy:snapshot`
- LKG `in:sec:account:policy:lkg`

### 配置

```yaml
ingot.security.account:
  mode: local | remote
  policy:
    fallback:
      local-floor-enabled: true
    cache:
      l1-enabled: true
      l1-ttl: 5m
      l2-enabled: true
      l2-ttl: 30m
  lockout: { ... }   # local 生效源 / remote 地板
```

## 数据流与失败处理

remote 读：CachedLoader → L1 → L2 → Feign。成功（非空）刷新 LKG。失败 / 空列表 → LKG → 地板。

写：AdminService upsert → `SecurityPolicyChangedSpringEvent` → 既有 InvalidationPublisher → 消费侧 Coordinator `evictAll`（不清 LKG）。

## 迁移与回滚

1. 执行 018（表+种子）、019（权限）。
2. 发布 security / PMS / Member。
3. 验证 Inner 后，按环境把 Nacos `mode` 改为 `remote`。

回滚：Nacos 改回 `local`；表与权限可留。提供 rollback SQL。

## 测试策略

- Remote loader：成功 / 失败 / 空列表
- Cached loader：L1 命中、evictAll、地板、fail-closed
- Admin：APP 永久锁拒绝、upsert 发 `ACCOUNT_LOCKOUT`
- 调用方 mock 改为 `getLockoutPolicy(userType)`
