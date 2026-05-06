# 字典模块使用指南

> 面向后端开发者，介绍如何在业务代码中接入字典、常用配置、最佳实践与典型反例。

## 1. 接入步骤

### 1.1 添加依赖

任意微服务（包括 PMS 自身）：

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-dict-client')
}
```

跨服务调用 PMS 字典还需要依赖 PMS 的 API 包以引入 Feign 接口（`ingot-pms-api`）。**通常项目里已经依赖**，无需额外配置。

事件总线模块 `ingot-event-bus` 是 `ingot-dict-client` 的传递依赖，无需显式添加。Spring Boot 会通过自动配置在类路径存在 Redis 时启用 `RedisInvalidationBus`。

### 1.2 写入配置（可选）

`ingot-dict-client` 与 `ingot-event-bus` 均提供合理默认值，**零配置即可工作**。如需调优：

```yaml
ingot:
  dict:
    client:
      mode: AUTO              # AUTO | LOCAL | REMOTE | NONE
      cache-enabled: true
      cache-maximum-size: 1024
      cache-ttl: 5m
      redis-enabled: true
      redis-key-prefix: in
      redis-ttl: 30m
      invalidation-enabled: true

  event-bus:
    type: redis               # redis | none
    # origin:                 # 留空则自动生成 ${spring.application.name}:UUID
    redis:
      topic-prefix: in:bus
```

### 1.3 注入并使用

```java
@Service
@RequiredArgsConstructor
public class UserBizService {

    private final DictService dictService;

    public List<DictItem> userStatusList() {
        return dictService.items("user_status");
    }
}
```

业务方**只需要面向 `DictService` 接口编程**，无需关心是否运行在 PMS 内、是否启用了缓存或失效广播。

---

## 2. 常用 API

### 2.1 读取字典项列表

```java
// 等价于 DictQuery.platform()
List<DictItem> items = dictService.items("user_status");

// 显式指定作用域
List<DictItem> tenantItems = dictService.items("user_status", DictQuery.tenant(tenantId));
List<DictItem> appItems    = dictService.items("user_status", DictQuery.app(appId));

// 包含禁用项（通常仅管理端使用）
DictQuery includeDisabled = DictQuery.builder()
        .scope(DictScope.PLATFORM)
        .includeDisabled(true)
        .build();
List<DictItem> all = dictService.items("user_status", includeDisabled);
```

返回的 `DictItem` 已按 `sort` 升序排列。

### 2.2 取展示文本（label）

```java
// 把存储值翻译成展示文本；找不到时返回原始 value，避免抛异常
String label = dictService.label("user_status", "0");          // "正常"
String unknown = dictService.label("user_status", "99");       // "99"

// 配合 DictQuery 使用租户 / 应用作用域
String tenantLabel = dictService.label("user_status", "0", DictQuery.tenant(1L));
```

### 2.3 取 value → label 映射

适合管理端列表回填、Excel 导入导出：

```java
Map<String, String> map = dictService.labelMap("user_status", DictQuery.platform());
// {"0":"正常","9":"禁用"}
```

### 2.4 校验值是否合法

```java
boolean valid = dictService.exists("user_status", "0", DictQuery.platform());
// 表单校验时使用，避免存入字典中没有的值
```

### 2.5 取字典项详情（含 extra）

```java
Optional<DictItem> opt = dictService.findItem("user_status", "0", DictQuery.platform());
opt.ifPresent(item -> {
    String icon = item.getExtra() == null ? null : (String) item.getExtra().get("icon");
});
```

### 2.6 批量读取

字典页面通常一次需要多个字典；批量调用避免 N 次 RPC：

```java
List<String> codes = List.of("user_status", "gender", "user_type");
Map<String, List<DictItem>> all = dictService.batchItems(codes, DictQuery.platform());
all.forEach((code, list) -> log.info("{} -> {}", code, list.size()));
```

`batchItems` 在 L1/L2 缓存层会自动做"命中跳过、缺失合并回源"——例如 3 个字典中 2 个已在 L1 命中，仅未命中的 1 个会发起一次回源请求。

### 2.7 工具方法

```java
List<DictItem> items = dictService.items("user_status");

// 按 value 索引
Map<String, DictItem> byValue = DictService.indexBy(items, DictItem::getValue);

