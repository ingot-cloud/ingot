# Phase 03 · LoginFailure 补齐与 credential 迁移

> 状态：blocked（等待 L4 change 验收）
>
> 前置：Phase 02 完成 **且** [20260729-security-access-protection](../../20260729-security-access-protection/TASKS.md) 的 T4-1（Resilience 故障注入）与 T4-2（全量 E2E）已完成

## 门禁理由

`ingot-security-access-adapter` 是 L4 active change 尚未验收的交付物。在其 E2E 与故障注入完成前替换缓存实现，会让测试失败的归因（L4 实现问题 vs 缓存迁移引入）无法区分。credential 更是已归档上线的 L1 基线，必须单独迁移、单独回归。

## 目标

- LoginFailure：换用框架组件并**首次补齐 L1+L2**，修复 D-C（`evictAll()` 为 no-op 导致 Coordinator 订阅了失效事件却无缓存可清，每次 `loadAll()` 都走 Feign）。
- credential：内部实现换框架组件，零行为变化。

## 实现要点

### LoginFailure

- `ResilientLoginFailurePolicyLoader` + `LoginFailureLkgStore` + `LocalLoginFailureFloorSupplier` 换成框架组件。
- 补齐 L1+L2，使 `LoginFailurePolicyCacheCoordinator` 的失效订阅真正生效。
- 该模块属 L4 change 交付物，改动需同步回写 [L4 DESIGN.md](../../20260729-security-access-protection/DESIGN.md)。

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
