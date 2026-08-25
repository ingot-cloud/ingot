# Phase 04 · dict 迁移与收口

> 状态：completed
>
> 前置：Phase 03 完成

## 目标

迁移 `ingot-dict-client` 以验证泛型抽象在**多 key** 场景下站得住（前三个消费者都是单 key 全量快照），随后清理旧实现并推进 current 基线。

## 实现要点

### dict 迁移

- `CaffeineDictService` 与 `RedisDictService` 换成框架 L1/L2 层，键为业务 key（code + scope），验证 `LayeredCache<K, V>` 的 K 不被单 key 场景带偏（DESIGN D6）。
- 保持 `mode=AUTO|LOCAL|REMOTE|NONE` 语义与 `evict(code)` / `evictAll()` 粒度不变。
- 保持配置键 `cache-enabled` / `cache-ttl` / `cache-maximum-size` / `redis-enabled` / `redis-ttl` / `redis-key-prefix` / `invalidation-enabled` 不变（DESIGN D2）。
- **不新增 LKG 与 Nacos 地板**：dict 远端不可用时的降级语义属业务决策，另开 change；框架已支持，届时开启即可。

## 实施记录

- 键为 `DictCacheKey`（code + scope + tenant + app + includeDisabled），L2 key 格式与迁移前一致。
- 按 code 失效走框架新增的 `evictMatching`；SPI 默认实现退化为 `evictAll()`。
- `batchItems` 改为按键顺序 `get`，冷缓存不再折叠为一次批量 RPC。
- `CaffeineDictService` / `RedisDictService` / `DictServiceFactory` 已删除。
- `LayeredDictServiceTest` 覆盖多 key 独立、`evict(code)` 粒度、空值不缓存、Redis key 格式。
- current `framework/layered-cache` 已建立；change 已归档。

### 清理与验证

- 删除四处旧实现类，确认全仓库无残留引用。
- 全量编译 + 单测。
- 三项端到端验证记录在案：gateway 冷启动 Feign 计数由 4 降为 1、credential 迁移前后 Actuator 与 Redis 内容对齐、失效广播端到端。

### Current 基线

- 新建 `specs/current/framework/layered-cache/`（README + SPEC）反映 As-Built。
- change 状态推进至 `completed` 并按 [SDD 归档规则](../../../../README.md) 移入 archive。

## 退出条件

- dict 多 key 粒度与四种 mode 行为不变。
- 旧实现类零残留引用。
- 全量编译 + 单测通过。
- current 基线建立，change 已归档。

## 回滚

dict 迁移为独立提交可单独回滚。旧实现类的删除与 current 更新在同一提交内，回滚一并恢复。