// 按 ID 索引
Map<Long, DictItem> byId = DictService.indexBy(items, DictItem::getId);
```

---

## 3. 写操作（PMS 管理端）

字典写操作只暴露在 PMS 管理端，不通过 RPC 提供（避免任意服务"擅自"修改全局字典）。

```http
POST   /v1/platform/base/dict                         创建
PUT    /v1/platform/base/dict                         更新
DELETE /v1/platform/base/dict/{id}                    删除
PATCH  /v1/platform/base/dict/{id}/status/{status}    启用/禁用
PUT    /v1/platform/base/dict/sort                    批量排序
GET    /v1/platform/base/dict/tree                    字典树
GET    /v1/platform/base/dict/page                    管理端分页
GET    /v1/platform/base/dict/items/{code}            字典项查询
```

字段定义与权限点参见 [API / RPC 参考](./API-REFERENCE.md#管理端-rest-api)。

任何写操作（含 `batchSort`）在事务提交后会自动触发跨节点失效，业务方**无需手动 evict**。

---

## 4. 字典数据建模

### 4.1 字典类型 vs 字典项

`platform_dict` 是单表两层树：

```
[TYPE]  user_status  "用户状态"
   ├─ [ITEM]  value=0  label=正常  sort=10
   └─ [ITEM]  value=9  label=禁用  sort=20

[TYPE]  gender       "性别"
   ├─ [ITEM]  value=M  label=男    sort=10
   └─ [ITEM]  value=F  label=女    sort=20
```

- TYPE 节点：`type=TYPE`、`pid=null`、`code` **必填且唯一**（如 `user_status`）、`value/label` 不填；
- ITEM 节点：`type=ITEM`、`pid=<TYPE 节点 ID>`、`value` **必填且在父 TYPE 内唯一**、`label` 是展示文本；
  `code` 不必填，写库时由后端自动同步父 TYPE 的 `code`，**不参与唯一性约束**——只是冗余字段，便于直接 SQL 排查时按 `code` 过滤同一字典所有节点。

> ⚠️ ITEM 的业务唯一键是 **`(pid, value)`**，不是 `(pid, code)`。同一字典类型下不可有重复 `value`。

读路径 `dictService.items("user_status")` 返回的是 ITEM 列表，已经按 sort 升序排列、剔除禁用项。

### 4.2 三种作用域

| `scopeType` | 含义 | 典型用途 |
|------------|------|---------|
| `PLATFORM` | 平台共享，所有租户 / 应用都能看到 | 系统级枚举：性别、用户状态、消息类型 |
| `TENANT` | 仅本租户可见 | 租户自定义枚举：行业类型、客户等级 |
| `APP` | 仅本应用可见 | 应用内业务枚举：订单状态机、SKU 分类 |

**读路径不会做"PLATFORM 兜底叠加"**——`DictQuery.tenant(1L)` 仅返回 `scope=TENANT, tenantId=1` 的数据。如果业务需要"租户没配则用平台默认"的语义，请在调用方按需先查租户、为空时再查平台：

```java
List<DictItem> tenant = dictService.items("user_status", DictQuery.tenant(tenantId));
if (tenant.isEmpty()) {
    tenant = dictService.items("user_status");          // 退回平台
}
```

> 这种显式策略避免缓存层做出不可预期的"合并"，简化心智模型。

### 4.3 扩展字段 `extra`

`extra` 是一个 `Map<String, Object>`，用于承载图标、颜色、i18n 等业务字段：

```json
{
  "icon": "check-circle",
  "color": "#52c41a",
  "i18n": {
    "en_US": "Normal",
    "zh_CN": "正常"
  }
}
```

数据库字段是 MySQL JSON 类型，MyBatis-Plus 通过 `JacksonTypeHandler` 自动序列化。读端：

```java
DictItem item = dictService.findItem("user_status", "0", DictQuery.platform()).orElseThrow();
Map<String, Object> extra = item.getExtra();
String color = extra == null ? null : (String) extra.get("color");
```

> **建议**：`extra` 中的 key 在团队内统一约定，不要每个开发者随意命名；可以把约定写在 `docs/dict/EXTRA-CONVENTIONS.md`（按需追加）。

---

## 5. 缓存与失效

### 5.1 默认行为

| 行为 | 默认 |
|-----|------|
| L1 Caffeine | 启用，5 分钟 TTL，最大 1024 条 |
| L2 Redis | 启用，30 分钟 TTL，key 前缀 `in` |
| 失效广播 | 启用，channel `in:bus:dict.invalidate` |

业务方做"读"操作时**永远不需要手动 evict**——PMS 的写操作会自动触发跨节点失效。

### 5.2 何时需要手动 `evict`

仅在以下罕见场景：

1. **绕过 PMS 直接写库**（例如运维通过 SQL 直接修改 `platform_dict`）：
   ```java
   dictService.evict("user_status");      // 单 code
   dictService.evictAll();                 // 全量
   ```
   注意：这种情况下其它节点不会自动收到广播，需要在每个节点都手动调用，或者通过 PMS 提供的"刷新"运维接口（如有）发起一次伪写操作触发广播。

2. **本地调试**：希望立即看到修改后的字典，最简单是重启服务或调用 `evictAll()`。

### 5.3 关闭缓存（不推荐）

调试或定位问题时，可以临时关闭 L1：

```yaml
ingot:
  dict:
    client:
      cache-enabled: false      # 关 L1
      redis-enabled: false      # 关 L2
