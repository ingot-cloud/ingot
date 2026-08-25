---
name: layered-cache
description: Wire a new or existing cache onto this repository's unified layered cache framework (ingot-framework/ingot-cache) with L1 Caffeine, L2 Redis, last-known-good fallback, local floor, cross-node invalidation, and versioned derived caches. Use when adding caching to a policy/config/dictionary style read path, when reviewing an existing cache for missing TTL or fallback, or when migrating a hand-rolled cache to the framework. Do not use for Spring `@Cacheable` service-layer CRUD caching.
---

# Layered Cache

## When To Use

Use this skill for **read-mostly reference data pulled from a remote source**: security policies, credential rules, rate-limit snapshots, dictionaries, tenant configuration. These share one shape — expensive to fetch, rarely changed, catastrophic to lose during an outage.

Do **not** use it for entity CRUD caching keyed by id. That is served by `InRedisCacheManager` and Spring's `@Cacheable`, which has no degradation story and needs none.

## Workflow

1. Read `specs/current/framework/layered-cache/SPEC.md`. Treat that as the source of truth when it differs from this skill.
2. Add `implementation project(ingot.framework_cache)` to the consuming module's `build.gradle`. Keep `spring-boot-starter-data-redis` and `spring-boot-actuator-autoconfigure` as `compileOnly` if the module does not already require them.
3. Implement `CacheValueLoader<K, V>` over the real data source (Feign or DB).
4. Map the module's existing configuration properties into a `LayeredCacheSettings`. Do **not** invent a new property prefix for an existing module.
5. Assemble with `LayeredCacheBuilder` in the module's `@AutoConfiguration`.
6. Register an evictor with a `LayeredCacheCoordinator` so cross-node invalidation reaches this cache.
7. If the value is compiled into non-serializable objects, add a `VersionedDerivedCache` on top.
8. Write tests covering the invariants listed below.

## Assembly

```java
LayeredCache<String, List<DictItem>> cache = LayeredCacheBuilder
        .<String, List<DictItem>>named("dict")
        .loader(remoteLoader)
        .settings(settings)
        .cacheable(v -> v != null && !v.isEmpty())
        .emptyValue(List::of)
        .resilientSingleKey(redisTemplate, objectMapper, TYPE, lkgKey, floorSupplier)
        .l2MultiKey(redisTemplate, objectMapper, TYPE, "in:dict:items:")
        .registry(registry)
        .build();
```

Layer order is fixed at `L1 → refresh notify → L2 → Resilient → loader` and is not configurable. Any optional layer that is unconfigured, switched off, or missing its Redis dependency is skipped without breaking the chain.

Choose the L2 key helper by cardinality: `l2SingleKey` for one aggregate snapshot (evictAll issues a plain DEL), `l2MultiKey` for per-entity keys (evictAll issues a prefix SCAN). When a business identifier (for example dict code) must invalidate many query variants, call `cache.evictMatching(predicate, redisScanPattern)` rather than `evict(singleKey)`.

Each consuming module must register its `CacheSourceHolder` as a **named** bean. A type-level `@ConditionalOnMissingBean(CacheSourceHolder.class)` will steal another module's holder in the same process (Auth hosts credential and LoginFailure together).

## Invariants

These are load-bearing. Violating any one reintroduces a bug the framework exists to prevent.

- **Resilient sits innermost, below L1/L2.** Degraded values then expire with the hot cache and the chain retries the remote automatically. Putting it outermost makes degraded values permanent until an invalidation arrives.
- **LKG is not a cache.** It lives in its own Redis key, has no TTL by default, and is never touched by `evict` or `evictAll`. It is written only on remote success.
- **Never cache empty.** Pass a `cacheable` predicate that rejects empty collections. Reading a stale empty value must delete the key and fall through, or a momentary gap gets frozen in for a full TTL.
- **Distinguish "remote is down" from "remote says there is nothing".** Only throw `RemoteUnavailableException` for genuine unavailability. A successful call returning an empty list is valid data: it refreshes LKG and must not trigger degradation.
- **Floor is fail-closed.** With the remote down, no LKG, and floor disabled, the chain throws rather than returning empty. A floor supplier must never return null or empty — supply a minimum baseline instead.
- **The publisher evicts itself.** `InvalidationBus` filters out the origin's own events, so whoever broadcasts must clear its local cache first.
- **TTL is not optional.** Redis Pub/Sub has no persistence and no redelivery; a node that misses one message would otherwise stay stale forever.

## Derived Caches

When the cached value gets compiled into objects that cannot be serialized — `Pattern`, `PathPattern`, prebuilt indexes — that product stays process-local and cannot go into L2. Put a `VersionedDerivedCache` above the shared cache:

```java
VersionedDerivedCache<SnapshotVO, CompiledIndex> derived = new VersionedDerivedCache<>(
        vo -> new SnapshotVersion(sourceHolder.current(), vo.getVersion()),
        vo -> CompiledIndex.compile(vo));

CompiledIndex index = derived.get(sharedCache.get(KEY));
```

The invalidation key must be the `(source, version)` pair, never the version number alone. Floor snapshots carry version 0, remote versions come from the database, and degrade/recover cycles produce version rollbacks and collisions. Comparing only the number silently skips recompilation on `remote(v0) → floor(v0) → remote(v0)`.

## Components Without A Read Path

A cache whose data is consumed by a runtime that holds its own copy — Sentinel's `GatewayRuleManager` is the example — gets no benefit from TTL, because nothing reads the cache to trigger a refresh. Subscribe to `CacheRefreshPublisher` and compare versions in the callback. If the shared cache is used by other domains, their request traffic refreshes it and the callback fires for free; a scheduled refresh is only needed when that component is the sole consumer.

## Test Checklist

- Decorator matrix: every on/off combination of L1, L2, and Resilient still yields a working chain.
- Empty values are not written to L1 or L2, and stale empty values are deleted on read.
- A legitimate empty remote response refreshes LKG and does not increment degradation counters.
- `evictAll` clears L1 and L2 but leaves LKG readable for the next degradation.
- Floor disabled plus no LKG throws instead of returning empty.
- Derived cache recompiles on version rollback and on same-version-different-source.
- Multi-key `evictAll` clears only its own prefix.
- Multi-key `evictMatching` clears only the intended subset (L1 predicate + L2 SCAN pattern).
- A null `StringRedisTemplate` degrades L2 and LKG to no-ops without throwing.
