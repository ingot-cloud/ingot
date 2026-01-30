# 凭证安全策略配置指南

## 策略体系概述

Ingot Cloud 凭证安全模块提供了灵活的策略配置体系，支持三大核心策略：

| 策略类型 | 说明 | 作用阶段 | 优先级 |
|---------|------|---------|-------|
| **密码强度策略** | 控制密码的复杂度要求 | 设置/修改密码时 | 10 |
| **密码过期策略** | 管理密码的生命周期 | 登录时检查 | 20 |
| **密码历史策略** | 防止密码重复使用 | 修改密码时 | 30 |

### 策略配置方式

支持两种配置方式：

1. **数据库配置**（推荐）- 支持动态更新、租户级覆盖
2. **YAML 配置** - 用于开发环境快速测试

### 策略生效优先级

```
租户级策略 > 全局默认策略 > 系统内置策略
```

## 密码强度策略

### 策略说明

密码强度策略确保用户设置的密码满足基本的安全要求，防止使用弱密码。

### 配置参数

| 参数 | 类型 | 默认值 | 说明 |
|-----|------|-------|------|
| `minLength` | int | 8 | 最小长度 |
| `maxLength` | int | 32 | 最大长度 |
| `requireUppercase` | boolean | true | 是否要求大写字母 |
| `requireLowercase` | boolean | true | 是否要求小写字母 |
| `requireDigit` | boolean | true | 是否要求数字 |
| `requireSpecialChar` | boolean | true | 是否要求特殊字符 |
| `specialChars` | string | `!@#$%^&*` | 允许的特殊字符集 |
| `forbiddenPatterns` | array | `[]` | 禁止的模式列表 |
| `forbidUserAttributes` | boolean | true | 禁止包含用户属性（用户名、手机号等） |

### 数据库配置示例

#### 全局默认策略

```sql
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled) VALUES
(NULL, 'STRENGTH', '{
  "minLength": 8,
  "maxLength": 32,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": true,
  "specialChars": "!@#$%^&*()_+-=[]{}|;:,.<>?",
  "forbiddenPatterns": [
    "password",
    "123456",
    "admin",
    "qwerty",
    "abc123"
  ],
  "forbidUserAttributes": true
}', 10, true);
```

#### 租户级策略（更严格）

```sql
-- 租户ID为1的企业，要求更高的密码强度
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled) VALUES
(1, 'STRENGTH', '{
  "minLength": 12,
  "maxLength": 64,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": true,
  "specialChars": "!@#$%^&*()_+-=[]{}|;:,.<>?",
  "forbiddenPatterns": [
    "password",
    "123456",
    "admin",
    "qwerty",
    "abc123",
    "company",
    "welcome"
  ],
  "forbidUserAttributes": true,
  "minEntropyBits": 40
}', 10, true);
```

### YAML 配置示例

```yaml
ingot:
  credential:
    policy:
      strength:
        enabled: true
        min-length: 8
        max-length: 32
        require-uppercase: true
        require-lowercase: true
        require-digit: true
        require-special-char: true
        special-chars: "!@#$%^&*()_+-=[]{}|;:,.<>?"
        forbidden-patterns:
          - password
          - 123456
          - admin
          - qwerty
        forbid-user-attributes: true
```

### 使用场景

#### 场景 1：标准企业应用

**需求：** 中等强度密码，平衡安全性和用户体验

**配置：**
```json
{
  "minLength": 8,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": false,
  "forbiddenPatterns": ["password", "123456"],
  "forbidUserAttributes": true
}
```

#### 场景 2：高安全要求（金融、医疗）

**需求：** 高强度密码，严格的安全要求

**配置：**
```json
{
  "minLength": 12,
  "maxLength": 64,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": true,
  "specialChars": "!@#$%^&*()_+-=[]{}|;:,.<>?",
  "forbiddenPatterns": [
    "password", "123456", "admin", "qwerty",
    "welcome", "company", "letmein"
  ],
  "forbidUserAttributes": true,
  "minEntropyBits": 50
}
```

#### 场景 3：C端用户应用

**需求：** 较低强度，降低注册门槛

**配置：**
```json
{
  "minLength": 6,
  "maxLength": 20,
  "requireUppercase": false,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": false,
  "forbiddenPatterns": ["123456", "password"],
  "forbidUserAttributes": false
}
```

### 校验规则说明

