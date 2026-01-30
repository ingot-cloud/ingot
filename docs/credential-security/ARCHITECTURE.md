# 凭证安全架构设计

## 设计目标

1. **统一策略** - 所有密码规则和凭证策略在一个地方定义和管理
2. **解耦但协同** - 与认证系统解耦，但通过标准接口协同工作
3. **联邦式数据** - 策略集中管理，数据分散存储，服务自治
4. **易于演进** - 支持未来扩展 MFA、Passkey 等高级凭证类型

## 架构分层

```
┌─────────────────────────────────────────────────────────────────────┐
│                         业务层 (Business Layer)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │ Auth Service │  │ PMS Service  │  │Member Service│               │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘               │
│         │                 │                 │                       │
│         │ 本地集成         │ RPC调用          │ RPC调用                │
│         ▼                 ▼                  ▼                      │
├─────────────────────────────────────────────────────────────────────┤
│                      Framework 层 (Framework Layer)                 │
│              ┌────────────────────────────────────┐                 │
│              │ Credential Security Framework      │                 │
│              │  (无状态策略校验引擎)                 │                 │
│              └────────────────────────────────────┘                 │
│              - PasswordValidator (密码校验器)                        │
│              - PasswordPolicy (策略接口)                             │
│              - PasswordCheckResult (校验结果)                        │
│              - CredentialStatus (凭证状态枚举)                       │
├─────────────────────────────────────────────────────────────────────┤
│                      Service 层 (Service Layer)                     │
│              ┌────────────────────────────────────┐                 │
│              │ Credential Security Service        │                 │
│              │  (独立微服务)                       │                 │
│              └────────────────────────────────────┘                 │
│              - RemoteCredentialService (RPC接口)                     │
│              - PolicyConfigService (策略管理)                        │
│              - AuditService (审计日志)                               │
├─────────────────────────────────────────────────────────────────────┤
│                      数据层 (Data Layer)                            │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐ │
│  │ PMS Database     │  │ Member Database  │  │ Credential DB    │ │
│  │ - password_hist  │  │ - password_hist  │  │ - policy_config  │ │
│  │ - password_exp   │  │ - password_exp   │  │ - audit_log      │ │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘ │
│      (联邦式数据)           (联邦式数据)           (集中式数据)         │
└─────────────────────────────────────────────────────────────────────┘
```

## 系统交互流程

### 用户注册/修改密码流程

```
用户提交密码
    ↓
Member/PMS Service
    ↓
RPC 调用 validatePasswordStrength()
    ↓
Credential Security Service
    ↓
加载策略配置 (带缓存)
    ↓
调用 Framework 层校验器
    ↓
返回校验结果
    ↓
如果通过：记录密码历史 (本地数据库)
    ↓
如果通过：记录审计日志 (远程服务)
    ↓
返回成功
```

### 用户登录流程

```
用户提交凭证
    ↓
Auth Service
    ↓
调用本地 Framework 层
    ↓
校验密码正确性 (PasswordEncoder)
    ↓
检查密码状态 (过期、强制修改)
    ↓
如果密码过期：返回 PasswordExpiredException
如果需强制修改：在 Token 中添加标记
    ↓
Gateway 检查 Token 标记
    ↓
限制访问范围 (只允许修改密码)
```

## 核心类图

