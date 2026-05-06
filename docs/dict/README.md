# 字典模块（Platform Dict）

## 模块概述

字典模块是 Ingot Cloud 的统一枚举/数据字典基础设施。它由 PMS 内的领域模型 `PlatformDict` 与 `ingot-framework` 中的 `ingot-dict-client`、`ingot-event-bus` 三部分组成，向业务代码提供"无感"的字典访问、多级缓存与跨节点缓存一致性。

### 核心特性

- ✅ **企业级字段方案** — 单表承载字典类型与字典项，原生支持平台 / 租户 / 应用三种作用域
- ✅ **统一访问入口** — 业务方仅依赖 `DictService` 接口；本地与 RPC 实现由自动配置二选一
- ✅ **三级缓存** — Caffeine（L1，进程内） → Redis（L2，集群共享） → MySQL（L3，源数据）
- ✅ **跨节点失效广播** — PMS 写操作事务提交后通过 `ingot-event-bus` 广播失效事件，所有实例自动同步清除 L1+L2
- ✅ **可插拔事件总线** — 默认 Redis Pub/Sub 实现；`InvalidationBus` SPI 可替换为 Kafka 等任意 MQ
- ✅ **可扩展属性** — `extra` JSON 字段支持图标、颜色、i18n 等业务扩展
- ✅ **管理端 API + 内部 RPC** — 后台管理走 `/v1/platform/base/dict/**`；微服务间调用走 `/inner/dict/**` Feign 接口

---

## 30 秒快速开始

### 业务方使用（任意微服务）

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-dict-client')
}
```

```java
@Autowired
private DictService dictService;

public void demo() {
    List<DictItem> items = dictService.items("user_status");

    String label = dictService.label("user_status", "0");

    Map<String, String> map = dictService.labelMap("user_status", DictQuery.platform());
}
```

**就是这么简单。** 无需关心是否为 PMS 进程内、是否需要 RPC、是否启用缓存——`DictClientAutoConfiguration` 会自动选择合适的实现并叠加 L1+L2 缓存。

---

## 文档导航

| 文档 | 说明 | 适合人群 |
|------|------|---------|
| [架构设计](./ARCHITECTURE.md) | 三层架构、装饰器链、三级缓存与跨节点失效流程 | 架构师、技术负责人 |
| [使用指南](./USAGE.md) | 业务方接入、配置示例、最佳实践与常见误用 | 后端开发者 |
| [API / RPC 参考](./API-REFERENCE.md) | 管理端 REST、内部 Feign、配置项、SQL 字段 | 前后端开发者、运维 |

---

## 模块组成

```
ingot-framework/
├── ingot-dict-client                  字典统一访问入口（业务方依赖）
│   ├── DictService                    业务接口（items / batchItems / label / evict ...）
│   ├── model/DictItem                 稳定输出契约（与 PMS 实体解耦）
│   ├── model/DictQuery                作用域查询条件（PLATFORM / TENANT / APP）
│   ├── internal/CaffeineDictService   L1 进程内缓存装饰器
│   ├── internal/RedisDictService      L2 Redis 共享缓存装饰器
│   ├── internal/DictServiceFactory    L1/L2 装配工厂
│   ├── internal/DictCacheCoordinator  订阅失效事件的协调器
│   ├── event/DictInvalidationEvent    @EventType("dict.invalidate")
│   ├── remote/RemoteDictService       基于 Feign 的远端实现
│   └── config/DictClientAutoConfiguration
│
└── ingot-event-bus                    跨节点失效广播 SPI
    ├── InvalidationBus                发布/订阅总线接口
    ├── InvalidationEvent              事件基类（origin / timestamp）
    ├── @EventType                     声明事件类型 → 推导 channel 名
    ├── Subscription                   订阅句柄
    └── redis/RedisInvalidationBus     默认 Redis Pub/Sub 实现

ingot-service/ingot-pms/
├── ingot-pms-api/.../domain/PlatformDict           领域实体
├── ingot-pms-api/.../rpc/RemotePmsDictService      Feign 接口
├── ingot-pms-provider/.../web/v1/.../PlatformDictAPI   管理端 REST
├── ingot-pms-provider/.../web/inner/InnerDictAPI       内部 RPC
├── ingot-pms-provider/.../service/dict/LocalDictService            本地实现（PMS 自身）
├── ingot-pms-provider/.../service/dict/LocalDictConfig             注册 dictDelegate
├── ingot-pms-provider/.../service/dict/DictChangedSpringEvent      本地变更事件
└── ingot-pms-provider/.../service/dict/DictInvalidationPublisher   事务提交后广播失效
```

---

## 装配与读写流程

### 业务方读路径

```
DictService（@Primary，dict-client 暴露）
   │
   ▼
CaffeineDictService（L1，进程内 5min）
   │ 命中 → 直接返回
   │ 未命中 ↓
   ▼
RedisDictService（L2，集群共享 30min）
   │ 命中 → 反序列化 → 写回 L1 → 返回
   │ 未命中 ↓
   ▼
delegate
   ├── PMS 进程：LocalDictService → BizPlatformDictService → MySQL
   └── 其它服务：RemoteDictService → Feign → InnerDictAPI（PMS）→ MySQL