```java
// 1. 长度检查
if (password.length() < minLength || password.length() > maxLength) {
    return fail("密码长度必须在 " + minLength + " 到 " + maxLength + " 之间");
}

// 2. 字符类型检查
if (requireUppercase && !containsUppercase(password)) {
    return fail("密码必须包含至少一个大写字母");
}

// 3. 禁止模式检查
for (String pattern : forbiddenPatterns) {
    if (password.toLowerCase().contains(pattern.toLowerCase())) {
        return fail("密码包含禁止使用的常见模式");
    }
}

// 4. 禁止用户属性
if (forbidUserAttributes) {
    if (password.contains(username) || password.contains(phone)) {
        return fail("密码不能包含用户名或手机号");
    }
}
```

## 密码过期策略

### 策略说明

密码过期策略通过强制用户定期更换密码，降低凭证长期泄露的风险。

### 配置参数

| 参数 | 类型 | 默认值 | 说明 |
|-----|------|-------|------|
| `enabled` | boolean | true | 是否启用过期策略 |
| `maxDays` | int | 90 | 密码最大有效天数 |
| `warningDaysBefore` | int | 7 | 提前多少天开始警告 |
| `graceLoginCount` | int | 3 | 过期后允许的宽限登录次数 |
| `forceChangeAfterReset` | boolean | true | 管理员重置后是否强制修改 |

### 数据库配置示例

#### 全局默认策略（90天过期）

```sql
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled) VALUES
(NULL, 'EXPIRATION', '{
  "enabled": true,
  "maxDays": 90,
  "warningDaysBefore": 7,
  "graceLoginCount": 3,
  "forceChangeAfterReset": true
}', 20, true);
```

#### 租户级策略（更短的有效期）

```sql
-- 金融行业租户，要求60天更换密码
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled) VALUES
(2, 'EXPIRATION', '{
  "enabled": true,
  "maxDays": 60,
  "warningDaysBefore": 14,
  "graceLoginCount": 1,
  "forceChangeAfterReset": true,
  "notifyBeforeExpire": true
}', 20, true);
```

#### 禁用过期策略（开发环境）

```sql
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled) VALUES
(NULL, 'EXPIRATION', '{
  "enabled": false
}', 20, true);
```

### YAML 配置示例

```yaml
ingot:
  credential:
    policy:
      expiration:
        enabled: true
        max-days: 90
        warning-days-before: 7
        grace-login-count: 3
        force-change-after-reset: true
```

### 使用场景

#### 场景 1：标准企业（90天）

**需求：** 符合一般企业安全规范

**配置：**
```json
{
  "enabled": true,
  "maxDays": 90,
  "warningDaysBefore": 7,
  "graceLoginCount": 3,
  "forceChangeAfterReset": true
}
```

**用户体验：**
- 密码83天后开始提示"您的密码将在7天后过期"
- 过期后允许登录3次，每次登录都强制跳转修改密码
- 第4次登录直接拒绝

#### 场景 2：高安全场景（60天）

**需求：** 金融、医疗等高安全行业

**配置：**
```json
{
  "enabled": true,
  "maxDays": 60,
  "warningDaysBefore": 14,
  "graceLoginCount": 1,
  "forceChangeAfterReset": true
}
```

**用户体验：**
- 密码46天后开始提示
- 过期后只允许登录1次
- 立即强制修改

#### 场景 3：C端应用（不过期）

**需求：** 用户体验优先

**配置：**
```json
{
  "enabled": false
}
```

### 宽限期工作流程

```
密码过期
    ↓
用户尝试登录
    ↓
检查宽限登录次数
    ↓
如果 graceLoginCount > 0
    ├─ 允许登录
    ├─ graceLoginCount--
    ├─ 返回强制修改标记
    └─ 前端跳转到修改密码页面
    
如果 graceLoginCount <= 0
    └─ 拒绝登录，提示必须重置密码
```

### 管理员操作说明

```java
// 管理员重置用户密码
@PutMapping("/{userId}/reset-password")
public R<?> resetPassword(@PathVariable Long userId) {
    // 1. 生成随机密码
    String tempPassword = RandomUtil.randomString(8);
    
    // 2. 更新密码
    userService.updatePassword(userId, tempPassword);
    
    // 3. 标记强制修改
    passwordExpirationService.setForceChange(userId, true);
    
    // 4. 返回临时密码
    return R.ok(new ResetPwdVO(tempPassword));
}

// 用户首次登录使用临时密码
@PostMapping("/login")
public R<?> login(@RequestBody LoginDTO dto) {
    // 检查是否需要强制修改
    if (passwordExpirationService.needForceChange(userId)) {
        return R.fail("FORCE_CHANGE_PASSWORD", "必须修改初始密码后才能使用");
    }
    
    return R.ok(token);
}
```