```
┌─────────────────────────────────────┐
│        «interface»                  │
│        PasswordPolicy               │
├─────────────────────────────────────┤
│ + getName(): String                 │
│ + getPriority(): int                │
│ + check(context): PolicyCheckResult │
│ + isEnabled(tenant): boolean        │
└────────────▲────────────────────────┘
             │
             │ 实现
             │
   ┌─────────┴─────────┬─────────────┬──────────────┐
   │                   │             │              │
┌──┴──────────────┐ ┌──┴───────────┐ ┌┴────────────┐ ┌┴─────────────┐
│PasswordStrength│ │PasswordExpir-│ │PasswordHist-│ │CustomPolicy  │
│Policy          │ │ationPolicy   │ │oryPolicy    │ │(扩展点)      │
└────────────────┘ └──────────────┘ └─────────────┘ └──────────────┘


┌─────────────────────────────────────┐
│        PasswordValidator            │
├─────────────────────────────────────┤
│ - policies: List<PasswordPolicy>    │
├─────────────────────────────────────┤
│ + validate(context): Result         │
│ # applyPolicies(): void             │
└─────────────────────────────────────┘
             │
             │ 使用
             ▼
┌─────────────────────────────────────┐
│     PolicyCheckContext              │
├─────────────────────────────────────┤
│ - password: String                  │
│ - username: String                  │
│ - tenantId: Long                    │
│ - userType: String                  │
│ - oldPasswordHash: String           │
└─────────────────────────────────────┘
             │
             │ 产生
             ▼
┌─────────────────────────────────────┐
│     PasswordCheckResult             │
├─────────────────────────────────────┤
│ - passed: boolean                   │
│ - failureReasons: List<String>      │
│ - warnings: List<String>            │
│ - metadata: Map<String, Object>     │
├─────────────────────────────────────┤
│ + isPassed(): boolean               │
│ + hasWarnings(): boolean            │
│ + getFailureMessage(): String       │
└─────────────────────────────────────┘
```

## 模块依赖关系

```
ingot-security-credential (Framework)
    ├── policy/
    │   ├── PasswordPolicy (接口)
    │   ├── PasswordStrengthPolicy
    │   ├── PasswordExpirationPolicy
    │   └── PasswordHistoryPolicy
    ├── validator/
    │   ├── PasswordValidator
    │   └── ValidatorChain
    ├── model/
    │   ├── PasswordCheckResult
    │   ├── PolicyCheckContext
    │   └── CredentialStatus (枚举)
    ├── exception/
    │   ├── PasswordWeakException
    │   ├── PasswordExpiredException
    │   └── PasswordReusedException
    └── config/
        └── CredentialSecurityProperties

ingot-credential (独立微服务)
    ├── api/
    │   ├── dto/
    │   │   ├── ValidatePasswordDTO
    │   │   ├── ChangePasswordDTO
    │   │   └── PolicyConfigDTO
    │   ├── vo/
    │   │   ├── PasswordCheckResultVO
    │   │   └── PolicyVO
    │   └── rpc/
    │       ├── RemoteCredentialService
    │       └── RemotePolicyService
    └── provider/
        ├── service/
        │   ├── PolicyService
        │   ├── AuditService
        │   └── ValidationService
        ├── mapper/
        │   ├── PolicyConfigMapper
        │   └── CredentialAuditMapper
        └── web/
            └── v1/
                ├── PolicyAPI
                └── AuditAPI

Auth Service
    ├── 依赖: ingot-security-credential
    └── 本地集成 Framework (无远程调用)

Member Service
    ├── 依赖: ingot-security-credential
    ├── 依赖: ingot-credential-api
    ├── 管理自己的密码历史表
    └── RPC 调用 Credential Security Service

PMS Service
    ├── 依赖: ingot-security-credential
    ├── 依赖: ingot-credential-api
    ├── 管理自己的密码历史表
    └── RPC 调用 Credential Security Service
```

## 设计模式

### 1. 策略模式 (Strategy Pattern)

`PasswordPolicy` 接口定义了密码策略的统一行为，不同的具体策略实现不同的校验规则。

