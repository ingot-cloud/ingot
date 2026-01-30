# 策略加载器架构设计

## 📌 概述

策略加载器（`CredentialPolicyLoader`）是凭证安全模块的核心抽象，负责从不同数据源加载密码策略。通过策略模式和依赖注入，系统支持灵活切换策略来源（本地配置、远程 RPC、数据库等），提供了极高的扩展性和灵活性。

---

## 🎯 设计目标

### 1. **灵活的策略来源**
- ✅ 支持从配置文件加载（Local）
- ✅ 支持从远程服务加载（Remote）
- ✅ 支持从数据库加载（Dynamic）
- ✅ 支持自定义数据源

### 2. **高可用性**
- ✅ 失败降级机制
- ✅ 缓存支持
- ✅ 本地配置兜底

### 3. **简化集成**
- ✅ 配置驱动（`mode: local/remote`）
- ✅ 自动装配
- ✅ 零侵入切换

---

## 🏗️ 架构设计

### 核心接口

```java
public interface CredentialPolicyLoader {
    
    /**
     * 缓存名称
     */
    String CACHE_NAME = "credential:policies";
    
    /**
     * 加载租户的策略列表
     * 
     * @param tenantId 租户ID（null表示全局）
     * @return 策略列表（已按优先级排序）
     */
    List<PasswordPolicy> loadPolicies(Long tenantId);
    
    /**
     * 重新加载策略
     * 
     * @param tenantId 租户ID
     */
    void reloadPolicies(Long tenantId);
    
    /**
     * 清空所有策略缓存
     */
    void clearPolicyCache();
}
```

### 三种实现

```
CredentialPolicyLoader (接口)
        ↑
        ├── LocalCredentialPolicyLoader   - 本地配置加载
        ├── RemoteCredentialPolicyLoader  - 远程 RPC 加载
        └── DynamicCredentialPolicyLoader - 数据库动态加载
```

---

## 📦 实现详解

### 1. LocalCredentialPolicyLoader

**用途：** 从 `application.yml` 加载策略

**特点：**
- ✅ 无需外部依赖
- ✅ 配置即生效
- ✅ 适合开发/测试环境

**配置示例：**

```yaml
ingot:
  credential:
    policy:
      mode: local  # 使用本地模式
      
      strength:
        enabled: true
        min-length: 8
        max-length: 32
        require-uppercase: true
        require-lowercase: true
        require-digit: true
        require-special-char: false
      
      history:
        enabled: true
        check-count: 5
        keep-recent-count: 5
      
      expiration:
        enabled: false
        max-days: 90
        warning-days-before: 7
        grace-login-count: 3
        force-change-after-reset: true
```

**实现逻辑：**

```java
@RequiredArgsConstructor
public class LocalCredentialPolicyLoader implements CredentialPolicyLoader {
    private final CredentialSecurityProperties properties;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Cacheable(value = CACHE_NAME, key = "#tenantId ?: 'global'")
    public List<PasswordPolicy> loadPolicies(Long tenantId) {
        List<PasswordPolicy> policies = new ArrayList<>();
        
        // 从配置创建策略
        if (properties.getPolicy().getStrength().isEnabled()) {
            policies.add(createStrengthPolicy());
        }
        if (properties.getPolicy().getHistory().isEnabled()) {
            policies.add(createHistoryPolicy());
        }
        if (properties.getPolicy().getExpiration().isEnabled()) {
            policies.add(createExpirationPolicy());
        }
        
        // 按优先级排序
        policies.sort(Comparator.comparingInt(PasswordPolicy::getPriority));
        return policies;
    }
}
```

**工作流程：**

```
1. 应用启动
   ↓
2. 读取 CredentialSecurityProperties
   ↓
3. 根据配置创建策略实例
   ├─ 强度策略（priority: 10）
   ├─ 历史策略（priority: 30）
   └─ 过期策略（priority: 20）
   ↓
4. 按优先级排序
   ↓
5. 缓存到 Redis（key: credential:policies:global）
   ↓
6. PasswordValidator 使用策略校验
```

---

### 2. RemoteCredentialPolicyLoader

**用途：** 通过 RPC 从 Credential Service 加载策略

**特点：**
- ✅ 支持动态配置
- ✅ 支持多租户
- ✅ 实时生效
- ⚠️ 需要网络连接

**配置示例：**

```yaml
ingot:
  credential:
    policy:
      mode: remote  # 使用远程模式
```

**依赖：**

```gradle
dependencies {
    implementation project(':ingot-credential-api')
}
```