## 密码历史策略

### 策略说明

密码历史策略防止用户反复使用相同的密码，提高密码更换的有效性。

### 配置参数

| 参数 | 类型 | 默认值 | 说明 |
|-----|------|-------|------|
| `enabled` | boolean | true | 是否启用历史策略 |
| `keepRecentCount` | int | 5 | 保留最近N次密码记录 |
| `checkCount` | int | 5 | 检查最近N次是否重复 |

### 数据库配置示例

#### 全局默认策略（记录最近5次）

```sql
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled) VALUES
(NULL, 'HISTORY', '{
  "enabled": true,
  "keepRecentCount": 5,
  "checkCount": 5
}', 30, true);
```

#### 高安全要求（记录最近10次）

```sql
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled) VALUES
(3, 'HISTORY', '{
  "enabled": true,
  "keepRecentCount": 10,
  "checkCount": 10
}', 30, true);
```

### YAML 配置示例

```yaml
ingot:
  credential:
    policy:
      history:
        enabled: true
        keep-recent-count: 5
        check-count: 5
```

### 环形缓冲实现

密码历史使用环形缓冲策略，自动维护固定数量的历史记录：

```
初始状态: []

第1次设置密码: [pwd1]
第2次修改密码: [pwd1, pwd2]
第3次修改密码: [pwd1, pwd2, pwd3]
第4次修改密码: [pwd1, pwd2, pwd3, pwd4]
第5次修改密码: [pwd1, pwd2, pwd3, pwd4, pwd5]

第6次修改密码: [pwd2, pwd3, pwd4, pwd5, pwd6]  ← 自动删除最旧的 pwd1
第7次修改密码: [pwd3, pwd4, pwd5, pwd6, pwd7]  ← 自动删除最旧的 pwd2
```

### 实现代码

```java
@Service
public class PasswordHistoryService {
    
    private static final int MAX_HISTORY_COUNT = 5;
    
    /**
     * 添加密码历史（自动维护环形缓冲）
     */
    @Transactional
    public void addHistory(Long userId, String passwordHash) {
        // 1. 插入新记录
        PasswordHistory history = new PasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(passwordHash);
        history.setCreatedAt(LocalDateTime.now());
        passwordHistoryMapper.insert(history);
        
        // 2. 查询该用户的历史记录数
        long count = passwordHistoryMapper.countByUserId(userId);
        
        // 3. 如果超过限制，删除最旧的记录
        if (count > MAX_HISTORY_COUNT) {
            long deleteCount = count - MAX_HISTORY_COUNT;
            passwordHistoryMapper.deleteOldest(userId, deleteCount);
        }
    }
    
    /**
     * 检查密码是否在历史中使用过
     */
    public boolean isPasswordReused(Long userId, String rawPassword) {
        List<PasswordHistory> histories = passwordHistoryMapper
            .selectByUserId(userId, MAX_HISTORY_COUNT);
        
        return histories.stream()
            .anyMatch(h -> passwordEncoder.matches(rawPassword, h.getPasswordHash()));
    }
}
```

### 存储优化

**优势：**
- ✅ 自动清理，无需定期维护
- ✅ 存储空间固定，不会无限增长
- ✅ 查询效率高，只需扫描固定条数

**SQL 优化：**
```sql
-- 高效的删除最旧记录（使用子查询）
DELETE FROM pms_password_history 
WHERE user_id = ? 
AND id NOT IN (
    SELECT id FROM (
        SELECT id FROM pms_password_history 
        WHERE user_id = ? 
        ORDER BY created_at DESC 
        LIMIT 5
    ) tmp
);

-- 或者使用更直接的方式（如果支持）
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

## 租户级策略配置

### 配置优先级

```
1. 租户级策略 (tenant_id = 具体值)
   ↓
2. 全局默认策略 (tenant_id = NULL)
   ↓
