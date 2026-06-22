# 字典模块 API / RPC / 配置参考

> 字典模块对外契约与配置项一览，作为业务方接入与运维调参的速查表。

## 目录

- [客户端接口（业务方使用）](#客户端接口业务方使用)
- [管理端 REST API](#管理端-rest-api)
- [内部 RPC（Feign）](#内部-rpcfeign)
- [事件总线契约](#事件总线契约)
- [配置项](#配置项)
- [数据库（platform_dict）](#数据库platform_dict)
- [枚举字典](#枚举字典)

---

## 客户端接口（业务方使用）

### `DictService`

业务方注入入口，包路径 `com.ingot.framework.dict.client.DictService`。

| 方法 | 入参 | 返回 | 说明 |
|------|-----|------|-----|
| `items(String)` | `dictCode` | `List<DictItem>` | 等价于 `items(dictCode, DictQuery.platform())` |
| `items(String, DictQuery)` | `dictCode`、作用域条件 | `List<DictItem>` | 按 sort 升序的字典项 |
| `batchItems(List<String>, DictQuery)` | `dictCodes`、作用域 | `Map<String,List<DictItem>>` | 批量查询，缓存层自动合并命中/缺失 |
| `label(String, String)` | `dictCode`、`value` | `String` | 转展示文本，未命中返回原始 `value` |
| `label(String, String, DictQuery)` | 同上 + 作用域 | `String` | 同上 |
| `labelMap(String, DictQuery)` | `dictCode`、作用域 | `Map<String,String>` | value → label 映射 |
| `exists(String, String, DictQuery)` | `dictCode`、`value`、作用域 | `boolean` | 校验值是否在字典中 |
| `findItem(String, String, DictQuery)` | `dictCode`、`value`、作用域 | `Optional<DictItem>` | 取字典项详情 |
| `evict(String)` | `dictCode` | `void` | 清单个字典缓存（沿装饰器链 L1+L2） |
| `evictAll()` | — | `void` | 清所有字典缓存 |
| `static indexBy(List<DictItem>, Function)` | items、key 函数 | `Map<K,DictItem>` | 工具方法，按指定字段索引 |

### `DictItem`

业务输出契约，与 PMS 实体解耦：

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | `Long` | 字典节点 ID |
| `pid` | `Long` | 父字典 ID |
| `code` | `String` | 字典编码 |
| `name` | `String` | 名称 |
| `value` | `String` | 字典项值（仅 ITEM 有效） |
| `label` | `String` | 字典项展示文本 |
| `item` | `boolean` | `true=ITEM，false=TYPE` |
| `scope` | `DictScope` | `PLATFORM / TENANT / APP` |
| `sort` | `Integer` | 排序权重 |
| `enabled` | `boolean` | 是否启用（已过滤逻辑） |
| `remark` | `String` | 备注 |
| `extra` | `Map<String,Object>` | 扩展属性 |

### `DictQuery`

```java
DictQuery.platform();              // PLATFORM
DictQuery.tenant(tenantId);        // TENANT + tenantId
DictQuery.app(appId);              // APP + appId

DictQuery.builder()
    .scope(DictScope.PLATFORM)
    .includeDisabled(true)         // 管理端含禁用项
    .build();
```

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `scope` | `DictScope` | 默认 `PLATFORM` |
| `tenantId` | `Long` | `scope=TENANT` 必填 |
| `appId` | `Long` | `scope=APP` 必填 |
| `includeDisabled` | `boolean` | 默认 `false`（仅返回启用项） |

### `DictScope`

```java
public enum DictScope { PLATFORM, TENANT, APP }
```

---

## 管理端 REST API

包路径 `com.ingot.cloud.pms.web.v1.platform.config.PlatformDictAPI`。

### 字典树

```http
GET /v1/platform/base/dict/tree
权限：platform:base:dict:query
```

Query 参数（`DictQueryDTO`）：

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `code` | string | 否 | 字典编码（精确匹配） |
| `keyword` | string | 否 | 名称（前缀匹配） |
| `type` | `TYPE`/`ITEM` | 否 | 节点类型 |
| `scopeType` | `0`/`1`/`2` | 否 | 作用域，缺省按平台级返回 |
| `tenantId` | long | 条件 | `scopeType=1` 必填 |
| `appId` | long | 条件 | `scopeType=2` 必填 |
| `orgType` | string | 否 | 组织类型 |
| `status` | `0`/`9` | 否 | 状态过滤；管理端可显式查禁用项 |

返回：`R<List<DictTreeNodeVO>>`（嵌套 `children` 字段）。

### 管理端分页

```http
GET /v1/platform/base/dict/page?current=1&size=20&...
权限：platform:base:dict:query
```

返回 `R<IPage<PlatformDict>>`，全字段返回（含 `extra` 与审计字段）。

### 字典项查询

```http
GET /v1/platform/base/dict/items/{code}?scopeType=0&...
权限：platform:base:dict:query
```

返回 `R<List<DictItemVO>>`，仅启用项、按 sort 升序。

### 创建

```http
POST /v1/platform/base/dict
Content-Type: application/json
权限：platform:base:dict:create
```

请求体：`PlatformDict`（`@Validated(Group.Create.class)`）。校验项：

- `type` 非空（`TYPE` / `ITEM`）；
- `name` 非空、≤ 128 字符；
- TYPE 节点：`code` 非空、≤ 64 字符；
- ITEM 节点：`value` 非空、≤ 128 字符；`code` 可省略（不传时后端自动同步父 TYPE 的 `code`，仅作冗余）；
- `label` ≤ 128 字符；
- `scopeType` 默认 `PLATFORM`，`TENANT` 必填 `tenantId`，`APP` 必填 `appId`；
- **TYPE 唯一性**：同 `(scope, tenantId, appId, pid, code)` 唯一；
- **ITEM 唯一性**：同 `(scope, tenantId, appId, pid, value)` 唯一（`code` 不参与）。

### 更新

```http
PUT /v1/platform/base/dict
权限：platform:base:dict:update
```

请求体：`PlatformDict`（`@Validated(Group.Update.class)`），`id` 必填。

> 内置字典（`systemFlag=true`）禁止修改 `code` 与 `value`。

### 切换状态

```http
PATCH /v1/platform/base/dict/{id}/status/{status}
权限：platform:base:dict:update
```

`status` 取值：`0`（启用）/ `9`（禁用）。

### 批量排序

```http
PUT /v1/platform/base/dict/sort
Content-Type: application/json
权限：platform:base:dict:update
```

请求体：

```json
[
  { "id": 1001, "sort": 10 },
  { "id": 1002, "sort": 20 }
]
```

会触发**全量失效广播**（`DictInvalidationEvent.all()`）。

### 删除

```http
DELETE /v1/platform/base/dict/{id}
权限：platform:base:dict:delete
```

仅允许删除叶子节点。`systemFlag=true` 时禁止删除。

---

## 内部 RPC（Feign）

包路径 `com.ingot.cloud.pms.api.rpc.RemotePmsDictService`。供其它微服务通过 `@FeignClient(value = "ingot-pms")` 调用，路由到 PMS 的 `/inner/dict/**`。

### 字典项查询

```http
POST /inner/dict/items?code=user_status
Content-Type: application/json

{
  "scopeType": "0",
  "tenantId": null,
  "appId": null,
  "orgType": null,
  "status": "0"
}
```

返回 `R<List<DictItemVO>>`。仅启用项、按 sort 升序。

### 字典节点查询（含 TYPE）

```http
POST /inner/dict/nodes?code=user_status
```

与 `items` 相比额外包含字典类型节点本身。供需要展示完整树状元数据的场景使用。

### 批量字典项查询

```http
POST /inner/dict/batch?codes=user_status,gender,user_type
Content-Type: application/json

{ "scopeType": "0" }
```

返回 `R<Map<String, List<DictItemVO>>>`，key 为 `dictCode`，value 为对应字典项列表。

> Feign 接口受 `@Permit(mode = PermitMode.INNER)` 限制，仅允许内部网格调用，不直接暴露给外部网关。

---

## 事件总线契约

### `DictInvalidationEvent`

```java
@EventType("dict.invalidate")
public class DictInvalidationEvent extends InvalidationEvent {
    private String dictCode;
    private boolean all;
}
```

JSON 报文示例：

```json
{
  "origin": "ingot-pms:7e3f8c-...",
  "timestamp": 1714050000000,
  "dictCode": "user_status",
  "all": false
}
```

| 字段 | 含义 |
|-----|------|
| `origin` | 发布节点标识；订阅端用于回环过滤（默认 `${spring.application.name}:UUID`） |
| `timestamp` | 发布时间戳（毫秒） |
| `dictCode` | 单字典失效目标；`all=true` 时无意义 |
| `all` | `true` 表示全量失效（清所有字典缓存） |

### Pub/Sub 频道

```
默认：in:bus:dict.invalidate
通用规则：<ingot.event-bus.redis.topic-prefix>:<@EventType.value>
```

观察事件流：

```bash
redis-cli psubscribe 'in:bus:*'
```

### 订阅 / 发布 SPI

```java
public interface InvalidationBus {
    <E extends InvalidationEvent> void publish(E event);
    <E extends InvalidationEvent> Subscription subscribe(Class<E> eventType, Consumer<E> handler);
}
```

`Subscription` 是 `AutoCloseable`，调用 `close()` 取消订阅；`DictCacheCoordinator` 在 bean 销毁时自动调用。

---

## 配置项

### `ingot.dict.client.*`

| Key | 类型 | 默认 | 说明 |
|-----|------|------|-----|
| `mode` | `AUTO` / `LOCAL` / `REMOTE` / `NONE` | `AUTO` | `NONE` 时业务方需自行注入 `DictService` |
| `cache-enabled` | boolean | `true` | L1 Caffeine 开关 |
| `cache-maximum-size` | long | `1024` | L1 最大条目数（按 `(code, scope, tenantId, appId, includeDisabled)` 计） |
| `cache-ttl` | Duration | `5m` | L1 失效时间（写入后） |
| `redis-enabled` | boolean | `true` | L2 Redis 开关 |
| `redis-key-prefix` | string | `in` | L2 Redis key 前缀（与 `IGNORE_TENANT_PREFIX` 一致） |
| `redis-ttl` | Duration | `30m` | L2 Redis 失效时间 |
| `invalidation-enabled` | boolean | `true` | 是否注册 `DictCacheCoordinator` 订阅失效广播 |

### `ingot.event-bus.*`

| Key | 类型 | 默认 | 说明 |
|-----|------|------|-----|
| `type` | `redis` / `none` | `redis` | `none` 时不注册任何 `InvalidationBus` |
| `origin` | string | 自动生成 | 节点标识，缺省 `${spring.application.name}:UUID` |
| `redis.topic-prefix` | string | `in:bus` | Pub/Sub channel 前缀 |

### 完整示例

```yaml
ingot:
  dict:
    client:
      mode: AUTO
      cache-enabled: true
      cache-maximum-size: 2048
      cache-ttl: 10m
      redis-enabled: true
      redis-key-prefix: in
      redis-ttl: 30m
      invalidation-enabled: true

  event-bus:
    type: redis
    redis:
      topic-prefix: in:bus
```

---

## 数据库（`platform_dict`）

### 表结构

```sql
CREATE TABLE `platform_dict` (
  `id`          bigint        NOT NULL,
  `pid`         bigint        DEFAULT NULL                    COMMENT '父ID（字典项指向其字典类型）',
  `code`        varchar(64)   NOT NULL                        COMMENT '字典编码',
  `name`        varchar(128)  NOT NULL                        COMMENT '名称',
  `value`       varchar(128)  DEFAULT NULL                    COMMENT '字典项值（仅 type=ITEM 有效）',
  `label`       varchar(128)  DEFAULT NULL                    COMMENT '字典项展示文本',
  `type`        char(1)       NOT NULL                        COMMENT '0=TYPE 1=ITEM',
  `scope_type`  char(1)       NOT NULL DEFAULT '0'            COMMENT '0=平台 1=租户 2=应用',
  `tenant_id`   bigint        DEFAULT NULL                    COMMENT '租户ID',
  `app_id`      bigint        DEFAULT NULL                    COMMENT '应用ID',
  `org_type`    char(1)       NOT NULL DEFAULT '0'            COMMENT '组织类型',
  `sort`        int           NOT NULL DEFAULT 0              COMMENT '排序权重，越小越靠前',
  `system_flag` bit(1)        NOT NULL DEFAULT b'0'           COMMENT '是否内置字典',
  `status`      char(1)       NOT NULL                        COMMENT '0=正常 9=禁用',
  `remark`      varchar(255)  DEFAULT NULL                    COMMENT '备注',
  `extra`       json          DEFAULT NULL                    COMMENT '扩展属性',
  `created_by`  bigint        DEFAULT NULL                    COMMENT '创建人',
  `updated_by`  bigint        DEFAULT NULL                    COMMENT '更新人',
  `created_at`  datetime      DEFAULT CURRENT_TIMESTAMP       COMMENT '创建时间',
  `updated_at`  datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`  datetime      DEFAULT NULL                    COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_dict_pid`         (`pid`),
  INDEX `idx_dict_code`        (`code`),
  INDEX `idx_dict_type_status` (`type`, `status`),
  INDEX `idx_dict_scope`       (`scope_type`, `tenant_id`, `app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台字典表（企业级字段方案）';
```

### 字段语义

| 字段 | 含义与约束 |
|-----|----------|
| `code` | 业务唯一键；同一 `(scope_type, tenant_id, app_id, pid)` 下唯一 |
| `pid` | TYPE 节点 `pid=null`；ITEM 节点指向所属 TYPE 节点的 `id` |
| `value` | ITEM 的存储值（如 `0` / `male`）；TYPE 节点为 NULL |
| `label` | ITEM 的展示文本（前端选择器主显字段）；TYPE 节点为 NULL |
| `scope_type` | `0=平台`、`1=租户`、`2=应用` |
| `system_flag` | 内置字典标记，禁止删除与修改 `code` / `value`；管理端 UI 应给出禁用提示 |
| `extra` | JSON，约定 key：`icon` / `color` / `i18n` 等；新增 key 需保持向后兼容 |
| `deleted_at` | 逻辑删除（MyBatis-Plus `@TableLogic`） |

### 迁移脚本

| 版本 | 文件 | 说明 |
|-----|------|------|
| V3 | `databases/migrations/003_rename_meta_to_platform.sql` | `meta_dict` → `platform_dict` |
| V4 | `databases/migrations/004_upgrade_platform_dict.sql` | 升级到企业级字段方案 |

回滚脚本：`databases/migrations/rollback_003.sql`。

### 典型查询

按 code 查启用项：

```sql
SELECT id, pid, code, name, value, label, sort, extra
FROM platform_dict
WHERE deleted_at IS NULL
  AND code = 'user_status'
  AND type = '1'
  AND scope_type = '0'
  AND status = '0'
ORDER BY sort ASC;
```

按租户查（`scope_type='1'` + `tenant_id`）：

```sql
SELECT ... FROM platform_dict
WHERE deleted_at IS NULL
  AND code = 'order_status'
  AND scope_type = '1' AND tenant_id = 12345
  AND status = '0'
ORDER BY sort ASC;
```

---

## 枚举字典

### `DictTypeEnum`

| 枚举 | DB 值 | 文本 | 说明 |
|------|------|-----|-----|
| `TYPE` | `0` | 字典类型 | 字典分组，作为 ITEM 的父节点 |
| `ITEM` | `1` | 字典项 | 实际可选值 |

### `DictScopeEnum`

| 枚举 | DB 值 | 文本 | 说明 |
|------|------|-----|-----|
| `PLATFORM` | `0` | 平台 | 跨租户共享，由平台管理员维护 |
| `TENANT` | `1` | 租户 | 仅当前租户可见，可覆盖同 code 的平台字典项 |
| `APP` | `2` | 应用 | 仅当前应用作用域可见，可覆盖同 code 的平台字典项 |

### `CommonStatusEnum`（来自 commons）

| 枚举 | DB 值 | 文本 |
|-----|------|------|
| `ENABLE` | `0` | 启用 |
| `DISABLE` | `9` | 禁用 |

---

## 权限点速查

| 权限 | 含义 |
|------|-----|
| `platform:base:dict:query` | 查询字典（树/分页/字典项） |
| `platform:base:dict:create` | 创建字典节点 |
| `platform:base:dict:update` | 更新字典 / 切换状态 / 批量排序 |
| `platform:base:dict:delete` | 删除字典节点 |

---

## 相关源代码索引

| 类 | 路径 |
|----|------|
| `DictService` | `ingot-framework/ingot-dict-client/src/main/java/com/ingot/framework/dict/client/DictService.java` |
| `DictClientAutoConfiguration` | `ingot-framework/ingot-dict-client/src/main/java/com/ingot/framework/dict/client/config/DictClientAutoConfiguration.java` |
| `CaffeineDictService` | `ingot-framework/ingot-dict-client/src/main/java/com/ingot/framework/dict/client/internal/CaffeineDictService.java` |
| `RedisDictService` | `ingot-framework/ingot-dict-client/src/main/java/com/ingot/framework/dict/client/internal/RedisDictService.java` |
| `DictCacheCoordinator` | `ingot-framework/ingot-dict-client/src/main/java/com/ingot/framework/dict/client/internal/DictCacheCoordinator.java` |
| `DictInvalidationEvent` | `ingot-framework/ingot-dict-client/src/main/java/com/ingot/framework/dict/client/event/DictInvalidationEvent.java` |
| `RemoteDictService` | `ingot-framework/ingot-dict-client/src/main/java/com/ingot/framework/dict/client/remote/RemoteDictService.java` |
| `InvalidationBus` | `ingot-framework/ingot-event-bus/src/main/java/com/ingot/framework/eventbus/InvalidationBus.java` |
| `RedisInvalidationBus` | `ingot-framework/ingot-event-bus/src/main/java/com/ingot/framework/eventbus/redis/RedisInvalidationBus.java` |
| `EventBusAutoConfiguration` | `ingot-framework/ingot-event-bus/src/main/java/com/ingot/framework/eventbus/config/EventBusAutoConfiguration.java` |
| `PlatformDict` | `ingot-service/ingot-pms/ingot-pms-api/src/main/java/com/ingot/cloud/pms/api/model/domain/PlatformDict.java` |
| `RemotePmsDictService` | `ingot-service/ingot-pms/ingot-pms-api/src/main/java/com/ingot/cloud/pms/api/rpc/RemotePmsDictService.java` |
| `PlatformDictAPI` | `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/base/PlatformDictAPI.java` |
| `InnerDictAPI` | `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/inner/InnerDictAPI.java` |
| `LocalDictService` / `LocalDictConfig` | `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/service/dict/` |
| `DictInvalidationPublisher` | `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/service/dict/DictInvalidationPublisher.java` |

---

返回：[模块概述](./README.md) | [架构设计](./ARCHITECTURE.md) | [使用指南](./USAGE.md)