```

### PMS 写路径

```
PlatformDictServiceImpl.create / update / delete / changeStatus / batchSort
   │
   │ ① 落库（@Transactional）
   ▼
ApplicationEventPublisher.publishEvent(DictChangedSpringEvent)
   │
   │ ② @TransactionalEventListener(AFTER_COMMIT)
   ▼
DictInvalidationPublisher
   │ ③ origin 节点先 evict L2 Redis
   │ ④ 通过 InvalidationBus.publish(DictInvalidationEvent)
   ▼
RedisInvalidationBus → Redis Pub/Sub channel `in:bus:dict.invalidate`
   │
   │ ⑤ 所有节点（origin 自身被回环过滤）
   ▼
DictCacheCoordinator.handle(event)
   │
   ▼
DictService.evict(dictCode)  ←  L1 + L2 自顶向下逐层清理
```

---

## 性能指标参考

| 场景 | 大致延迟 | 说明 |
|-----|---------|------|
| L1 命中 | < 1 ms | Caffeine 进程内 |
| L2 命中 | 2–5 ms | Redis GET + JSON 反序列化 |
| 全部回源（PMS 本地） | 10–30 ms | DB 查询 + 装配 |
| 全部回源（其它服务 RPC） | 20–50 ms | Feign + DB 查询 |
| 跨节点失效广播延迟 | < 50 ms | Redis Pub/Sub 同集群 |

---

## 数据库设计

字典单表 `platform_dict` 同时存储字典类型节点（`type=TYPE`）与字典项节点（`type=ITEM`），通过 `pid` 形成两层树形结构。完整字段定义见 [API / RPC 参考](./API-REFERENCE.md#数据库platform_dict)。

```sql
-- 关键字段示意
CREATE TABLE platform_dict (
    id          bigint        NOT NULL,
    pid         bigint        DEFAULT NULL COMMENT '父ID（字典项指向其字典类型）',
    code        varchar(64)   NOT NULL    COMMENT '字典编码',
    name        varchar(128)  NOT NULL    COMMENT '名称',
    value       varchar(128)  DEFAULT NULL COMMENT '字典项值（仅 type=ITEM 有效）',
    label       varchar(128)  DEFAULT NULL COMMENT '字典项展示文本',
    type        char(1)       NOT NULL    COMMENT '0=TYPE 1=ITEM',
    scope_type  char(1)       NOT NULL    COMMENT '0=平台 1=租户 2=应用',
    tenant_id   bigint        DEFAULT NULL,
    app_id      bigint        DEFAULT NULL,
    sort        int           NOT NULL    DEFAULT 0,
    system_flag bit(1)        NOT NULL    DEFAULT b'0',
    status      char(1)       NOT NULL    COMMENT '0=正常 9=禁用',
    extra       json          DEFAULT NULL COMMENT '扩展属性',
    -- ... 审计字段
    PRIMARY KEY (id),
    INDEX idx_dict_code        (code),
    INDEX idx_dict_type_status (type, status),
    INDEX idx_dict_scope       (scope_type, tenant_id, app_id)
);
```

数据库迁移脚本：

- `databases/migrations/003_rename_meta_to_platform.sql` — 把 `meta_dict` 重命名为 `platform_dict`
- `databases/migrations/004_upgrade_platform_dict.sql` — 升级到企业级字段方案（新增 `value/label/scope_type/tenant_id/app_id/sort/system_flag/extra/审计字段`）

---

## 关键约定

1. **`code` 是业务唯一键** — 业务调用一律使用 `dictCode`，不要直接依赖自增 ID。
2. **作用域优先级** — 同一 `code` 在 APP / TENANT / PLATFORM 中可同时存在；查询时按调用方 `DictQuery` 显式指定，不做隐式合并。
3. **缓存键不绑租户** — Redis 默认 key 前缀 `in:dict:items:*` 对应 `CacheConstants.IGNORE_TENANT_PREFIX`，跨租户共享同一份字典缓存；业务作用域写在 key 后段。
4. **失效粒度按 `dictCode`** — 所有 `evict(dictCode)` 都会沿装饰器链 L1 → L2 一并清除；批量排序等"全量"操作会走 `evictAll()`。
5. **`InvalidationEvent` 必须标注 `@EventType`** — 类型字符串决定 Pub/Sub channel 名（`in:bus:<type>`）；同 domain 内事件类型唯一。
6. **回环过滤** — `RedisInvalidationBus` 通过 `origin` 字段过滤本节点广播给自己的事件，避免重复清缓存。

---

## 未来规划

- 🚧 字典项 i18n 多语言展示（基于 `extra.i18n`）
- 🚧 `social-common` 失效逻辑迁移至 `ingot-event-bus`，统一全局失效广播
- 🚧 字典管理端前端组件（树形 + 拖拽排序 + 批量启停）
- 🚧 Kafka 实现 `InvalidationBus`，用于跨地域多集群部署

---

## 技术支持

- 📖 文档：[本目录下各文档](#文档导航)
- 🔗 项目主页：https://github.com/ingot-cloud/ingot

---

Copyright © 2026 Ingot Cloud
