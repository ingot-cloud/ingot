# 安全事件边沿去重与账号锁定链路短路

> 状态：completed

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260811-security-event-edge-dedup-lock-shortcut` |
| 领域 | `security`（account-protection、security-event-recording、access-protection、gateway、bff） |
| 负责人 | jy |
| 创建日期 | 2026-08-11 |
| 完成日期 | 2026-08-14 |

## 背景

生产与 DEV 验证发现：

1. **DURABLE 状态变更事件**（如 `ACCOUNT_LOCKED`）在条件持续为真时被重复写入 `security_event`（经 file spool），而 **BEST_EFFORT** 的 `LOGIN_FAILURE` 正常。
2. 账号锁定后，登录失败仍穿透 **Gateway → BFF → Auth → PMS**，产生多余 Feign、`account_lock_state` 写与重复 DURABLE 事件。
3. 生产登录走 **BFF `/bff/auth/login`**（`@InCryptoHybridContext` + `@InDecrypt`），Gateway **无法**从密文 body 解析 username；登录锁定拦截须分层实施。
4. **网关限流升级**：`hey -n 100 -c 100` 打 `/pms/test/limit`、`block-threshold=25` 时，中心落入约 **73** 条 `RATE_LIMIT_VIOLATION`（≈限流次数−threshold+1），而非「首次进入 temp-block 仅 1 条」。根因是同波并发已越过 `BlacklistFilter`，且 `count ≥ threshold` 后每次 429 都 `report`；千级/万级并发会线性打满 `ingot_security.security_event`。第二次压测全 403 时不再写入（`BlacklistFilter` 短路），说明问题在**首次升级边沿未去重**，而非中心「写满溢出」。

## 目标

- 状态变更类安全事件 **边沿触发**（仅 false→true / true→false 发 DURABLE）。
- **保留** `LOGIN_FAILURE` 等活动遥测（BEST_EFFORT，每次失败可发）。
- **Redis 双 Key** 同步锁定态；**BFF** 负责加密登录拦截；**Gateway** 负责 JWT 已认证 API 拦截；**Auth** 缓存兜底。
- Access/Gateway 侧 `LOGIN_FAIL_*_EXCEED`、`BLACKLIST_BLOCK`，以及限流升级路径上的 **`RATE_LIMIT_VIOLATION`（进入 temp-block）**：**同一 key 在一次临时封禁生命周期内至多 1 条**；后续同窗口违规只刷新 TTL，不上报。

## 范围

### 包含

- `ingot-security-account-core`：锁定/解锁/登录记录/启禁用的边沿检测、`AccountLockSignalPort` + Redis 双 Key、单测
- `ingot-security-account-adapter`：账号用例仅在 adapter 在 classpath 时装配（避免 BFF/Auth 误装事务依赖）
- `ingot-auth`：RemoteUserDetails 锁定缓存（name key）
- `ingot-bff`：`BffAuthService.login` 解密后锁定检查
- `ingot-gateway`：`AccountLockFilter`（JWT uid key）+ Sentinel 限流升级边沿去重（含并发 SETNX）+ LoginFailure DURABLE 去重
- `ingot-security-access-core`：`LoginFailureProtectionService` 边沿去重
- `ingot-commons`：`RedisKeyConstants.AccountLock`
- Nacos 样例与 `example.yml` 注释（配置块）
- 验收完成后更新 `specs/current/security/account-protection`、`specs/current/framework/security-event-recording`

### 不包含

- 修改 `LOGIN_FAILURE` 默认优先级（仍为 BEST_EFFORT）
- 为每次普通 429 新增采样级 `RATE_LIMIT_VIOLATION` 遥测（当前亦不按次上报；本期只收紧升级边沿）
- Gateway 解密 BFF HYBRID 登录 body
- 手动锁定「延长 lockedUntil」的单独产品（重复锁定本期 no-op）
- `account_security_event` legacy 表变更（见已归档 [20260811-security-drop-account-security-event](../20260811-security-drop-account-security-event/README.md)）
- Token 黑名单 / 强制踢下线全量会话（本期仅 Gateway uid key 403 + 自然失效语义）
- **B 类天然边沿**（`PASSWORD_*` / create-delete）的额外短路 guard（见 REQUIREMENTS R1.1）
- **`SecurityEventType` 双枚举合并**与 recording 事件 code 常量模块（见 [20260812-security-event-type-sot-cleanup](../../active/20260812-security-event-type-sot-cleanup/README.md)）

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)
- [功能验收清单](./FUNCTIONAL-TEST-CHECKLIST.md)

## 依赖与关系

- 依赖已验收的 [security-event-recording](../../../current/framework/security-event-recording/SPEC.md) 与 [account-protection](../../../current/security/account-protection/SPEC.md) 基线。
- 与 [20260806-security-event-legacy-cleanup](../20260806-security-event-legacy-cleanup/README.md) 正交（本 change 不改 recording SPI）。
- 正交 follow-up：[20260812-security-event-type-sot-cleanup](../../active/20260812-security-event-type-sot-cleanup/README.md)（类型 SoT / code 常量；不阻塞本 change）。

## 完成记录

- 完成日期：2026-08-14
- 关联提交或 PR：（随代码一并提交）
- 更新的 current capability：
  - `specs/current/security/account-protection/`
  - `specs/current/framework/security-event-recording/`
  - `specs/current/security/access-protection/`
  - `specs/current/gateway/header-conventions/`
- 与原设计的差异：
  1. `AccountLockSignalPort` / Redis 适配器落在 **account-core**（非 adapter），以便 BFF/Auth 只读信号、不装账号用例。
  2. 账号用例装配条件化：仅 classpath 存在 `DefaultLockStatePortAdapter`（account-adapter）时扫描 UseCase，避免 BFF 缺 `PlatformTransactionManager`。
  3. Gateway uid 拦截身份来自 JWT `i` + OnlineToken `userType`（瘦身 JWT 通常不含 `ut`）；claim 常量下沉 `InJwtClaimNames`。
  4. **T3-4 未做**（明文 Auth token 路径 name key）；生产主路径为 BFF 加密登录 + JWT uid Filter。
  5. 类型 SoT follow-up 仍为 [20260812-security-event-type-sot-cleanup](../../active/20260812-security-event-type-sot-cleanup/README.md)（draft，不阻塞本 change）。
- 取消原因：
