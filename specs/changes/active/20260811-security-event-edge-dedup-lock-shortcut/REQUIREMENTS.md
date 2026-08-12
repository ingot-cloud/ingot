# Requirements

## 用户场景

### US-1 连续登录失败触发锁定（PMS B 端）

- **触发**：同一 ADMIN 账号连续 N 次（默认 5）密码错误，经 Auth → PMS `recordFailure` 异步回调。
- **期望**：
  - 恰好 **1 条** `ACCOUNT_LOCKED`（DURABLE）写入 `ingot_core.security_event`。
  - 每次失败各有 **1 条** `LOGIN_FAILURE`（BEST_EFFORT）。
  - 第 N 次失败当次 Auth 仍返回 bad credentials；第 N+1 次起返回账号已锁定。

### US-2 锁定后继续尝试登录

- **触发**：账号已锁定，继续错密或通过 BFF 登录。
- **期望**：
  - **不再**新增 `ACCOUNT_LOCKED`。
  - **仍可**新增 `LOGIN_FAILURE`（若请求进入 Auth/PMS 记录链）；BFF 前置拦截时 **不**调 Auth（默认不产生 LOGIN_FAILURE）。
  - **不再**递增 `account_lock_state.failed_login_count`（已锁定时 skip increment）。

### US-3 BFF 加密登录（生产主路径）

- **触发**：`POST /bff/auth/login`，body 为 HYBRID 加密；Redis 已有该用户 name key。
- **期望**：BFF 解密后查 Redis，**直接返回锁定错误**，不调用 Auth `pre_authorize`。
- **约束**：Gateway **不得**尝试解析 `/bff/auth/login` 密文 body。

### US-4 会话中途锁定

- **触发**：用户已持 JWT 访问业务 API；管理员锁定该账号；用户继续请求。
- **期望**：Gateway `AccountLockFilter` 读 JWT uid key，**403**，不进入 BFF/PMS 业务。

### US-5 管理员重复操作

- **触发**：对已锁定账号再次「锁定」；对已解锁账号再次「解锁」；对已启用账号再次「启用」。
- **期望**：**不**重复发送对应 DURABLE 事件（`ACCOUNT_LOCKED` / `ACCOUNT_UNLOCKED` / `ACCOUNT_ENABLED` / `ACCOUNT_DISABLED`）。

### US-6 访问防护超阈值边沿去重

- **触发**：登录失败保护或 Sentinel 限流升级后，继续在同窗口内失败/违规（含高并发同波请求）。
- **期望**：
  - `LOGIN_FAIL_*_EXCEED`、`BLACKLIST_BLOCK` **仅首次超阈值**写入。
  - 网关限流升级（`SentinelBlockHandler` → temp-block）产生的 **`RATE_LIMIT_VIOLATION`：同一 `(keyType, keyValue)` 在一次临时封禁生命周期内至多 1 条**（例如 `block-threshold=25` 时，不因第 26、27… 次 429 或同波并发再刷库）。
  - Redis 临时封禁仍可在后续违规时 **刷新 TTL**；已 temp-block 后由 `BlacklistFilter` 返回 403 的请求 **不上报**（与现网一致）。
- **动机**：并发压测下「`count ≥ threshold` 每次 report」会按限流次数线性膨胀（实测 100 并发约数十条；1000/10000 并发会显著占用中心库与 admission）。

## 业务规则

### R1 事件分类语义

| 分类 | 默认优先级 | 触发语义 |
|------|-----------|---------|
| 活动遥测（`LOGIN_FAILURE`、`LOGIN_SUCCESS`；若上报则含 `LOGOUT` / `TOKEN_REFRESH`） | BEST_EFFORT | **电平触发**：每次尝试可记录 |
| 限流升级边沿（网关当前映射为 `RATE_LIMIT_VIOLATION`） | BEST_EFFORT（优先级表不变） | **边沿触发**：仅「未封禁 → 写入/首次取得 temp-block」上报一次；**不是**每次 429 |
| 状态变更（`ACCOUNT_*`、`PASSWORD_*`、`LOGIN_FAIL_*_EXCEED`、`BLACKLIST_BLOCK` 等） | DURABLE | **边沿触发**：仅状态变化时记录 |

> 说明：网关**不会**在每次 Sentinel 429 上写事件；今日唯一的限流相关 ACCESS 上报发生在违规升级写 temp-block 时。该上报虽类型名为 `RATE_LIMIT_VIOLATION`、优先级仍为 BEST_EFFORT，但本 change 将其触发语义收紧为**边沿**（与 US-6 一致）。普通 429 采样遥测不在本期范围。

#### R1.1 边沿实现强度（A / B / C）

不是所有「边沿语义」事件都需要同一套 DB/Redis guard。按「条件可持续为真、入口可被反复调用」分级：

| 级别 | 事件 | 本期处理 |
|------|------|----------|
| **A · 必须显式边沿去重** | `ACCOUNT_LOCKED` / `ACCOUNT_UNLOCKED` / `ACCOUNT_ENABLED` / `ACCOUNT_DISABLED`；`LOGIN_FAIL_*_EXCEED`；`BLACKLIST_BLOCK`；网关升级路径 `RATE_LIMIT_VIOLATION` | 本 change 实施：状态未变 → no-op / 不 `report`；并发用 DB 状态或 SETNX |
| **B · 用例天然边沿** | `ACCOUNT_CREATED` / `ACCOUNT_DELETED`；`PASSWORD_CHANGED` / `PASSWORD_RESET` / `FORCE_CHANGE_PASSWORD` / `PASSWORD_EXPIRED` | 语义仍是「仅变化时记」；靠 use case 单次动作即可，**本期不增加额外短路**。若后续发现重试入口刷事件，另开 change 补 guard |
| **C · 电平（禁止边沿去重）** | `LOGIN_FAILURE` / `LOGIN_SUCCESS`（及活动类 `LOGOUT` / `TOKEN_REFRESH`） | 每次尝试可记；锁定后进入 `recordFailure` 时 **仍发** `LOGIN_FAILURE` |

