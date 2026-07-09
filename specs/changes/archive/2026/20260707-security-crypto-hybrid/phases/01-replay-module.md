# Phase 01 · 通用防重放模块 ingot-security-replay

> 状态：completed（模块编译 + 7 项单测通过）

## 目标

新建独立通用模块，提供与业务无关的防重放能力（时间窗 + nonce 去重）与 `@Idempotent` 注解，供加密、验签与业务复用。

## 实现要点

- 模块路径：`ingot-framework/ingot-security/ingot-security-replay`，纳入父 `pom` 聚合与安全模块 BOM。
- `NonceStore`：`boolean tryAcquire(String key, Duration ttl)`；默认 `RedisNonceStore` 用 `StringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl)`。
- `ReplayGuard`：`check(String namespace, String nonce, long timestamp)`：
  1. `|now - timestamp| <= clockSkew` 否则 `REPLAY_TIMESTAMP_EXPIRED`。
  2. `tryAcquire(keyPrefix+namespace+nonce, window)` 为 false 则 `REPLAY_NONCE_DUPLICATE`。
- `ReplayProperties`（`ingot.replay.*`）：`enabled`、`window`(默认 5m)、`clockSkew`(默认 5m)、`keyPrefix`、`failOpen`(默认 false)。
- `ReplayErrorCode implements ErrorCode`：`REPLAY_TIMESTAMP_EXPIRED`、`REPLAY_NONCE_DUPLICATE`。
- `@Idempotent(key, namespace, ttl)` + `IdempotentAspect`（`@Around`，SpEL 解析 key），复用 `ReplayGuard`。
- `ReplayAutoConfiguration`（`@AutoConfiguration` + `@ConditionalOnMissingBean`），通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册。

## 退出条件

- 模块编译通过并被聚合构建。
- 单测：`RedisNonceStore` 并发只首次成功；`ReplayGuard` 超窗/ 重复错误码；`@Idempotent` 重复拒绝。

## 回滚

- 不被任何模块依赖前可直接移除；已被 crypto 依赖时，回滚 crypto 依赖即可。
