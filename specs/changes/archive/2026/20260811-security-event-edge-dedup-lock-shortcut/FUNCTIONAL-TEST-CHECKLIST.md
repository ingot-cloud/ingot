# 功能验收清单

> Change：`20260811-security-event-edge-dedup-lock-shortcut`  
> 前置：`ingot.security.event.enabled=true`；PMS `delivery.spool.directory` 为**可写绝对路径**；Redis 可用。

## 1. 边沿事件（PMS / account-core）

| # | 场景 | 操作 | 预期 `ingot_core.security_event` |
|---|------|------|----------------------------------|
| E1 | 连续失败至锁定 | ADMIN 错密 5 次（间隔小于窗口） | **1** 条 `ACCOUNT_LOCKED` + **5** 条 `LOGIN_FAILURE` |
| E2 | 锁定后继续失败 | 同一账号再错密 10 次 | 仅新增 `LOGIN_FAILURE`（若进入 recordFailure）；**0** 条新 `ACCOUNT_LOCKED` |
| E3 | 管理员重复锁定 | 对已锁定用户再次「手动锁定」 | **0** 条新 `ACCOUNT_LOCKED` |
| E3b | 重复解锁/启禁 | 对已解锁用户再解锁；对已启用用户再启用 | **0** 条对应 DURABLE 事件 |

**SQL 示例**：

```sql
SELECT event_type, COUNT(*) cnt
FROM security_event
WHERE user_id = :userId
  AND occurred_at > NOW() - INTERVAL 1 HOUR
GROUP BY event_type;
```

## 2. Redis 锁定信号

| # | 场景 | 预期 |
|---|------|------|
| E-R1 | 自动/手动锁定后 | Redis 存在 `in:sec:account:locked:uid:*` 与 `in:sec:account:locked:name:*` |
| E-R2 | 解锁后 | 两 key 均删除 |
| E-R3 | Redis 短暂不可用期间锁定 | DB 仍 locked=true；恢复后补写或下次登录仍被 Auth/BFF 拦截（fail-open 不丢 DB 状态） |

## 3. BFF 加密登录

| # | 场景 | 预期 |
|---|------|------|
| E4a | 账号已锁定，`POST /bff/auth/login`（HYBRID 加密） | 返回账号锁定错误；**无** Auth `pre_authorize` 调用（日志/Feign trace） |
| E4a' | 未锁定正常登录 | 流程与现网一致 |

## 4. Gateway JWT 拦截

| # | 场景 | 预期 |
|---|------|------|
| E4b | 已持 JWT 调业务 API，账号中途被锁 | Gateway **403**；请求不进 BFF/PMS 业务。瘦身 JWT 无 `ut` 时须能从 Redis `token:jti:{jti}`（OnlineToken.userType）补全后命中 uid key；Auth 与 Gateway 共用同一 Redis |
| E4c | 匿名/无 JWT 公共路径 | Gateway **放行**（与现网一致） |
| E4d | OnlineToken miss（jti 无对应 Redis） | Gateway **fail-open 放行**（与 Redis down 语义一致）；不因缺少 userType 误 403 |

## 5. Auth 缓存

| # | 场景 | 预期 |
|---|------|------|
| E5 | 锁定后 Auth 仍收到认证（inner/直连） | **无** PMS `/inner/user/details` Feign（日志）；返回 locked UserDetails |

## 6. Access / Gateway 超阈值边沿去重

| # | 场景 | 预期 |
|---|------|------|
| E6 | 登录失败保护：同 IP 超阈值后继续失败 | **1** 条 `LOGIN_FAIL_*_EXCEED`（或等价 ACCESS DURABLE）；Redis temp block 仍生效 |
| E7 | Sentinel 限流升级：清 Redis temp/violation 后执行 `hey -n 100 -c 100 http://localhost:7980/pms/test/limit`（`block-threshold=25`，`categories.access=true`，`target=center`） | `ingot_security.security_event` 新增 **恰好 1** 条 `RATE_LIMIT_VIOLATION`（`source_module` 为 `ingot-gateway` 或配置值）；**不得**再出现「≈限流次数−threshold+1」条数；Redis 存在对应 `in:gw:bl:tmp:*`；本次压测响应可为 200/429 混合（同波未全变 403 可接受） |
| E7b | 紧接着再执行一次同样 hey（不清理 temp-block） | HTTP 多为 **403**；**0** 条新的 `RATE_LIMIT_VIOLATION`；Gateway/Security `/actuator/securityrecording` 无异常 `failed` 暴涨 |
| E7c | （可选压力）清 key 后 `hey -n 1000 -c 1000` 同路径 | 仍为 **恰好 1** 条 `RATE_LIMIT_VIOLATION`（验证不随并发线性膨胀） |
| E7d | temp-block TTL 过期（或手工 DEL）后再次压测达阈值 | 允许再产生 **1** 条新 `RATE_LIMIT_VIOLATION`（新生命周期） |

**E7 SQL 示例**：

```sql
SELECT event_type, source_module, COUNT(*) cnt
FROM ingot_security.security_event
WHERE event_type = 'RATE_LIMIT_VIOLATION'
  AND received_at > NOW() - INTERVAL 1 HOUR
GROUP BY event_type, source_module;
```

## 7. 回归

| # | 场景 | 预期 |
|---|------|------|
| E8 | 正常登录成功 | `LOGIN_SUCCESS` 仍写入；锁定计数清零 |
| E9 | 管理员解锁 | **1** 条 `ACCOUNT_UNLOCKED`；Redis key 清除；可再登录 |
| E10 | Gateway 限流/黑名单 | 现有 BlacklistFilter、Sentinel 行为不退化（403/429 语义不变；仅上报次数收敛） |

## 观测点

- PMS：`/actuator/securityrecording` — `published` / `persisted` / `failed` / `dropped`
- PMS：`/actuator/beans` — `compositeSecurityEventPort` 存在
- Gateway：Filter 顺序与 403 响应体错误码
- BFF：登录失败时 Feign 调用栈无 `preAuthorize`

## 备注

- E2 中若启用 BFF 前置拦截（E4a），锁定后 BFF 登录可能 **不产生** LOGIN_FAILURE（未进入 Auth）；经 Auth 直连或 inner 路径仍可产生 LOGIN_FAILURE。
- spool 目录权限错误会导致 **仅 DURABLE** 丢失；验收前须确认 Nacos `ingot.security.event.delivery.spool.directory`。
- E7：改造前同场景会落入约 `限流次数 − (blockThreshold − 1)` 条 `RATE_LIMIT_VIOLATION`；验收以 **恰好 1 条** 为准。第二次全 403 不上报是预期（E7b），不代表中心「写满」。
- E4b：会话中途锁定依赖 Gateway 能读取 Auth 写入的 OnlineToken（`token:jti:{jti}`）。验收时确认 Auth 与 Gateway 指向同一 Redis；JWT 瘦身后不要再期望 payload 含 `ut`。