```java
// 策略接口
public interface PasswordPolicy {
    String getName();
    int getPriority();
    PolicyCheckResult check(PolicyCheckContext context);
    boolean isEnabled(Tenant tenant);
}

// 具体策略 A - 强度策略
public class PasswordStrengthPolicy implements PasswordPolicy {
    @Override
    public PolicyCheckResult check(PolicyCheckContext context) {
        // 检查密码长度、复杂度等
        if (context.getPassword().length() < minLength) {
            return PolicyCheckResult.fail("密码长度不足");
        }
        return PolicyCheckResult.pass();
    }
}

// 具体策略 B - 历史策略
public class PasswordHistoryPolicy implements PasswordPolicy {
    @Override
    public PolicyCheckResult check(PolicyCheckContext context) {
        // 检查是否与历史密码重复
        if (isReused(context.getPassword())) {
            return PolicyCheckResult.fail("密码已使用过");
        }
        return PolicyCheckResult.pass();
    }
}

// 使用策略
@Service
public class PasswordValidator {
    private final List<PasswordPolicy> policies;
    
    public PasswordCheckResult validate(PolicyCheckContext context) {
        for (PasswordPolicy policy : policies) {
            if (policy.isEnabled(context.getTenant())) {
                PolicyCheckResult result = policy.check(context);
                if (!result.isPassed()) {
                    return result;
                }
            }
        }
        return PasswordCheckResult.pass();
    }
}
```

### 2. 责任链模式 (Chain of Responsibility)

密码校验通过责任链传递，每个策略依次校验：

```java
public class ValidatorChain {
    private List<PasswordPolicy> chain;
    
    public PasswordCheckResult validate(PolicyCheckContext context) {
        PasswordCheckResult result = new PasswordCheckResult();
        
        // 按优先级排序
        chain.sort(Comparator.comparingInt(PasswordPolicy::getPriority));
        
        // 依次执行每个策略
        for (PasswordPolicy policy : chain) {
            PolicyCheckResult policyResult = policy.check(context);
            
            if (!policyResult.isPassed()) {
                result.addFailure(policyResult);
                if (policy.isBlocking()) {
                    break;  // 阻断式策略失败后停止
                }
            } else if (policyResult.hasWarnings()) {
                result.addWarnings(policyResult.getWarnings());
            }
        }
        
        return result;
    }
}
```

### 3. 联邦式数据架构

策略集中管理，数据分散存储：

```java
// Credential Security Service - 集中管理策略
@Service
public class PolicyConfigService {
    @Cacheable(value = "credential:policy", key = "#tenantId + ':' + #policyType")
    public PolicyConfig getPolicy(Long tenantId, String policyType) {
        // 1. 查询租户级策略
        PolicyConfig tenantPolicy = repository.findByTenantAndType(tenantId, policyType);
        if (tenantPolicy != null) {
            return tenantPolicy;
        }
        
        // 2. 回退到全局默认策略
        return repository.findDefaultPolicy(policyType);
    }
}

// Member Service - 管理自己的密码历史
@Service
public class MemberPasswordHistoryService {
    
    @Transactional
    public void addHistory(Long userId, String passwordHash) {
        // 1. 插入新记录到本地数据库
        MemberPasswordHistory history = new MemberPasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(passwordHash);
        historyMapper.insert(history);
        
        // 2. 环形缓冲：删除超过限制的旧记录
        long count = historyMapper.countByUserId(userId);
        if (count > MAX_HISTORY_COUNT) {
            historyMapper.deleteOldest(userId, count - MAX_HISTORY_COUNT);
        }
    }
    
    public boolean isPasswordReused(Long userId, String rawPassword) {
        List<MemberPasswordHistory> histories = 
            historyMapper.selectByUserId(userId, MAX_HISTORY_COUNT);
        
        return histories.stream()
            .anyMatch(h -> passwordEncoder.matches(rawPassword, h.getPasswordHash()));
    }
}

// PMS Service - 同样管理自己的密码历史（结构相同，数据独立）
@Service
public class SysPasswordHistoryService {
    // 与 Member 类似的实现，但使用 sys_password_history 表
}
```

## 数据模型设计

### 集中式：策略配置表