3. 系统内置策略 (硬编码)
```

### 配置示例

#### 全局默认（标准强度）

```sql
-- 适用于所有未配置策略的租户
INSERT INTO credential_policy_config VALUES
(NULL, 'STRENGTH', '{"minLength": 8, "requireUppercase": true, ...}', 10, true);
```

#### 租户1（金融，高安全）

```sql
-- 覆盖全局配置
INSERT INTO credential_policy_config VALUES
(1, 'STRENGTH', '{"minLength": 12, "requireSpecialChar": true, ...}', 10, true),
(1, 'EXPIRATION', '{"maxDays": 60, ...}', 20, true),
(1, 'HISTORY', '{"keepRecentCount": 10, ...}', 30, true);
```

#### 租户2（C端应用，宽松）

```sql
-- 覆盖全局配置
INSERT INTO credential_policy_config VALUES
(2, 'STRENGTH', '{"minLength": 6, "requireSpecialChar": false, ...}', 10, true),
(2, 'EXPIRATION', '{"enabled": false, ...}', 20, true),
(2, 'HISTORY', '{"keepRecentCount": 3, ...}', 30, true);
```

### 查询逻辑

```java
@Service
public class PolicyConfigService {
    
    @Cacheable(value = "credential:policy", key = "#tenantId + ':' + #policyType")
    public PolicyConfig getPolicy(Long tenantId, String policyType) {
        // 1. 优先查询租户级策略
        PolicyConfig tenantPolicy = policyRepository
            .findByTenantIdAndType(tenantId, policyType);
        
        if (tenantPolicy != null && tenantPolicy.isEnabled()) {
            return tenantPolicy;
        }
        
        // 2. 回退到全局默认策略
        PolicyConfig defaultPolicy = policyRepository
            .findByTenantIdAndType(null, policyType);
        
        if (defaultPolicy != null && defaultPolicy.isEnabled()) {
            return defaultPolicy;
        }
        
        // 3. 使用系统内置策略
        return getBuiltInPolicy(policyType);
    }
}
```

## 动态更新策略

### 通过 REST API 更新

```java
@RestController
@RequestMapping("/v1/credential/policy")
public class PolicyAPI {
    
    /**
     * 更新策略配置
     */
    @PutMapping("/{policyType}")
    @AdminOrHasAnyAuthority({"platform:credential:policy:update"})
    public R<Void> updatePolicy(
        @PathVariable String policyType,
        @RequestBody PolicyConfigDTO config
    ) {
        Long tenantId = SecurityContext.getTenantId();
        policyService.updatePolicy(tenantId, policyType, config);
        return R.ok();
    }
}
```

### 请求示例

```bash
curl -X PUT http://localhost:9090/v1/credential/policy/STRENGTH \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "minLength": 10,
    "requireUppercase": true,
    "requireLowercase": true,
    "requireDigit": true,
    "requireSpecialChar": true,
    "forbiddenPatterns": ["password", "123456"]
  }'
```

### 缓存失效

```java
@Service
public class PolicyConfigService {
    
    @CacheEvict(value = "credential:policy", allEntries = true)
    public void updatePolicy(Long tenantId, String policyType, PolicyConfig config) {
        // 1. 更新数据库
        policyRepository.save(config);
        
        // 2. 发布事件通知其他节点
        eventPublisher.publishEvent(new PolicyUpdatedEvent(tenantId, policyType));
        
        // 3. 缓存自动失效（@CacheEvict）
    }
}

// 监听策略更新事件（集群环境）
@Component
public class PolicyUpdateListener {
    