**实现逻辑：**

```java
@RequiredArgsConstructor
public class RemoteCredentialPolicyLoader implements CredentialPolicyLoader {
    private final RemoteCredentialService remoteCredentialService;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Cacheable(value = CACHE_NAME, key = "#tenantId ?: 'global'")
    public List<PasswordPolicy> loadPolicies(Long tenantId) {
        // 通过 RPC 获取策略配置
        List<CredentialPolicyConfigVO> configs = 
            remoteCredentialService.getPolicyConfigs(tenantId)
                .ifErrorThrow()
                .getData();
        
        // 转换为策略实例
        List<PasswordPolicy> policies = new ArrayList<>();
        for (CredentialPolicyConfigVO config : configs) {
            PasswordPolicy policy = createPolicy(config);
            if (policy != null) {
                policies.add(policy);
            }
        }
        
        // 按优先级排序
        policies.sort(Comparator.comparingInt(PasswordPolicy::getPriority));
        return policies;
    }
    
    private PasswordPolicy createPolicy(CredentialPolicyConfigVO config) {
        return switch (config.getPolicyType()) {
            case STRENGTH -> PasswordPolicyUtil.createStrengthPolicy(
                config.getPolicyConfig(), config.getPriority());
            case HISTORY -> PasswordPolicyUtil.createHistoryPolicy(
                config.getPolicyConfig(), config.getPriority(), passwordEncoder);
            case EXPIRATION -> PasswordPolicyUtil.createExpirationPolicy(
                config.getPolicyConfig(), config.getPriority());
        };
    }
}
```

**工作流程：**

```
1. PasswordValidator 调用 validate()
   ↓
2. RemoteCredentialPolicyLoader.loadPolicies(tenantId)
   ↓
3. 查询缓存（credential:policies:{tenantId}）
   ├─ 缓存命中 → 返回
   └─ 缓存未命中 ↓
4. RPC 调用 → RemoteCredentialService.getPolicyConfigs(tenantId)
   ↓
5. Credential Service 返回策略配置列表
   ↓
6. 使用 PasswordPolicyUtil 创建策略实例
   ↓
7. 按优先级排序
   ↓
8. 缓存到 Redis（TTL: 5分钟）
   ↓
9. 返回策略列表
   ↓
10. PasswordValidator 执行校验
```

**优势：**
- ✅ **动态更新** - 修改配置后实时生效（缓存过期后）
- ✅ **多租户隔离** - 每个租户独立策略
- ✅ **中心化管理** - 统一在 Credential Service 管理

---

### 3. DynamicCredentialPolicyLoader

**用途：** Credential Service 内部使用，从数据库加载策略

**特点：**
- ✅ 数据库持久化
- ✅ 失败降级（本地配置兜底）
- ✅ 高可用

**实现逻辑：**

```java
@Service
@RequiredArgsConstructor
public class DynamicCredentialPolicyLoader implements CredentialPolicyLoader {
    private final PolicyConfigService policyConfigService;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Cacheable(value = CACHE_NAME, key = "#tenantId ?: 'global'")
    public List<PasswordPolicy> loadPolicies(Long tenantId) {
        try {
            // 1. 从数据库加载
            List<CredentialPolicyConfig> configs = 
                policyConfigService.getAllPolicyConfigs(tenantId);
            return createPolicies(configs);
        } catch (Exception e) {
            log.error("数据库加载策略失败，使用本地配置兜底", e);
            // 2. 失败时从配置文件兜底
            return loadFallbackPolicies();
        }
    }
    
    private List<PasswordPolicy> loadFallbackPolicies() {
        // 从 application.yml 加载默认策略
        return localPolicyLoader.loadPolicies(null);
    }
}
```

**高可用设计：**

```
┌─────────────────┐
│ loadPolicies()  │
└────────┬────────┘
         │
         ▼
┌─────────────────────┐
│ 尝试从数据库加载     │
└────────┬────────────┘
         │
    ┌────┴────┐
    │ 成功？   │
    └────┬────┘
         │
    ┌────┴─────┐
    │Yes      No│
    ▼          ▼
┌────────┐ ┌──────────────────┐
│ 返回   │ │ 日志错误          │
└────────┘ │ 使用本地配置兜底  │
           └────────┬──────────┘
                    │
                    ▼
           ┌────────────────┐
           │ 返回默认策略    │
           └────────────────┘
```

---

## 🔄 策略工具类

`PasswordPolicyUtil` 提供统一的策略创建方法：

