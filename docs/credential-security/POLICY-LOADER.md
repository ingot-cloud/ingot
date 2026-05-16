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
  security:
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

**核心流程：**

应用启动时从配置文件读取策略参数，创建策略实例并缓存，供 PasswordValidator 使用。

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
  security:
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

**核心流程：**

通过 RPC 从 Credential Service 获取策略配置，使用 PasswordPolicyUtil 创建策略实例，缓存5分钟。

**优势：**
- ✅ 支持动态更新和多租户隔离
- ✅ 中心化管理，统一配置

---

### 3. DynamicCredentialPolicyLoader

**用途：** Credential Service 内部使用，从数据库加载策略

**特点：**
- ✅ 数据库持久化
- ✅ 失败降级（本地配置兜底）
- ✅ 高可用

**核心特点：**

从数据库加载策略，失败时自动降级到本地配置兜底，确保高可用。

---

## 🔄 策略工具类

`PasswordPolicyUtil` 提供统一的策略创建方法，支持从 Map 配置创建策略实例，用于 Remote 和 Dynamic 加载器。

---

## 🎚️ 配置切换

### 自动配置逻辑

通过 Spring Boot 自动配置，根据 `ingot.security.credential.policy.mode` 属性值自动选择对应的加载器实现（local 或 remote）。

### 配置示例

**开发环境（Local）：**

```yaml
# application-dev.yml
ingot:
  security:
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
  security:
    credential:
      policy:
        mode: remote  # 从 Credential Service 加载
```

---

## 🔍 使用场景

### 场景 1：开发/测试环境

```yaml
ingot:
  security:
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
  security:
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
  security:
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
  security:
    credential:
      policy:
        mode: remote
```

Member/PMS Service 可选择 Local 或 Remote：

```yaml
# member-service/application.yml
ingot:
  security:
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

支持自定义策略加载器，只需实现 `CredentialPolicyLoader` 接口并注册为 Spring Bean（使用 @Primary 优先）。系统会自动使用自定义加载器。

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
  security:
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
