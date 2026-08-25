# Phase 03 · LoginFailure 补齐与 credential 迁移

> 状态：completed（L4 门禁已解除，已实施）
>
> 前置：Phase 02 完成 **且** [20260729-security-access-protection](../../archive/2026/20260729-security-access-protection/TASKS.md) 的 T4-1（Resilience 故障注入）与 T4-2（全量 E2E）已完成（2026-08-01 验收归档）

## 门禁理由

`ingot-security-access-adapter` 是 L4 交付物。L4 已验收归档后，可将 LoginFailure 缓存实现迁移至 `ingot-cache` 框架，并与 credential 分 Phase 独立回归。

## 目标

- LoginFailure：换用框架组件并**首次补齐 L1+L2**，修复 D-C（`evictAll()` 为 no-op 导致 Coordinator 订阅了失效事件却无缓存可清，每次 `loadAll()` 都走 Feign）。
- credential：内部实现换框架组件，零行为变化。

## 实施记录

- `ResilientLoginFailurePolicyLoader` / `LoginFailureLkgStore` / `LoginFailurePolicySource*` 删除，改用框架组件。
- 首次补齐 L1+L2，配置键 `ingot.security.access.policy.cache.*`，L2 key `in:sec:lf:policy:snapshot`。
- `LoginFailurePolicyRemoteUnavailableException` 改为继承框架 `RemoteUnavailableException`。
- `CacheSourceHolder` 以具名 Bean `loginFailurePolicySourceHolder` 注册，避免与 credential 串台。
- `CachedLoginFailurePolicyLoaderTest` 覆盖 L1 命中、evictAll 真实清除（D-C）、地板与 fail-closed。
- credential：`Resilient*` / `Caffeine*` / `Redis*` / 模块内 `LastKnownGoodStore` / `CredentialPolicySource*` 删除；远端 delegate 保持裸 Feign，Resilient 仅在 `@Primary` 分层链上、且仅当 delegate 为 `RemoteCredentialPolicyConfigService` 时启用。
- `LayeredCredentialPolicyConfigServiceTest` 覆盖远端成功、合法空、地板、fail-closed、evict 后重载。

### LoginFailure

- `ResilientLoginFailurePolicyLoader` + `LoginFailureLkgStore` + `LocalLoginFailureFloorSupplier` 换成框架组件。
- 补齐 L1+L2，使 `LoginFailurePolicyCacheCoordinator` 的失效订阅真正生效。
- 改动需与 [L4 归档 DESIGN.md](../../archive/2026/20260729-security-access-protection/DESIGN.md) 及 current [access-protection](../../../current/security/access-protection/SPEC.md) 一致。

### credential

零行为变化清单，逐项核对：

| 不变项 | 值 |
|---|---|
| SPI | `CredentialPolicyConfigService` 方法签名 |
| L2 Redis key | `in:credential:configs:all` |
| LKG Redis key | `in:credential:policy:lkg` |
| Actuator | endpoint id `credentialpolicy`，输出字段与顺序 |
| 配置键 | `ingot.security.credential.cache.*` 全部键名与默认值 |
| provider 装配 | `LocalCredentialPolicyConfigConfig` 覆盖同名 delegate（MySQL 直查、不包 Resilient），L1/L2 仍叠加其上 |

## 退出条件

- LoginFailure 失效事件后 L1/L2 被真实清除。
- credential 迁移前后 Actuator `credentialpolicy` 输出逐字段一致。
- credential 两个 Redis key 内容格式不变，回滚后旧实现可直接读取。
- credential 配置键全部仍生效、默认值不变。
- provider 侧 delegate 覆盖行为不变。

## 回滚

两项迁移各为独立提交，可分别回滚。因对外契约与 Redis key 格式不变，回滚不涉及配置或数据迁移。