判定口诀：**false→true / true→false 才发一次**；同状态下重复调用 = no-op。刷库风险集中在 A 类可重复状态入口与超阈值升级，而非全部 DURABLE。

### R2 边沿检测（账号域）

- `lockAutomatically` / `lockManually`：当前已 `locked=true` → **no-op**，**不发** `ACCOUNT_LOCKED`。
- `unlockManually` / 过期自动解锁：当前已 `locked=false` → **no-op**，**不发** `ACCOUNT_UNLOCKED`。
- `recordFailure`：已锁定 → 仍发 `LOGIN_FAILURE`；**skip** `incrementFailCount` 与 `lockAutomatically`。
- `enableAccount` / `disableAccount`：目标状态与当前一致 → no-op，不发事件。

### R3 Redis 锁定信号（双 Key）

- 锁定 transition 成功后写入：
  - `in:sec:account:locked:uid:{userType}:{userId}`
  - `in:sec:account:locked:name:{userType}:{username}`
- 解锁 transition 成功后 **删除** 上述两 key。
- 写入时机：**DB 事务 afterCommit**（与 recording afterCommit 对齐，缩短空窗）。
- Redis 不可用：**fail-open**（锁定落库不受影响；BFF/Gateway/Auth 降级现有行为）。

### R4 分层拦截职责

| 层级 | 职责 | 身份来源 |
|------|------|---------|
| BFF | 加密登录锁定拦截 | 解密后 `username` → name key |
| Gateway | 已认证 API 锁定拦截 | JWT `i` + `user_type` → uid key |
| Auth | UserDetails 缓存兜底 | name key |
| PMS 领域层 | 边沿检测与事件发布 | DB `LockStatePort` |

### R5 fail-open 与排除

- Gateway：无 JWT、JWT 解析失败、Redis  down、exclude 路径 → **放行**。
- Gateway **不**解析 `/bff/**` 请求 body。
- BFF 拦截默认 **不调 Auth**，故 **默认不产生** LOGIN_FAILURE（可配置项留 DESIGN，默认关闭）。

### R6 网关限流升级边沿（并发安全）

- 判定维度：`(keyType, keyValue)`（当前主路径为 IP）+ 一次 temp-block 生命周期（key 存在至 TTL 过期/显式解除）。
- **仅**在成功取得「首次写入 temp-block」边沿时调用 `BlacklistEventReporter.report`（事件类型保持现网映射：`AUTO` + `RATE_LIMIT` + `BLOCK` → `RATE_LIMIT_VIOLATION`）。
- `count ≥ blockThreshold` 但 temp-block **已存在**：允许刷新 TTL，**禁止**再次 `report`。
- 不得仅用 `count == blockThreshold` 作为唯一去重条件：高并发下多请求可同时看到 `count` 为 25、26、27…，且多数请求在写 Redis 前已越过 `BlacklistFilter`，仅靠计数值会漏去重。
- temp-block 过期后，若再次累计超阈值：允许再产生 **1** 条新事件（新生命周期）。

## 边界与非目标

- **并发阈值（账号）**：同一时刻多个请求同时达到第 N 次失败，领域层 `LockAccountUseCaseService` 边沿检测应尽量保证 **至多 1 条** `ACCOUNT_LOCKED`；极端竞态以 DB 状态为准，事件幂等靠边沿而非 eventId 业务 dedup。
- **并发阈值（网关）**：同波 `hey -c N` 越过 Blacklist 后撞 Sentinel 时，靠 Redis **首次占位（SETNX / setIfAbsent）** 保证至多 1 条升级事件；后续请求可续期 TTL。
- **Inner API**：不经 Gateway 的内网 Feign 不受 Gateway Filter 保护，依赖 Phase 1 领域边沿 + Auth 缓存。
- **Member C 端**：与 PMS 共用 account-core；永久锁禁止等 Nacos 约束不变。
- **不在范围**：修改 recording 框架优先级表默认值；Gateway 验 JWT 签名；为每次 429 增加 ACCESS 遥测。
- **不在范围（B 类）**：为 `PASSWORD_*` / `ACCOUNT_CREATED` / `ACCOUNT_DELETED` 增加与 A 类同构的边沿短路（见 R1.1）。
- **不在范围（类型 SoT）**：合并双份 `SecurityEventType`、抽取 recording 可用的事件 code 常量模块——见 follow-up [20260812-security-event-type-sot-cleanup](../20260812-security-event-type-sot-cleanup/README.md)。

## 验收标准

- [ ] E1–E7 / E7b 见 [FUNCTIONAL-TEST-CHECKLIST.md](./FUNCTIONAL-TEST-CHECKLIST.md) 全部通过
- [ ] account-core 边沿检测单测通过
- [ ] LoginFailureProtection / Sentinel 边沿单测或集成测试通过（含并发：threshold 后多次 `report` 调用次数 = 1）
- [ ] spool 目录可写前提下 DURABLE 事件无静默丢失（运维项：PMS Nacos `delivery.spool.directory` 须为可写绝对路径）
- [ ] `specs/current` 基线已更新且本 change 已归档