```sql
-- Credential Security Service 数据库
CREATE TABLE credential_policy_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT COMMENT '租户ID，NULL表示全局默认',
    policy_type VARCHAR(50) NOT NULL COMMENT '策略类型: STRENGTH, EXPIRATION, HISTORY',
    policy_config JSON NOT NULL COMMENT '策略配置JSON',
    priority INT DEFAULT 0 COMMENT '优先级，数字越小优先级越高',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_type (tenant_id, policy_type),
    INDEX idx_type_enabled (policy_type, enabled)
) COMMENT '凭证策略配置表';

-- 示例数据：全局默认强度策略
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled) VALUES
(NULL, 'STRENGTH', '{
  "minLength": 8,
  "maxLength": 32,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": true,
  "forbiddenPatterns": ["password", "123456", "admin"]
}', 10, true);

-- 示例数据：全局默认过期策略
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled) VALUES
(NULL, 'EXPIRATION', '{
  "enabled": true,
  "maxDays": 90,
  "graceLoginCount": 3,
  "warningDaysBefore": 7
}', 20, true);
```

### 联邦式：密码历史表（PMS）

```sql
-- PMS Service 数据库
CREATE TABLE pms_password_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_created (user_id, created_at DESC)
) COMMENT 'PMS用户密码历史记录（环形缓冲）';

CREATE TABLE pms_password_expiration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    password_set_at TIMESTAMP NOT NULL COMMENT '密码设置时间',
    expire_at TIMESTAMP COMMENT '过期时间',
    last_warned_at TIMESTAMP COMMENT '最后警告时间',
    grace_login_count INT DEFAULT 0 COMMENT '宽限期登录次数',
    force_change BOOLEAN DEFAULT FALSE COMMENT '是否强制修改',
    UNIQUE KEY uk_user (user_id),
    INDEX idx_expire (expire_at)
) COMMENT 'PMS用户密码过期信息';
```

### 联邦式：密码历史表（Member）

```sql
-- Member Service 数据库
CREATE TABLE member_password_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_created (user_id, created_at DESC)
) COMMENT 'Member用户密码历史记录（环形缓冲）';

CREATE TABLE member_password_expiration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    password_set_at TIMESTAMP NOT NULL COMMENT '密码设置时间',
    expire_at TIMESTAMP COMMENT '过期时间',
    last_warned_at TIMESTAMP COMMENT '最后警告时间',
    grace_login_count INT DEFAULT 0 COMMENT '宽限期登录次数',
    force_change BOOLEAN DEFAULT FALSE COMMENT '是否强制修改',
    UNIQUE KEY uk_user (user_id),
    INDEX idx_expire (expire_at)
) COMMENT 'Member用户密码过期信息';
```

### 集中式：审计日志表

```sql
-- Credential Security Service 数据库
CREATE TABLE credential_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_type VARCHAR(20) NOT NULL COMMENT '用户类型: PMS, MEMBER',
    action VARCHAR(50) NOT NULL COMMENT '操作: PASSWORD_CHANGE, PASSWORD_RESET, PASSWORD_VALIDATE',
    operator_id BIGINT COMMENT '操作人ID（管理员重置时）',
    operator_type VARCHAR(20) COMMENT '操作人类型',
    result VARCHAR(20) NOT NULL COMMENT '结果: SUCCESS, FAILURE',
    failure_reason VARCHAR(500) COMMENT '失败原因',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT 'User Agent',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, user_type, created_at DESC),
    INDEX idx_action_time (action, created_at DESC),
    INDEX idx_result_time (result, created_at DESC)
) COMMENT '凭证操作审计日志';
```

## 扩展新的策略

要支持新的密码策略（例如密码复杂度评分、基于机器学习的弱密码检测），只需：

### 步骤1：定义新策略类