    @EventListener
    public void onPolicyUpdated(PolicyUpdatedEvent event) {
        log.info("策略已更新: tenantId={}, policyType={}", 
            event.getTenantId(), event.getPolicyType());
        
        // 通知前端刷新配置
        webSocketService.notifyPolicyUpdate(event);
    }
}
```

## 配置示例速查

### 标准企业配置

```sql
-- 适合大多数企业应用
INSERT INTO credential_policy_config VALUES
(NULL, 'STRENGTH', '{
  "minLength": 8,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": false
}', 10, true),
(NULL, 'EXPIRATION', '{
  "enabled": true,
  "maxDays": 90,
  "warningDaysBefore": 7,
  "graceLoginCount": 3
}', 20, true),
(NULL, 'HISTORY', '{
  "enabled": true,
  "keepRecentCount": 5
}', 30, true);
```

### 高安全配置

```sql
-- 金融、医疗等高安全行业
INSERT INTO credential_policy_config VALUES
(NULL, 'STRENGTH', '{
  "minLength": 12,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": true,
  "minEntropyBits": 50
}', 10, true),
(NULL, 'EXPIRATION', '{
  "enabled": true,
  "maxDays": 60,
  "warningDaysBefore": 14,
  "graceLoginCount": 1
}', 20, true),
(NULL, 'HISTORY', '{
  "enabled": true,
  "keepRecentCount": 10
}', 30, true);
```

### C端应用配置

```sql
-- 注重用户体验的C端应用
INSERT INTO credential_policy_config VALUES
(NULL, 'STRENGTH', '{
  "minLength": 6,
  "requireUppercase": false,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": false
}', 10, true),
(NULL, 'EXPIRATION', '{
  "enabled": false
}', 20, true),
(NULL, 'HISTORY', '{
  "enabled": true,
  "keepRecentCount": 3
}', 30, true);
```

### 开发环境配置

```sql
-- 开发和测试环境，降低限制
INSERT INTO credential_policy_config VALUES
(NULL, 'STRENGTH', '{
  "minLength": 6,
  "requireUppercase": false,
  "requireLowercase": false,
  "requireDigit": false,
  "requireSpecialChar": false
}', 10, true),
(NULL, 'EXPIRATION', '{
  "enabled": false
}', 20, true),
(NULL, 'HISTORY', '{
  "enabled": false
}', 30, true);
```

## 最佳实践

### 1. 策略配置建议

| 场景 | 密码长度 | 复杂度要求 | 过期天数 | 历史记录 |
|-----|---------|-----------|---------|---------|
| 企业内部系统 | 8-16 | 中等 | 90 | 5 |
| 金融/医疗 | 12-32 | 高 | 60 | 10 |
| C端应用 | 6-20 | 低 | 不过期 | 3 |
| 管理后台 | 10-32 | 高 | 60 | 5 |

### 2. 分环境配置

```yaml
# application-dev.yml - 开发环境
ingot:
  credential:
    policy:
      strength:
        min-length: 6
        require-special-char: false
      expiration:
        enabled: false

# application-prod.yml - 生产环境
ingot:
  credential:
    policy:
      strength:
        min-length: 8
        require-special-char: true
      expiration:
        enabled: true
        max-days: 90
```

### 3. 策略迁移步骤

如果需要调整现有策略：

```sql
-- 1. 先更新配置（不启用）
UPDATE credential_policy_config 
SET policy_config = '{"minLength": 10, ...}', 
    enabled = false
WHERE policy_type = 'STRENGTH';

-- 2. 通知用户即将调整策略（提前7天）

-- 3. 启用新策略
UPDATE credential_policy_config 
SET enabled = true
WHERE policy_type = 'STRENGTH';

-- 4. 设置过渡期（可选）
-- 允许现有密码继续使用，仅对新设置的密码生效
```

### 4. 监控告警

```java
// 监控密码过期情况
@Scheduled(cron = "0 0 1 * * ?")
public void checkPasswordExpiration() {
    // 统计即将过期的密码数量
    long expiringSoon = passwordExpirationService
        .countExpiringWithinDays(7);
    
    if (expiringSoon > 100) {
        alertService.sendAlert(
            "大量密码即将过期",
            String.format("有 %d 个用户的密码将在7天内过期", expiringSoon)
        );
    }
}

// 监控弱密码使用情况
@Scheduled(cron = "0 0 2 * * ?")
public void checkWeakPasswords() {
    // 扫描并标记弱密码用户
    List<User> weakPasswordUsers = userService.findWeakPasswordUsers();
    
    if (weakPasswordUsers.size() > 0) {
        // 发送邮件提醒用户修改密码
        notificationService.sendWeakPasswordWarning(weakPasswordUsers);
    }
}
```

### 5. 用户体验优化

```java
// 前端实时校验
@PostMapping("/validate")
public R<PasswordCheckResult> validatePassword(@RequestBody ValidatePasswordDTO dto) {
    // 提供实时反馈，无需等到提交
    PasswordCheckResult result = passwordValidator.validate(dto.getPassword());
    return R.ok(result);
}

// 密码强度指示器
{
  "score": 75,        // 0-100
  "level": "MEDIUM",  // WEAK, MEDIUM, STRONG, VERY_STRONG
  "suggestions": [
    "添加特殊字符可以提高强度",
    "长度达到12位会更安全"
  ]
}
```

## 总结

通过灵活的策略配置体系，Ingot Cloud 凭证安全模块可以适应不同场景的安全需求：

- ✅ **三大核心策略** - 强度、过期、历史，全面覆盖密码安全
- ✅ **多租户支持** - 每个租户可独立配置策略
- ✅ **动态更新** - 无需重启，实时生效
- ✅ **环形缓冲** - 自动维护历史记录，无需人工清理
- ✅ **降级友好** - 支持 YAML 配置，便于开发测试

根据实际需求选择合适的策略配置，在安全性和用户体验之间找到最佳平衡点。
