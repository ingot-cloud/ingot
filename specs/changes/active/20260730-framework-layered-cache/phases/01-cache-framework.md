# Phase 01 · 缓存框架模块 ingot-cache 与 skill

> 状态：pending

## 目标

新建通用模块 `ingot-framework/ingot-cache`，把「L1 Caffeine → L2 Redis → Resilient(remote → LKG → Nacos 地板)」抽象为泛型装饰器链，并配套 skill 使其成为新增缓存能力的默认机制。本 Phase 不改动任何现有消费者。

## 实现要点

- 模块路径 `ingot-framework/ingot-cache`，包 `com.ingot.framework.cache`；在 `settings.gradle` 与 `config/ingot.gradle` 注册。
- 依赖策略同 [ingot-gateway-rule-client](../../../../../ingot-framework/ingot-gateway-rule-client/build.gradle)：caffeine 为 `implementation`，spring-boot-starter-data-redis 与 actuator 为 `compileOnly`，Redis 侧组件全部经 `ObjectProvider` 可选注入。
- SPI：`LayeredCache<K, V>`（`get` / `evict` / `evictAll`）、`CacheValueLoader<K, V>`、`CacheFloorSupplier<V>`、`RemoteUnavailableException`。
- 五层组件：`CaffeineCacheLayer`、`RedisCacheLayer`（多 key `evictAll()` 走 SCAN+DEL）、`ResilientCacheLayer`、`LastKnownGoodStore`、`CacheSource` + `CacheSourceHolder`。
- 派生层：`VersionedDerivedCache<S, D>`，失效键为 `SnapshotVersion(source, version)` 二元组。
- 装配与可观测：`LayeredCacheCoordinator`、`CacheRefreshListener`、`LayeredCacheBuilder`、`LayeredCacheSettings`（普通 POJO）、`LayeredCacheAutoConfiguration`、`LayeredCacheRegistry`、`LayeredCacheEndpoint`（id `layeredcache`）。
- 语义参照 `ingot-security-credential` 现有实现逐条固化，见 [REQUIREMENTS 业务规则 1-7](../REQUIREMENTS.md#必须固化到框架的既有语义p0)。

## 退出条件

- 模块编译通过并被聚合构建。
- 单测：装饰器组合矩阵、派生缓存 `(source, version)` 失效、多 key evict、fail-closed、不缓存空值、LKG 不随 evict 清除。
- `Redis` 缺失（`ObjectProvider` 返回 null）时链路退化为 L1 + Resilient 且不抛异常。
- `.agents/skills/layered-cache/` 可独立指导接入；`AGENTS.md` 已引用。

## 回滚

不被任何模块依赖，可直接移除模块目录并回滚 `settings.gradle` / `config/ingot.gradle` / `AGENTS.md`。