```java
public class PasswordPolicyUtil {
    
    /**
     * 创建密码强度策略
     */
    public static PasswordStrengthPolicy createStrengthPolicy(
            Map<String, Object> config, int priority) {
        
        PasswordStrengthPolicy policy = new PasswordStrengthPolicy() {
            @Override
            public int getPriority() {
                return priority;
            }
        };
        
        // 设置配置参数
        if (config.containsKey("minLength")) {
            policy.setMinLength(((Number) config.get("minLength")).intValue());
        }
        // ... 其他参数
        
        return policy;
    }
    
    /**
     * 创建密码历史策略
     */
    public static PasswordHistoryPolicy createHistoryPolicy(
            Map<String, Object> config, int priority, PasswordEncoder encoder) {
        // ...
    }
    
    /**
     * 创建密码过期策略
     */
    public static PasswordExpirationPolicy createExpirationPolicy(
            Map<String, Object> config, int priority) {
        // ...
    }
}
```

**使用场景：**
- RemoteCredentialPolicyLoader - 从 RPC 返回的 Map 创建策略
- DynamicCredentialPolicyLoader - 从数据库配置创建策略

---

## 🎚️ 配置切换

### 自动配置逻辑

```java
@AutoConfiguration
public class CredentialSecurityAutoConfiguration {
    
    // Local 模式（默认）
    @Bean
    @ConditionalOnMissingBean(CredentialPolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.credential.policy.mode", 
                          havingValue = "local", 
                          matchIfMissing = true)
    public CredentialPolicyLoader localLoader(
            CredentialSecurityProperties properties,
            PasswordEncoder passwordEncoder) {
        return new LocalCredentialPolicyLoader(properties, passwordEncoder);
    }
    
    // Remote 模式
    @Bean
    @ConditionalOnMissingBean(CredentialPolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.credential.policy.mode", 
                          havingValue = "remote")
    public CredentialPolicyLoader remoteLoader(
            RemoteCredentialService remoteService,
            PasswordEncoder passwordEncoder) {
        return new RemoteCredentialPolicyLoader(remoteService, passwordEncoder);
    }
}
```

### 配置示例

**开发环境（Local）：**

```yaml
# application-dev.yml
ingot:
  credential:
    policy:
      mode: local
      strength:
        enabled: true
        min-length: 6  # 宽松要求
```

**生产环境（Remote）：**

```yaml
# application-prod.yml
ingot:
  credential:
    policy:
      mode: remote  # 从 Credential Service 加载
```

---

## 🔍 使用场景

### 场景 1：开发/测试环境

```yaml
ingot:
  credential:
    policy:
      mode: local  # 本地模式
      strength:
        min-length: 6  # 宽松策略，方便测试
```

**优势：**
- ✅ 无需部署 Credential Service
- ✅ 快速迭代，修改配置即生效
- ✅ 无网络依赖

---

### 场景 2：生产环境（单租户）

```yaml
ingot:
  credential:
    policy:
      mode: remote  # 远程模式
```

通过管理后台配置策略 → 实时生效

**优势：**
- ✅ 动态调整策略，无需重启
- ✅ 统一管理

---

### 场景 3：生产环境（多租户）

```yaml
ingot:
  credential:
    policy:
      mode: remote  # 远程模式
```

每个租户独立策略配置：

```sql
-- 租户 A（金融行业）- 高安全要求
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled) 
VALUES 
(1001, 'STRENGTH', '{"minLength": 12, "requireSpecialChar": true}', 10, 1);

-- 租户 B（教育行业）- 中等要求
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled) 
VALUES 
(1002, 'STRENGTH', '{"minLength": 8, "requireSpecialChar": false}', 10, 1);
```

**优势：**
- ✅ 租户级策略隔离
- ✅ 满足不同行业合规要求

---

### 场景 4：混合部署

Auth Service（无数据库）使用 Remote 模式：

```yaml
# auth-service/application.yml
ingot:
  credential:
    policy:
      mode: remote
```

Member/PMS Service 可选择 Local 或 Remote：

```yaml
# member-service/application.yml
ingot:
  credential:
    policy:
      mode: local  # 或 remote
```

---

## 🎯 最佳实践

### 1. 模式选择

| 环境 | 推荐模式 | 原因 |
|-----|---------|------|
| 本地开发 | Local | 简单、快速 |
| CI/CD | Local | 稳定、可预测 |
| 测试环境 | Remote | 模拟生产 |
| 生产环境（单租户） | Remote | 动态配置 |
| 生产环境（多租户） | Remote | 租户隔离 |

### 2. 缓存策略