```java
package com.ingot.framework.credential.policy;

public class PasswordComplexityScorePolicy implements PasswordPolicy {
    
    private final int minScore;
    
    @Override
    public String getName() {
        return "PASSWORD_COMPLEXITY_SCORE";
    }
    
    @Override
    public int getPriority() {
        return 15;  // 在强度策略之后执行
    }
    
    @Override
    public PolicyCheckResult check(PolicyCheckContext context) {
        int score = calculateComplexityScore(context.getPassword());
        
        if (score < minScore) {
            return PolicyCheckResult.fail(
                String.format("密码复杂度不足，当前评分: %d，要求: %d", score, minScore)
            );
        }
        
        return PolicyCheckResult.pass();
    }
    
    @Override
    public boolean isEnabled(Tenant tenant) {
        // 从配置中读取是否启用
        return policyConfigService.isEnabled(tenant.getId(), getName());
    }
    
    private int calculateComplexityScore(String password) {
        int score = 0;
        // 长度得分
        score += Math.min(password.length() * 2, 20);
        // 字符类型得分
        if (hasUpperCase(password)) score += 10;
        if (hasLowerCase(password)) score += 10;
        if (hasDigit(password)) score += 10;
        if (hasSpecialChar(password)) score += 15;
        // 熵值得分
        score += calculateEntropy(password) / 2;
        return score;
    }
}
```

### 步骤2：注册策略

```java
@Configuration
public class CredentialPolicyConfiguration {
    
    @Bean
    public PasswordPolicy passwordComplexityScorePolicy() {
        return new PasswordComplexityScorePolicy(60); // 最低评分60
    }
    
    @Bean
    public PasswordValidator passwordValidator(List<PasswordPolicy> policies) {
        return new PasswordValidator(policies);
    }
}
```

### 步骤3：添加策略配置

```sql
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled) VALUES
(NULL, 'COMPLEXITY_SCORE', '{
  "minScore": 60,
  "scoreWeights": {
    "length": 2,
    "uppercase": 10,
    "lowercase": 10,
    "digit": 10,
    "specialChar": 15,
    "entropy": 0.5
  }
}', 15, true);
```

**完成！** 新策略会自动加入校验链，无需修改现有代码。

## 性能考虑

### 1. 策略配置缓存

```java
@Service
public class PolicyConfigService {
    
    @Cacheable(
        value = "credential:policy", 
        key = "#tenantId + ':' + #policyType",
        unless = "#result == null"
    )
    public PolicyConfig getPolicy(Long tenantId, String policyType) {
        // 数据库查询（带缓存）
        return repository.findPolicy(tenantId, policyType);
    }
    
    @CacheEvict(value = "credential:policy", allEntries = true)
    public void updatePolicy(PolicyConfig policy) {
        repository.save(policy);
    }
}
```

**缓存策略：**
- 使用 Redis 存储策略配置
- TTL: 1小时
- 策略更新时清除所有缓存

### 2. 密码历史查询优化

```java
// 使用索引优化查询
CREATE INDEX idx_user_created ON pms_password_history(user_id, created_at DESC);

// 只查询最近N条记录
SELECT password_hash 
FROM pms_password_history 
WHERE user_id = ? 
ORDER BY created_at DESC 
LIMIT 5;

// 环形缓冲：自动清理旧记录
DELETE FROM pms_password_history 
WHERE user_id = ? 
AND created_at < (
    SELECT created_at 
    FROM pms_password_history 
    WHERE user_id = ? 
    ORDER BY created_at DESC 
    LIMIT 5, 1
);
```

### 3. 批量校验优化

```java
@Service
public class BatchPasswordValidator {
    
    public List<PasswordCheckResult> validateBatch(List<PolicyCheckContext> contexts) {
        // 1. 批量加载策略配置（避免N+1查询）
        Set<Long> tenantIds = contexts.stream()
            .map(PolicyCheckContext::getTenantId)
            .collect(Collectors.toSet());
        Map<Long, PolicyConfig> policyCache = 
            policyService.batchLoadPolicies(tenantIds);
        
        // 2. 并行校验
        return contexts.parallelStream()
            .map(context -> {
                context.setPolicyConfig(policyCache.get(context.getTenantId()));
                return validator.validate(context);
            })
            .collect(Collectors.toList());
    }
}
```

