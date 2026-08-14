# Tasks

> 状态：`completed`。全部 **已定决策** 已确认。T3-4（明文 Auth token name key）本期未做，见完成记录。

## 已定决策

| 决策 | 结论 | 依据 |
|------|------|------|
| 范围 | Phase 1+2+3（含 BFF）+ access/gateway DURABLE 去重 | 用户确认 |
| LOGIN_FAILURE | 保留电平触发；锁定后 skip increment | 用户确认 |
| ACCOUNT_LOCKED | 边沿触发；锁定后不再发 | 用户确认 |
| BFF 登录 | Gateway 不解密；BFF 解密后查 name key | HYBRID 加密约束 |
| Gateway | GlobalFilter + JWT uid key；非 BFF 明文 token 可选 name key | 用户确认 |
| 重复 manual lock | no-op | 本期简化 |
| Redis | 双 Key；afterCommit 写入；fail-open | DESIGN D4–D5、D9 |
| 网关限流升级事件 | 保持 `RATE_LIMIT_VIOLATION` 类型；**边沿**触发；并发用 SETNX 去重 | DEV 实测刷库；REQUIREMENTS R6、DESIGN D12–D14 |
| 每次 429 遥测 | **不做** | 非目标 |
| 边沿实现强度 | 仅 A 类显式去重；B 类天然边沿本期不加 guard；C 类电平 | REQUIREMENTS R1.1、DESIGN D15 |
| 类型 SoT / code 常量 | **不做**（另开 follow-up） | DESIGN D16；[20260812-security-event-type-sot-cleanup](../../active/20260812-security-event-type-sot-cleanup/README.md) |

## Phase 0 · SDD 与评审

- [x] T0-1：SDD 四工件 + 验收清单
  - 依赖：无
  - 验收：本目录 README/REQUIREMENTS/DESIGN/TASKS/FUNCTIONAL-TEST-CHECKLIST 齐备；`specs/README.md` §7 已登记；R1.1 / D15–D16 / follow-up 链接已写入
- [x] T0-2：评审 → `approved`
  - 依赖：T0-1
  - 验收：README 状态改为 `approved`；TASKS 未定项无开放问题；用户确认开始实施后进入 `implementing`

## Phase 1 · 账号域边沿检测

- [x] T1-1：`LockAccountUseCaseService` / `UnlockAccountUseCaseService` 边沿
  - 依赖：T0-2
  - 验收：已 locked 再 lock 不 `publishEvent`；已 unlocked 再 unlock 不 `publishEvent`
- [x] T1-2：`RecordLoginUseCaseService` 已锁定 skip increment + lock；保留 LOGIN_FAILURE
  - 依赖：T0-2
  - 验收：锁定后继续 recordFailure 只调 `publishEvent(LOGIN_FAILURE)` 一次逻辑路径，不调 lockAutomatically
- [x] T1-3：`ManageAccountStatusUseCaseService` 启禁用边沿
  - 依赖：T0-2
  - 验收：重复 enable/disable 不发 DURABLE
- [x] T1-4：account-core 单测
  - 依赖：T1-1–T1-3
  - 验收：`RecordLoginUseCaseServiceTest`、`LockAccountUseCaseServiceTest`、`ManageAccountStatusUseCaseServiceTest` 通过

## Phase 2 · Redis 信号与 Auth 缓存

- [x] T2-1：`RedisKeyConstants.AccountLock` + `AccountLockSignal` 模型
  - 依赖：T0-2
  - 验收：双 key 拼装方法单测
- [x] T2-2：`AccountLockSignalPort` + `RedisAccountLockSignalAdapter` + AutoConfiguration
  - 依赖：T2-1
  - 验收：write/clear/isLocked 单测；无 Redis 时 NoOp
- [x] T2-3：lock/unlock 用例 afterCommit 挂载 write/clear
  - 依赖：T1-1、T2-2
  - 验收：锁定 transition 后 Redis 两 key 存在；解锁后不存在
- [x] T2-4：Auth `CachingRemoteUserDetailsService`
  - 依赖：T2-2
  - 验收：name key hit 时不调 Feign；miss 时委托原实现

## Phase 3 · BFF 与 Gateway 拦截

- [x] T3-1：BFF `BffAuthService.login` 锁定检查 + 配置
  - 依赖：T2-2
  - 验收：Redis hit 时不调 `authClient.preAuthorize`；错误码/文案与 Auth 一致
- [x] T3-2：`BearerJwtPayloadReader.readUserType`（或 readClaims）
  - 依赖：T0-2
  - 验收：单测覆盖 JWT payload 解析
- [x] T3-3：Gateway `AccountLockFilter` + 配置 + 单测
  - 依赖：T2-2、T3-2
  - 验收：JWT uid key hit → 403；exclude 路径放行；Redis down fail-open
- [ ] T3-4：（可选）非 BFF 明文 Auth token 路径 name key 检查
  - 依赖：T3-3
  - 验收：仅 `/auth/oauth2/token` 等明文 form 路径；`/bff/**` 不解析 body
  - 备注：本期未做；生产主路径为 BFF 加密登录 + JWT uid Filter

## Phase 4 · Access/Gateway 超阈值边沿

- [x] T4-1：`LoginFailureProtectionService` 重复超阈值 skip reportEvent
  - 依赖：T0-2
  - 验收：单测：超阈值后连续失败仅 1 次 DURABLE 上报
- [x] T4-2：`TempBlockStore` 首次占位 API（`tryBlockFirst` / SETNX）+ TTL 刷新
  - 依赖：T0-2
  - 验收：单测：首次 `true`、并发/二次 `false`；已存在可续期且不改变「非首次」语义
- [x] T4-3：`SentinelBlockHandler` 仅在首次占位成功时 `BlacklistEventReporter.report`
  - 依赖：T4-2
  - 验收：单测/集成：`count ≥ threshold` 连续/并发多次，`report` 调用次数 = 1；事件类型仍为 `RATE_LIMIT_VIOLATION`；后续可 refresh TTL
- [x] T4-4：（可选）高并发手工对照 E7 / E7b
  - 依赖：T4-3
  - 验收：见 FUNCTIONAL-TEST-CHECKLIST（已按清单验收）

## Phase 5 · 配置、文档与验收

- [x] T5-1：Nacos 样例 / `example.yml` 注释（account-lock-gateway/bff/signal）
  - 依赖：T3-3、T3-1
  - 验收：DEV/TEST/PROD gateway、bff、pms 注释块对齐
- [x] T5-2：执行 [FUNCTIONAL-TEST-CHECKLIST.md](./FUNCTIONAL-TEST-CHECKLIST.md) E1–E7 / E7b
  - 依赖：Phase 1–4
  - 验收：清单全部勾选
- [x] T5-3：更新 `specs/current` 并归档
  - 依赖：T5-2
  - 验收：`account-protection/SPEC.md`、`security-event-recording/SPEC.md` 增量；change 移入 archive

## 验证任务

- [x] V1：`ingot-security-account` 相关模块 `./gradlew test`
- [x] V2：`ingot-bff`、`ingot-gateway`、`ingot-auth` 相关测试
- [x] V3：grep 确认无遗漏「已锁定仍 lockAutomatically 无 guard」路径（仅 `LockAccountUseCaseService` / `RecordLoginUseCaseService` 入口，均已加边沿）

## 完成检查

- [x] 实现与 DESIGN 一致（差异见 README 完成记录）
- [x] REQUIREMENTS 验收标准全部满足（T3-4 可选未做，不阻塞）
- [x] Current 已更新
- [x] Change 已记录完成信息并归档
