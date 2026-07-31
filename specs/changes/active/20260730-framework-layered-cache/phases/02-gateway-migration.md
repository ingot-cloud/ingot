# Phase 02 · gateway-rule-client 迁移

> 状态：pending
>
> 前置：Phase 01 完成

## 目标

把 `ingot-gateway-rule-client` 的缓存与降级逻辑换成框架组件，顺带修复 D-A（冷启动最多 4 次重复 Feign）与 D-B（`LocalCompiledCache` 无 TTL 导致失效消息丢失后永久 stale）。

## 实现要点

### 共享快照层

- `SecurityPolicySnapshotVO` 走完整 L1+L2+Resilient(LKG+Floor)，替换现有无缓存的 `ResilientSnapshotFetcher`（其 Javadoc 已明确「本类不做缓存」）。
- 四域共用同一个 `LayeredCache` 实例，一次远端调用供四域复用。
- 复用现有配置键 `lkg-redis-key`、`local-floor-enabled`、`resilience-enabled`（语义不变）；新增 `ingot.security.policy.client.cache.*`（l1/l2/refresh，均有默认值）。

### 四域派生层

- `LocalCompiledCache`（`AtomicReference`、无 TTL）换成 `VersionedDerivedCache`，按共享快照 `(source, version)` 决定是否重编译。
- 编译产物 `CompiledIpList`（持 `java.util.regex.Pattern`）与 `CompiledChallengePolicy`（持 `PathPattern`）不可 JSON 序列化，只留本机，不进 L2。
- `SnapshotAssembler`、`CompiledIpList.compile`、`PathPatternParser.parse` 均有实际开销，编译缓存保留价值。

### Sentinel 联动

- `SentinelGatewayConfiguration.reloadRules` 增加 last-loaded `SnapshotVersion` 比对，未变化则跳过 `loadApiDefinitions` + `loadRules`，消除规则抖动。
- 订阅 `CacheRefreshListener`，使 TTL 懒刷新路径也能触发 reload——ratelimit 域在请求路径上无缓存读者，仅靠 TTL 是空转，详见 [DESIGN S4](../DESIGN.md#s4ratelimit-域的-ttl-空转问题与解法)。
- 新增可配 `refresh-interval` 定时兜底，默认关闭；仅「只开 ratelimit 且其他域无流量」的部署需要。

## 退出条件

- 冷启动 Feign 调用次数从 4 降为 1；`ALL` 失效事件后同为 1。
- 广播失效丢失时，最迟一个 L1 TTL 周期内节点收敛到最新快照。
- 共享快照 version 未变化时 `GatewayRuleManager.loadRules` 不被调用；变化时（含 TTL 懒刷新路径）被调用。
- `local-floor-enabled=false` 且无 LKG 时仍抛 `PolicyRemoteUnavailableException`，未 fail-open。
- Actuator `securitypolicy` 输出字段与迁移前一致。
- 配置正交性未回退：不设 `ingot.security.policy.client.*` 任何键时 remote 模式可装配；单域关闭不影响其他域（延续 [上一轮解耦契约](../../20260729-security-access-protection/DESIGN.md)）。
- `test-case/security-policy-e2e.md` 既有用例回归通过。

## 回滚

本 Phase 为独立提交，回滚即恢复 `LocalCompiledCache` 与原 `ResilientSnapshotFetcher`。对外契约、Redis key 格式、配置键均不变，回滚不涉及配置或数据迁移。
