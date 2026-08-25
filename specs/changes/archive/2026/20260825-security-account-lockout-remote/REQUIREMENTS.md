# Requirements

## 用户场景

1. 安全中心管理员查看 / 更新 B 端（ADMIN）与 C 端（APP）账号锁定策略；保存后各消费节点数秒内生效。
2. PMS / Member 在 `mode=remote` 下从安全中心拉取对应 `userType` 的策略，用于登录失败自动锁定与认证 meta 提示。
3. 安全中心不可用时，消费侧先用 LKG，再无则用本进程 Nacos 地板；不允许把「无策略」当成未启用锁定。
4. 未部署安全中心或未切 remote 的环境保持 `mode=local`，行为与现网一致。

## 业务规则

1. 策略按 `user_type` 分行：`0` ADMIN、`1` APP；不允许删除行，只允许更新。
2. ADMIN 种子：`enabled=true`、`maxAttempts=5`、`lockDurationMinutes=30`、`attemptWindowMinutes=15`、`hintAfterAttempts=3`。
3. APP 种子：`enabled=true`、`maxAttempts=5`、`lockDurationMinutes=15`、`attemptWindowMinutes=15`、`hintAfterAttempts=3`；**禁止** `lockDurationMinutes=0`。
4. 锁定状态与失败计数永远落 `account_lock_state`，不进策略缓存链。
5. `mode=local`（缺省）：每次即时读 Nacos `ingot.security.account.lockout`。
6. `mode=remote`：`L1 → L2 → Feign → LKG → Nacos 地板`；远端失败抛 `RemoteUnavailableException` 子类；空列表视为不可用，不 refresh LKG。
7. 消费侧经 `AccountLockoutPolicyLoader.getLockoutPolicy(UserTypeEnum)` 取策略。
8. Member 现网 Nacos 地板 `lockDurationMinutes=30` 本 change 不改；APP 永久锁约束只在中心管理面与种子生效。

## 边界与非目标

- 前端管理台页面不在本期。
- Inner Feign 不对前端暴露。
- BFF / Gateway / Auth 不拉锁定策略，继续只用 Redis 锁定信号。
- 不把代码或 Nacos 缺省 `mode` 改为 `remote`。
- `mode=remote` 但 Loader 未装配时 fail-fast，不再静默回退 local。

## 验收标准

- [ ] `mode=local` 行为与现网一致，不建分层缓存
- [ ] `mode=remote` 可按 userType 取到中心策略；写后失效广播清 L1/L2，LKG 保留
- [ ] 远端失败有 LKG 用 LKG，无 LKG 用地板；地板关闭则抛异常
- [ ] 空列表当不可用，不 fail-open
- [ ] APP 更新 `lockDurationMinutes=0` 被拒绝
- [ ] Platform API 有 OpenAPI 注解，且 `PLATFORM-API.md` 与契约一致
- [ ] 未切 remote 的服务无需改代码即可启动