### 4. 异步审计日志

```java
@Service
public class AuditService {
    
    @Async("auditExecutor")
    public CompletableFuture<Void> recordAudit(CredentialAuditDTO audit) {
        try {
            auditMapper.insert(audit);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("审计日志记录失败", e);
            // 审计失败不影响主流程
            return CompletableFuture.completedFuture(null);
        }
    }
}

// 配置异步线程池
@Configuration
public class AsyncConfiguration {
    
    @Bean("auditExecutor")
    public ThreadPoolTaskExecutor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
```

## 安全考虑

### 1. 访问控制

```java
// 策略配置修改需要管理员权限
@RestController
@RequestMapping("/v1/credential/policy")
public class PolicyAPI {
    
    @PutMapping("/{policyType}")
    @AdminOrHasAnyAuthority({"platform:credential:policy:update"})
    public R<Void> updatePolicy(
        @PathVariable String policyType,
        @RequestBody PolicyConfigDTO config
    ) {
        policyService.updatePolicy(config);
        return R.ok();
    }
}
```

### 2. 审计日志保护

```java
// 审计日志只能查询，不能修改或删除
@RestController
@RequestMapping("/v1/credential/audit")
public class AuditAPI {
    
    @GetMapping("/user/{userId}")
    @AdminOrHasAnyAuthority({"platform:audit:query"})
    public R<Page<AuditLogVO>> queryUserLogs(@PathVariable Long userId) {
        // 只读操作
        return R.ok(auditService.queryUserLogs(userId));
    }
    
    // 不提供删除接口，通过数据库归档策略管理
}

// 定期归档历史审计日志
@Scheduled(cron = "0 0 2 1 * ?")
public void archiveOldAuditLogs() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(6);
    auditService.archiveLogsBefore(cutoffDate);
}
```

### 3. 密码哈希保护

```java
// 历史密码存储时使用与用户表相同的加密方式
@Service
public class PasswordHistoryService {
    
    public void addHistory(Long userId, String rawPassword) {
        // 使用 BCrypt 加密存储
        String passwordHash = passwordEncoder.encode(rawPassword);
        
        PasswordHistory history = new PasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(passwordHash);
        historyMapper.insert(history);
    }
    
    public boolean isPasswordReused(Long userId, String rawPassword) {
        List<PasswordHistory> histories = historyMapper.selectByUserId(userId, 5);
        
        // 使用安全的密码比对方法
        return histories.stream()
            .anyMatch(h -> passwordEncoder.matches(rawPassword, h.getPasswordHash()));
    }
}
```

### 4. 防止暴力破解

```java
// 在 Auth Service 中限制密码校验频率
@Service
public class RateLimitService {
    
    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;
    
    public boolean checkRateLimit(String username) {
        String key = "pwd:validate:" + username;
        Integer attempts = redisTemplate.opsForValue().get(key);
        
        if (attempts != null && attempts >= 5) {
            return false; // 超过限制
        }
        
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 15, TimeUnit.MINUTES);
        return true;
    }
}
```

## 总结

这个架构设计实现了：

1. ✅ **职责分离** - Framework 专注策略校验，Service 管理数据和流程
2. ✅ **高内聚低耦合** - 各服务独立，通过标准接口协作
3. ✅ **数据自治** - 联邦式架构，服务管理自己的历史数据
4. ✅ **易于扩展** - 支持添加新策略、新凭证类型
5. ✅ **性能优化** - 缓存、异步、批量处理
6. ✅ **安全可靠** - 权限控制、审计、加密存储
7. ✅ **降级友好** - Service 不可用时本地降级

通过这种设计，Ingot Cloud 拥有了一个**统一、灵活、可演进**的凭证安全管理体系，为未来支持 MFA、Passkey 等高级特性奠定了坚实的基础。
