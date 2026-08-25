# 统一分层缓存框架抽象与消费者迁移

> 状态：completed

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260730-framework-layered-cache` |
| 领域 | `framework` |
| 负责人 | jy |
| 创建日期 | 2026-07-30 |
| 目标发布日期 | TBD（分 Phase 交付） |
| Roadmap | [平台优化路线图](../../../../docs/requirements/ROADMAP.md) · R-2026-027 |
| 需求来源 | 实施 [20260729-security-access-protection](../../archive/2026/20260729-security-access-protection/README.md) 期间发现的横切重复与缓存一致性缺陷 |

## 目标

把仓库中已重复三到四次的分层缓存与降级逻辑抽象为通用框架模块，并将现有消费者全量迁移，使「L1 Caffeine + L2 Redis + LKG + Nacos 地板降级 + 跨节点失效」成为**新增缓存能力的默认机制**。

### 触发原因

实施 L4 访问防护时发现同一套逻辑已被写了三到四遍：

| 组件 | credential | gateway-rule-client | access-adapter（LoginFailure） | dict |
|---|---|---|---|---|
| Resilient 降级阶梯 | `ResilientCredentialPolicyConfigService` | `ResilientSnapshotFetcher` | `ResilientLoginFailurePolicyLoader` | 无 |
| LKG Store | `LastKnownGoodStore` | `PolicyLastKnownGoodStore` | `LoginFailureLkgStore` | 无 |
| Nacos 地板 | `LocalFloorSupplier` | `LocalPolicyFloorSupplier` | `LocalLoginFailureFloorSupplier` | 无 |
| L1 本机缓存 | `CaffeineCredentialPolicyConfigService` | `LocalCompiledCache`（`AtomicReference`，**无 TTL**） | **缺失** | `CaffeineDictService` |
| L2 Redis 缓存 | `RedisCredentialPolicyConfigService` | **缺失** | **缺失** | `RedisDictService` |
| 来源可观测 | `CredentialPolicySourceHolder` | `PolicySourceHolder` | `LoginFailurePolicySourceHolder` | 无 |
| 失效协调器 | `CredentialCacheCoordinator` | `SecurityPolicyCacheCoordinator` | `LoginFailurePolicyCacheCoordinator` | `DictCacheCoordinator` |

上表为迁移前现状。迁移后四处均走 `ingot-cache`；LoginFailure 首次补齐 L1+L2。

### 顺带修复的既有缺陷

| 缺陷 | 现状（迁移前） | 影响 |
|---|---|---|
| **D-A 重复 Feign** | 四域各持独立 `LocalCompiledCache`，但共享的 `ResilientSnapshotFetcher` 自身不缓存 | 冷启动或 `ALL` 失效后最多打出 4 次 Feign，并各写一次 LKG |
| **D-B 无 TTL 兜底** | `LocalCompiledCache` 无 TTL，一致性完全依赖 `InvalidationBus` | Redis Pub/Sub 无持久化，消息丢失后该节点**永久 stale** |
| **D-C Coordinator 空转** | `LoginFailurePolicyLoader.evictAll()` 为 no-op 且 `ResilientLoginFailurePolicyLoader` 未 override | 订阅了失效事件却无缓存可清，每次 `loadAll()` 都走 Feign |

## 范围

**包含：**

- 新建 `ingot-framework/ingot-cache`（包 `com.ingot.framework.cache`）：泛型 `LayeredCache` 装饰器链、`VersionedDerivedCache`、`LastKnownGoodStore`、`CacheSourceHolder`、`LayeredCacheCoordinator`、`LayeredCacheBuilder`、Actuator 汇总端点。
- `.agents/skills/layered-cache/` skill + [AGENTS.md](../../../../AGENTS.md) 引用段落。
- 迁移 `ingot-gateway-rule-client` 四域（共享快照层 + 派生编译层 + Sentinel version 联动）。
- 迁移 `ingot-security-access-adapter`（LoginFailure），首次补齐 L1+L2。
- 迁移 `ingot-security-credential`（零行为变化的内部实现替换）。
- 迁移 `ingot-dict-client`（多 key 场景，验证泛型抽象）。
- 删除四处旧实现类。
- 新建 `specs/current/framework/layered-cache/`（验收后）。

**不包含（非目标）：**

- **给 dict 新增 LKG 与 Nacos 地板**：dict 远端不可用时的降级语义属业务决策（当前为返回空并透传），另开 change。框架支持，届时开启即可。
- **account lockout 的 remote 弹性**：仍属 L2 后续独立 change。
- **替换 Spring Cache 体系**：[ingot-core](../../../../ingot-framework/ingot-core) 的 `LocalCacheConfig` 与 `ingot-data-redis` 的 `InRedisCacheManager`（`@Cacheable` 用）不在本次范围。
- **指标体系**：不引入 Micrometer 埋点，沿用现有 `AtomicLong` 计数 + Actuator 暴露；统一 metrics 归 R-2026-020。
- **失效总线换实现**：继续以 `InvalidationBus` 为跨节点失效的唯一出口，不引入 MQ。

## 分 Phase 交付与门禁

| Phase | 内容 | 前置门禁 | 状态 |
|---|---|---|---|
| [01](./phases/01-cache-framework.md) | 框架模块 + skill | 无 | completed |
| [02](./phases/02-gateway-migration.md) | gateway-rule-client 迁移 | Phase 01 | completed |
| [03](./phases/03-loginfailure-credential.md) | LoginFailure 补齐 + credential 迁移 | Phase 02 **且** L4 change 完成 E2E 验收 | completed |
| [04](./phases/04-dict-closure.md) | dict 迁移 + 收口归档 | Phase 03 | completed |

**Phase 03 外部门禁**：L4 [20260729-security-access-protection](../../archive/2026/20260729-security-access-protection/) 已于 2026-08-01 验收归档（T4-1/T4-2 完成）。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)
- [阶段](./phases/)

## 完成记录

- 完成日期：2026-08-22
- 关联提交或 PR：
- 更新的 current capability：`framework/layered-cache`（新建）；`security/access-protection`、`security/credential-security`
- 与原设计的差异：见 [DESIGN As-Built](./DESIGN.md#as-built-与原设计差异)。主要项：SPI 追加 `evictMatching`；dict 键为完整 `DictCacheKey`；`CacheSourceHolder` 按模块具名；dict `batchItems` 改为按键 `get`。
- 取消原因：
- 未完成项：V3 gateway E2E（`test-case/security-policy-e2e.md`）需运行环境，待用户执行。