```

或将客户端模式改为 `NONE`（业务方自行注入实现）：

```yaml
ingot:
  dict:
    client:
      mode: NONE
```

> 生产环境强烈建议保持默认（开启 L1+L2），否则 PMS 数据库压力会显著上升。

### 5.4 关闭失效广播（不推荐）

如果不依赖事件总线（例如单实例部署、本地开发）：

```yaml
ingot:
  event-bus:
    type: none
```

此时 `DictCacheCoordinator` 不会注册，PMS 的写操作仍会清自身 Redis L2，但其它节点的 L1 只能等 TTL 自然过期（默认 5 分钟）后才能感知到变更。

---

## 6. 与 PMS 写操作的协作

PMS 内部业务代码若需要在自定义流程中"通知字典变更"，应当发布 `DictChangedSpringEvent`：

```java
@Service
@RequiredArgsConstructor
public class CustomDictBusinessService {

    private final ApplicationEventPublisher publisher;

    @Transactional
    public void customAction(String dictCode) {
        // ... 写库逻辑
        publisher.publishEvent(DictChangedSpringEvent.of(this, dictCode));
    }
}
```

`DictInvalidationPublisher` 会在事务提交后自动接管：清自身 L2 + 广播失效事件。`DictChangedSpringEvent` 是 PMS 内部 API，不应该由其它微服务直接发布；其它微服务只读字典，不应该发起字典级广播。

---

## 7. 最佳实践

### ✅ 推荐做法

1. **统一通过 `DictService` 访问字典**，不要直接注入 `BizPlatformDictService` 或 `RemotePmsDictService`，否则会绕过缓存。
2. **在表单校验、状态机入口使用 `exists` 而不是手动 `contains`**：
   ```java
   if (!dictService.exists("order_status", value, DictQuery.platform())) {
       throw new BusinessException("非法订单状态：" + value);
   }
   ```
3. **列表展示用 `labelMap` 一次拿全**，避免 N+1：
   ```java
   Map<String, String> labels = dictService.labelMap("user_status", DictQuery.platform());
   for (UserVO u : list) {
       u.setStatusLabel(labels.getOrDefault(u.getStatus(), u.getStatus()));
   }
   ```
4. **批量请求多个字典走 `batchItems`**，让缓存层做合并：
   ```java
   Map<String, List<DictItem>> all = dictService.batchItems(
           List.of("user_status", "gender", "user_type"),
           DictQuery.platform());
   ```
5. **`extra` 字段保留向后兼容**：新增 key 不影响老调用方，删除 key 之前先确认所有消费方是否使用。

### ❌ 反例

1. **❌ 在循环里调 `label`**——每次都会触发 `items` 调用：
   ```java
   for (UserVO u : list) {
       u.setStatusLabel(dictService.label("user_status", u.getStatus()));   // ❌
   }
   ```
   即便 L1 命中也意味着每条记录一次 `Caffeine.get`，比 `labelMap` 一次性拿到 Map 慢得多。

2. **❌ 直接读 `PlatformDict` 实体或 SQL**——绕过缓存，且会引入 PMS 数据库依赖：
   ```java
   @Autowired
   PlatformDictMapper mapper;       // ❌ 业务代码不应直接接触
   ```

3. **❌ 把 `DictItem.extra` 当强类型用**：
   ```java
   MyDto dto = (MyDto) item.getExtra().get("config");   // ❌ ClassCastException 风险
   ```
   正确做法：先取 `Map`/基本类型，再用 Jackson 转换：
   ```java
   ObjectMapper om = ...;
   MyDto dto = om.convertValue(item.getExtra().get("config"), MyDto.class);
   ```

4. **❌ 手动维护"字典常量类"**：
   ```java
   public class UserStatus {
       public static final String NORMAL = "0";
       public static final String DISABLED = "9";
   }
   ```
   字典存在的意义就是把枚举值的"展示语义"和"存储值"解耦给运营配置。常量类只在写代码时用作可读性辅助，**不要替代字典存在**——否则一旦运营增加新字典项，代码就要重新发布。

5. **❌ 在写完字典后立即在同一线程读期望看到新值**：
   ```java
   bizDictService.update(params);
   List<DictItem> latest = dictService.items(params.getCode());  // ❌ 可能仍是旧值
   ```
   写路径走 MyBatis 不会清 L1；如果业务确需立即可见，请显式 `dictService.evict(params.getCode())`，但更好的做法是不要让单元业务依赖"立即一致"。

---

## 8. 调试与排查

### 8.1 确认装配模式

启动日志会输出客户端组合情况：

```
[DictClient] register local delegate (LocalDictService)            ← PMS 进程
[DictClient] register remote delegate (RemoteDictService)           ← 其它服务
[DictClient] L2 Redis layer enabled, ttl=PT30M, keyPrefix=in
[DictClient] DictService composed (mode=AUTO, l1=true, l2=true)
[DictClient] cache coordinator subscribed                           ← 失效广播已订阅
[EventBus] initialized RedisInvalidationBus origin=ingot-pms:xxx, topicPrefix=in:bus
```

如果看不到 `cache coordinator subscribed`，检查：

1. `ingot.dict.client.invalidation-enabled` 是否为 `false`；
2. `ingot.event-bus.type` 是否为 `none`；
3. 是否缺少 Redis 依赖。

### 8.2 监听 Pub/Sub

直接在 Redis CLI 上观察广播：

```bash
redis-cli psubscribe 'in:bus:*'

