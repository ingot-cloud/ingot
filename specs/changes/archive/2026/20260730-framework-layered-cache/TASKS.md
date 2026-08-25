# Tasks

> 分 Phase 交付。Phase 03 起有外部门禁，见 [README](./README.md#分-phase-交付与门禁)。

## 已定决策

| 决策 | 结论 | 依据 |
|---|---|---|
| 迁移范围 | 一次性全量统一：框架 + gateway-rule-client + LoginFailure + credential + dict | 用户确认（收益最大，credential 回归风险以独立 Phase + 逐字段对比控制） |
| 派生缓存失效策略 | 版本号驱动 + TTL 兜底 + 广播失效三重；派生层比对 `(source, version)`；Sentinel `reloadRules` 仅在 version 变化时触发 | 用户确认（精确，无冗余重编译与规则抖动） |
| 交付节奏 | 先框架，再 gateway；LoginFailure/credential/dict 等 L4 change 验收后 | 用户确认 |
| 模块形态 | 单模块 `ingot-framework/ingot-cache` | DESIGN D1 |
| 配置键归属 | 框架不定义 `@ConfigurationProperties`，各消费者保留现有键名 | DESIGN D2 |
| 层次顺序 | Resilient 位于 L1/L2 之下；刷新通知位于最外层 | DESIGN D3 |
| Sentinel 联动 | 订阅 `CacheRefreshListener`，用快照对象引用比对；`refresh-interval` 定时兜底默认关闭 | DESIGN D5 |
| L2 与 LKG | 两个独立 Redis key，不合并 | DESIGN D7 |
| dict 降级 | 不新增 LKG/地板 | DESIGN D8 |
| Actuator | 各模块端点保留，框架另加 `layeredcache` 汇总端点 | DESIGN D9 |

## Phase 01 · 缓存框架与 skill

- [x] T1-1：SDD 工件与登记
  - 依赖：无
  - 验收：change 目录四工件 + phases 齐备；ROADMAP 追加 R-2026-027；`specs/README.md` §7 活动变更列表包含本 change
- [x] T1-2：模块骨架
  - 依赖：无
  - 验收：`ingot-framework/ingot-cache` 在 `settings.gradle` 与 `config/ingot.gradle` 注册；`build.gradle` 中 caffeine 为 `implementation`、redis/actuator 为 `compileOnly`；`spi` 四个接口（`LayeredCache` / `CacheValueLoader` / `CacheFloorSupplier` / `RemoteUnavailableException`）编译通过
- [x] T1-3：五层组件
  - 依赖：T1-2
  - 验收：`CaffeineCacheLayer`、`RedisCacheLayer`（含多 key SCAN evictAll）、`ResilientCacheLayer`、`LastKnownGoodStore`、`CacheSource` + `CacheSourceHolder` 实现完成，语义符合 REQUIREMENTS 规则 1-6
  - 实施补充：另增 `LoaderCacheLayer`（弹性关闭时的最内层占位）与 `RefreshNotifyingCacheLayer`（最外层刷新通知）
- [x] T1-4：派生缓存
  - 依赖：T1-3
  - 验收：`VersionedDerivedCache<S, D>` 与 `SnapshotVersion(source, version)` 实现完成，覆盖降级导致 version 回退或相等的场景（规则 9）
  - 实施补充：另增 `LazyDerivedCache<D>` 供无版本源的 local 模式使用，替代 gateway 的 `LocalCompiledCache`
- [x] T1-5：装配与可观测
  - 依赖：T1-3
  - 验收：`LayeredCacheCoordinator`（`InvalidationBus` 订阅 + 分域 evictor 注册，单回调异常不中断）、`CacheRefreshListener`、`LayeredCacheBuilder`、`LayeredCacheSettings`、`LayeredCacheAutoConfiguration`、`LayeredCacheRegistry` + `LayeredCacheEndpoint` 完成并可通过 `AutoConfiguration.imports` 注册
  - 实施补充：`LayeredCacheCoordinator` 泛型化为 `<E, D>`；另增 `CacheRefreshPublisher` 解耦监听方装配时机、`LayeredCacheDescriptor` 承载注册画像
- [x] T1-6：框架单测
  - 依赖：T1-4、T1-5
  - 验收：装饰器组合矩阵、派生缓存 `(source, version)` 失效、多 key evict、fail-closed、不缓存空值、LKG 不随 evict 清除，全部通过（50 项）
- [x] T1-7：skill 与 AGENTS 引用
  - 依赖：T1-6
  - 验收：`.agents/skills/layered-cache/`（`SKILL.md` + `agents/openai.yaml`）可独立指导一次接入；`AGENTS.md` 追加「缓存接入规范」段落

## Phase 02 · gateway-rule-client 迁移

- [x] T2-1：共享快照层
  - 依赖：T1-6
  - 验收：`SecurityPolicySnapshotVO` 走 L1+L2+Resilient(LKG+Floor)，替换现有无缓存的 `ResilientSnapshotFetcher`；冷启动与 `ALL` 失效后 Feign 调用次数各为 1（原为最多 4），由 `SharedSnapshotCacheTest` 覆盖
  - 实施补充：`PolicyRemoteUnavailableException` 改为继承框架 `RemoteUnavailableException`；`PolicySource`/`PolicySourceHolder`/`PolicyLastKnownGoodStore`/`ResilientSnapshotFetcher` 删除，改用框架同义组件，Actuator 输出字段与枚举值不变
- [x] T2-2：四域派生层
  - 依赖：T2-1
  - 验收：`LocalCompiledCache` 换成 `VersionedDerivedCache`，按共享快照 `(source, version)` 决定是否重编译；version 未变则复用旧编译产物
  - 实施补充：四个 local 模式服务改用 `LazyDerivedCache`；各域 `evictAll()` 同时清共享层，使清理不依赖失效回调的注册顺序
- [x] T2-3：Sentinel version 联动
  - 依赖：T2-2
  - 验收：拆出 `reloadRules()`（失效广播，先清缓存后无条件重载）与 `reloadIfChanged()`（TTL 懒刷新与定时兜底，快照引用未变则跳过）；订阅 `CacheRefreshPublisher`；新增可配 `refresh-interval` 定时兜底（默认关闭）
- [x] T2-4：正交性回归
  - 依赖：T2-3
  - 验收：`GatewayRuleClientWiringTest` 四项与 `SharedSnapshotCacheTest` 五项全绿；不设 `ingot.security.policy.client.*` 任何键时 remote 模式可装配；单域关闭不影响其他域；Actuator `securitypolicy` 输出与迁移前一致

## Phase 03 · LoginFailure 与 credential 迁移

> **门禁**：[20260729-security-access-protection](../../archive/2026/20260729-security-access-protection/TASKS.md) 的 T4-1、T4-2 已完成（2026-08-01 验收归档）。

- [x] T3-1：LoginFailure 迁移
  - 依赖：T2-4 + 外部门禁
  - 验收：换用框架组件并首次补齐 L1+L2；失效事件后 L1/L2 被真实清除（修复 D-C）；同步更新 L4 change 的 DESIGN.md
- [x] T3-2：credential 迁移
  - 依赖：T3-1
  - 验收：内部实现换框架组件，DESIGN「零行为变化清单」六项逐项核对通过；provider 侧 delegate 覆盖装配行为不变；迁移前后 Actuator 输出字段与 Redis key 不变

## Phase 04 · dict 迁移与收口

- [x] T4-1：dict 迁移
  - 依赖：T3-2
  - 验收：多 key 场景验证泛型抽象；`mode=AUTO|LOCAL|REMOTE|NONE` 与配置键 `cache-*`/`redis-*` 行为不变；不新增 LKG/地板
- [x] T4-2：清理与全量验证
  - 依赖：T4-1
  - 验收：四处旧实现类删除且全仓库无残留引用；相关模块编译 + 单测通过；gateway 冷启动 Feign 计数、credential 契约对齐、失效广播三项由单测与代码核对记录在案
- [x] T4-3：Current 基线
  - 依赖：T4-2
  - 验收：`specs/current/framework/layered-cache/`（README + SPEC）建立并反映 As-Built

## 验证任务

- [x] V1：Phase 01 框架单测全绿（T1-6），50 项（收口后 52 项，含 `evictMatching`）
- [x] V2：Phase 02 gateway 冷启动 Feign 计数由 4 降为 1，且 version 未变时派生缓存返回同一引用、Sentinel 不重载（`SharedSnapshotCacheTest`）
- [ ] V3：Phase 02 复用 [test-case/security-policy-e2e.md](../../../../test-case/security-policy-e2e.md) 完成 gateway 回归（需运行环境，待用户执行）
- [x] V4：Phase 03 credential 迁移后 Actuator 字段（`currentSource` / `lastKnownGoodCount` / `localFloorCount` / `lastDegradeAt`）与 Redis key（`in:credential:configs:all`、`in:credential:policy:lkg`）与迁移前一致
- [x] V5：Phase 04 相关模块编译 + 单测通过（cache 52、LoginFailure 4、credential 分层 5、dict 7，均 0 fail）

## 完成检查

- [x] 实现与 DESIGN 一致（差异见 DESIGN As-Built）
- [x] REQUIREMENTS 验收标准全部满足（V3 除外，待运行环境）
- [x] Current 已更新
- [x] Change 已记录完成信息并归档