**Local 模式：**
- 应用启动时加载一次
- 缓存永久有效（直到重启）

**Remote 模式：**
- 首次请求时加载
- 缓存 TTL: 5分钟
- 修改策略后，最多 5分钟生效

**主动刷新：**

```java
@Autowired
private CredentialPolicyLoader policyLoader;

// 修改策略后，主动刷新
policyLoader.reloadPolicies(tenantId);

// 或清空所有缓存
policyLoader.clearPolicyCache();
```

### 3. 故障降级

**推荐配置：**

```java
// Credential Service 内部
@Service
public class DynamicCredentialPolicyLoader implements CredentialPolicyLoader {
    
    @Override
    public List<PasswordPolicy> loadPolicies(Long tenantId) {
        try {
            return loadFromDatabase(tenantId);
        } catch (Exception e) {
            log.error("数据库加载失败，使用本地配置兜底", e);
            return loadFallbackPolicies();  // 兜底
        }
    }
}
```

**保障措施：**
1. 数据库加载失败 → 本地配置兜底
2. 本地配置缺失 → 使用默认策略
3. 默认策略：最低安全要求（min-length: 8）

---

## 🔮 扩展性

### 自定义策略加载器

```java
@Component
public class EtcdPolicyLoader implements CredentialPolicyLoader {
    
    @Autowired
    private EtcdClient etcdClient;
    
    @Override
    @Cacheable(value = CACHE_NAME, key = "#tenantId ?: 'global'")
    public List<PasswordPolicy> loadPolicies(Long tenantId) {
        // 从 Etcd 加载策略
        String key = "/ingot/policies/" + (tenantId != null ? tenantId : "global");
        String json = etcdClient.get(key);
        
        // 解析并创建策略
        return parsePolicies(json);
    }
    
    @Override
    @CacheEvict(value = CACHE_NAME, key = "#tenantId ?: 'global'")
    public void reloadPolicies(Long tenantId) {
        // 刷新缓存
    }
    
    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearPolicyCache() {
        // 清空所有缓存
    }
}
```

**使用自定义加载器：**

```java
@Configuration
public class CustomPolicyLoaderConfig {
    
    @Bean
    @Primary  // 优先使用
    public CredentialPolicyLoader etcdPolicyLoader(EtcdClient etcdClient) {
        return new EtcdPolicyLoader(etcdClient);
    }
}
```

---

## 📊 性能指标

| 场景 | Local 模式 | Remote 模式（缓存命中） | Remote 模式（缓存未命中） |
|-----|-----------|---------------------|---------------------|
| 策略加载 | < 5ms | < 5ms | 20-50ms |
| 内存占用 | ~1KB | ~1KB | ~1KB |
| 网络调用 | 0 | 0 | 1次 RPC |

**优化建议：**
1. ✅ 合理设置缓存 TTL（默认 5分钟）
2. ✅ 避免频繁刷新缓存
3. ✅ 生产环境启用 Redis 缓存

---

## 📞 常见问题

### Q1: 如何切换模式？

**A:** 修改配置 + 重启应用

```yaml
ingot:
  credential:
    policy:
      mode: remote  # 改为 remote
```

如果切换到 remote，还需添加依赖：

```gradle
implementation project(':ingot-credential-api')
```

### Q2: Remote 模式下策略不生效？

**A:** 检查：
1. 缓存是否过期（默认 5分钟）
2. 主动刷新缓存：`policyLoader.reloadPolicies(tenantId)`
3. Credential Service 是否正常运行

### Q3: 如何支持热更新？

**A:** 使用 Remote 模式 + 主动刷新：

```java
// 修改策略后
@PostMapping("/policy/refresh")
public R<Void> refreshPolicy(@RequestParam Long tenantId) {
    policyLoader.reloadPolicies(tenantId);
    return R.ok();
}
```

### Q4: DynamicCredentialPolicyLoader 何时使用？

**A:** 仅在 Credential Service 内部使用，其他服务使用 Local 或 Remote 模式。

---

## 📄 总结

策略加载器架构提供了：

1. ✅ **灵活性** - 支持多种数据源
2. ✅ **可扩展** - 易于添加新的加载器
3. ✅ **高可用** - 失败降级机制
4. ✅ **简化集成** - 配置驱动，零侵入

**设计原则：**
- 接口抽象，实现分离
- 策略模式，灵活切换
- 依赖注入，自动装配
- 缓存优先，性能保障

---

**版本：** v1.0  
**更新日期：** 2026-01-30  
**作者：** Ingot Cloud Team