# 触发一次 PMS 字典更新后会看到：
# 1) "pmessage"
# 2) "in:bus:*"
# 3) "in:bus:dict.invalidate"
# 4) "{\"origin\":\"ingot-pms:xxx\",\"timestamp\":1714...,\"dictCode\":\"user_status\",\"all\":false}"
```

### 8.3 验证缓存键

```bash
redis-cli keys 'in:dict:items:*'
# in:dict:items:user_status:PLATFORM:_:_:0
# in:dict:items:gender:PLATFORM:_:_:0

redis-cli get 'in:dict:items:user_status:PLATFORM:_:_:0'
# JSON 数组
```

### 8.4 强制清空（运维）

```bash
redis-cli --scan --pattern 'in:dict:items:*' | xargs -r redis-cli del
```

或在任意 PMS 实例触发一次空操作（例如调一次"切换状态再切回来"），让所有节点 L1 跟着失效。

### 8.5 打开 DEBUG 日志

```yaml
logging:
  level:
    com.ingot.framework.dict.client: DEBUG
    com.ingot.framework.eventbus: DEBUG
```

---

## 9. 单元测试建议

### 9.1 业务代码 mock `DictService`

```java
@ExtendWith(MockitoExtension.class)
class UserBizServiceTest {

    @Mock DictService dictService;
    @InjectMocks UserBizService bizService;

    @Test
    void shouldUseLabelFromDict() {
        when(dictService.label("user_status", "0")).thenReturn("正常");

        String label = bizService.statusLabel("0");

        assertThat(label).isEqualTo("正常");
    }
}
```

### 9.2 集成测试关闭缓存

避免缓存导致测试间相互影响：

```yaml
# application-test.yml
ingot:
  dict:
    client:
      cache-enabled: false
      redis-enabled: false
      invalidation-enabled: false
  event-bus:
    type: none
```

---

下一步：[API / RPC 参考](./API-REFERENCE.md) | [架构设计](./ARCHITECTURE.md)
