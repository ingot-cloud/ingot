# 凭证安全常见问题

## 设计相关

### Q1: 为什么采用 Framework + Service 架构？

**A:** 这种架构设计有以下优势：

1. **解耦合**
   - Framework 无状态，可以本地集成到 Auth Service，避免登录链路对外部服务的依赖
   - Service 管理策略和数据，可以独立部署和扩展

2. **降级友好**
   - Auth Service 只依赖 Framework，即使 Credential Service 不可用也不影响登录
   - 策略可以配置本地降级规则

3. **职责清晰**
   - Framework：策略校验引擎
   - Service：数据管理、策略配置、审计日志

4. **易于扩展**
   - 新策略只需实现 `PasswordPolicy` 接口
   - 支持自定义策略插件

---

### Q2: 为什么选择联邦式数据架构？

**A:** 联邦式架构符合 DDD 领域边界和微服务自治原则：

**优势：**
- ✅ 各服务数据完全自治（Member 管理自己的历史，PMS 管理自己的历史）
- ✅ 单一服务故障不影响其他服务
- ✅ 符合 GDPR 数据隔离要求
- ✅ 支持独立扩展和备份

**劣势：**
- ⚠️ 数据分散，需要聚合查询时较复杂
- ⚠️ 策略一致性需要通过 Credential Service 保证

**为什么不用集中式？**
- 如果密码历史都存在 Credential Service，会产生强依赖
- 服务调用链变长：Member → Credential Service → 查询历史
- 单点故障风险增加

---

### Q3: 与 Spring Security 的关系？

**A:** 凭证安全模块是 Spring Security 的补充，而不是替代：

| 功能 | Spring Security | 凭证安全模块 |
|-----|-----------------|-------------|
| 密码加密 | ✅ PasswordEncoder | - |
| 密码校验 | ✅ 基础校验 | ✅ 策略化校验 |
| 密码强度 | ❌ | ✅ |
| 密码过期 | ❌ | ✅ |
| 密码历史 | ❌ | ✅ |
| 审计日志 | ❌ | ✅ |

**集成方式：**
```java
@Override
protected void additionalAuthenticationChecks(UserDetails user, 
    UsernamePasswordAuthenticationToken authentication) {
    
    // 1. Spring Security 的密码校验
    if (!passwordEncoder.matches(presentedPassword, user.getPassword())) {
        throw new BadCredentialsException("密码不正确");
    }
    
    // 2. 凭证安全模块的额外检查
    PasswordCheckResult result = credentialChecker.check(context);
    if (result.isExpired()) {
        throw new PasswordExpiredException("密码已过期");
    }
}
```

---

### Q4: 为什么不直接用数据库触发器管理密码历史？

**A:** 虽然数据库触发器可以自动记录历史，但有以下问题：

**不推荐触发器的原因：**
- ❌ 业务逻辑在数据库层，不易测试和维护
- ❌ 无法灵活控制（如跳过某些场景）
- ❌ 跨数据库迁移困难
- ❌ ORM 框架可能绕过触发器

**推荐应用层管理：**
- ✅ 业务逻辑清晰可控
- ✅ 易于测试和调试
- ✅ 支持复杂的业务规则
- ✅ 审计日志可以同步记录

---

## 使用相关

### Q5: 如何自定义密码策略？

**A:** 有两种方式：

#### 方式 1：数据库配置（推荐）

```sql
INSERT INTO credential_policy_config VALUES
(1, 'STRENGTH', '{
  "minLength": 12,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": true,
  "customRule": "your-custom-rule"
}', 10, true, NOW(), NOW());
```

#### 方式 2：代码扩展

```java
@Component
public class CustomPasswordPolicy implements PasswordPolicy {
    
    @Override
    public String getName() {
        return "CUSTOM_COMPLEXITY_CHECK";
    }
    
    @Override
    public PolicyCheckResult check(PolicyCheckContext context) {
        // 自定义校验逻辑
        if (!isComplexEnough(context.getPassword())) {
            return PolicyCheckResult.fail("密码复杂度不足");
        }
        return PolicyCheckResult.pass();
    }
}
```

---

### Q6: 如何处理密码过期？

**A:** 密码过期有三种处理方式：

#### 1. 宽限期模式（推荐）

```json
{
  "enabled": true,
  "maxDays": 90,
  "graceLoginCount": 3  // 过期后允许登录3次
}
```

**用户体验：**
- 过期后允许登录，但每次登录都提示修改密码
- 超过宽限次数后拒绝登录

#### 2. 立即强制修改

```json
{
  "enabled": true,
  "maxDays": 90,
  "graceLoginCount": 0  // 过期后立即拒绝
}
```

**用户体验：**
- 过期后立即拒绝登录
- 必须通过"忘记密码"流程重置

#### 3. 警告但不强制

```json
{
  "enabled": true,
  "maxDays": 90,
  "warningDaysBefore": 7,
  "graceLoginCount": -1  // 永不强制（仅警告）
}
```

---

### Q7: 如何实现强制修改密码？

**A:** 通过 `force_change` 标记实现：

#### 管理员重置密码

```java
// 1. 管理员重置
public void resetPassword(Long userId) {
    String tempPassword = RandomUtil.randomString(8);
    userService.updatePassword(userId, tempPassword);
    
    // 2. 标记强制修改
    passwordExpirationService.setForceChange(userId, true);
}
```

#### Gateway 拦截

```java
// 检查 Token 中的强制修改标记
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String forceChange = extractClaim("force_change_password");
    
    if ("true".equals(forceChange)) {
        String path = exchange.getRequest().getURI().getPath();
        
        // 只允许访问修改密码和登出接口
        if (!isPasswordChangeOrLogout(path)) {
            return unauthorized(exchange, "必须修改初始密码");
        }
    }
    
    return chain.filter(exchange);
}
```

#### 前端处理

```javascript
// 登录后检查
axios.post('/api/login', credentials)
  .then(response => {
    const { token, forceChangePassword } = response.data;
    
    if (forceChangePassword) {
      // 跳转到修改密码页面
      router.push('/change-password?force=true');
    } else {
      // 正常进入系统
      router.push('/dashboard');
    }
  });
```

---

### Q8: 支持哪些加密算法？

**A:** 使用 Spring Security 的 `DelegatingPasswordEncoder`，支持多种算法：

```java
// 自动识别算法前缀
{bcrypt}$2a$10$...  // BCrypt（推荐）
{pbkdf2}...         // PBKDF2
{scrypt}...         // SCrypt
{argon2}...         // Argon2（最安全）
{sha256}...         // SHA-256（不推荐）
```

**推荐配置：**
```java
@Bean
public PasswordEncoder passwordEncoder() {
    // 默认使用 BCrypt，兼容其他算法
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

**升级策略：**
```java
// 自动升级到更安全的算法
if (passwordEncoder.upgradeEncoding(user.getPassword())) {
    String newPassword = passwordEncoder.encode(rawPassword);
    userService.updatePassword(user.getId(), newPassword);
}
```

---

## 集成相关

### Q9: Auth Service 如何集成？

**A:** Auth Service 只集成 Framework（本地调用，无远程依赖）：

```java
@Service
@RequiredArgsConstructor
public class OAuth2UserDetailsAuthenticationProvider {
    
    private final PasswordValidator passwordValidator;
    private final PasswordExpirationChecker expirationChecker;
    
    @Override
    protected void additionalAuthenticationChecks(UserDetails user, 
        OAuth2UserDetailsAuthenticationToken token) {
        
        // 1. 密码正确性校验
        if (!passwordEncoder.matches(presentedPassword, user.getPassword())) {
            throw new BadCredentialsException("密码不正确");
        }
        
        // 2. 构建校验上下文
        PolicyCheckContext context = PolicyCheckContext.builder()
            .password(presentedPassword)
            .username(user.getUsername())
            .tenantId(user.getTenantId())
            .build();
        
        // 3. 本地策略校验
        PasswordCheckResult result = passwordValidator.validate(context);
        
        // 4. 检查密码过期（本地 Framework）
        ExpirationStatus status = expirationChecker.check(user.getId());
        
        if (status.isExpired()) {
            if (status.getGraceLoginRemaining() > 0) {
                // 允许登录但添加警告
                token.setAdditionalParameter("force_change_password", true);
            } else {
                throw new PasswordExpiredException("密码已过期");
            }
        }
    }
}
```

**优势：**
- ✅ 无远程调用，性能最优
- ✅ 不依赖外部服务，可用性高
- ✅ 策略可以通过配置文件快速调整

---

### Q10: 如何在 Gateway 控制路由？

**A:** Gateway 检查 Token 中的凭证状态标记：

```java
@Component
public class CredentialStatusFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        
        // 1. 从 JWT Token 中提取凭证状态
        ServerHttpRequest request = exchange.getRequest();
        String authorization = request.getHeaders().getFirst("Authorization");
        
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            Claims claims = jwtUtil.parseToken(token);
            
            // 2. 检查是否需要强制修改密码
            Boolean forceChange = claims.get("force_change_password", Boolean.class);
            
            if (Boolean.TRUE.equals(forceChange)) {
                String path = request.getURI().getPath();
                
                // 3. 只允许访问密码修改和登出接口
                if (!isAllowedPath(path)) {
                    return buildUnauthorizedResponse(exchange, 
                        "FORCE_CHANGE_PASSWORD", 
                        "必须修改密码后才能访问其他功能");
                }
            }
        }
        
        return chain.filter(exchange);
    }
    
    private boolean isAllowedPath(String path) {
        return path.matches(".*/user/change-password") ||
               path.matches(".*/auth/logout");
    }
    
    @Override
    public int getOrder() {
        return -100; // 在认证过滤器之后执行
    }
}
```

---

### Q11: 如何记录审计日志？

**A:** 通过异步方式记录，不影响主流程：

```java
@Service
@RequiredArgsConstructor
public class PasswordChangeService {
    
    private final RemoteCredentialService credentialService;
    
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = SecurityContext.getUserId();
        
        try {
            // 1. 执行业务逻辑
            userService.updatePassword(userId, dto.getNewPassword());
            
            // 2. 异步记录审计日志（成功）
            recordAuditAsync(userId, "PASSWORD_CHANGE", "SUCCESS", null);
            
        } catch (Exception e) {
            // 3. 异步记录审计日志（失败）
            recordAuditAsync(userId, "PASSWORD_CHANGE", "FAILURE", e.getMessage());
            throw e;
        }
    }
    
    @Async("auditExecutor")
    private void recordAuditAsync(Long userId, String action, String result, String reason) {
        try {
            CredentialAuditDTO audit = new CredentialAuditDTO();
            audit.setUserId(userId);
            audit.setUserType("MEMBER");
            audit.setAction(action);
            audit.setResult(result);
            audit.setFailureReason(reason);
            audit.setIpAddress(RequestUtil.getIpAddress());
            audit.setUserAgent(RequestUtil.getUserAgent());
            
            credentialService.recordAudit(audit);
        } catch (Exception e) {
            // 审计失败不影响主流程
            log.error("记录审计日志失败", e);
        }
    }
}
```

---

## 性能相关

### Q12: 策略校验性能如何？

**A:** 性能测试结果（单次校验）：

| 场景 | 耗时 | QPS |
|-----|------|-----|
| 本地 Framework 校验 | 5-10ms | 20,000+ |
| RPC 调用 Service | 20-30ms | 5,000+ |
| 包含历史密码检查 | 30-50ms | 3,000+ |

**优化建议：**

1. **Auth Service 使用本地 Framework**
```java
// ✅ 推荐：本地校验
passwordValidator.validate(context);  // 5-10ms

// ❌ 不推荐：远程调用
credentialService.validatePasswordStrength(...);  // 20-30ms
```

2. **启用策略缓存**
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1小时
```

3. **历史密码查询优化**
```sql
-- 使用索引
CREATE INDEX idx_user_created ON password_history(user_id, created_at DESC);

-- 限制查询数量
SELECT * FROM password_history 
WHERE user_id = ? 
ORDER BY created_at DESC 
LIMIT 5;
```

---

### Q13: 如何优化历史密码查询？

**A:** 使用环形缓冲策略：

```java
@Service
public class PasswordHistoryService {
    
    private static final int MAX_HISTORY_COUNT = 5;
    
    /**
     * 添加历史记录（自动清理旧记录）
     */
    @Transactional
    public void addHistory(Long userId, String passwordHash) {
        // 1. 插入新记录
        PasswordHistory history = new PasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(passwordHash);
        historyMapper.insert(history);
        
        // 2. 删除超过限制的旧记录
        long count = historyMapper.countByUserId(userId);
        if (count > MAX_HISTORY_COUNT) {
            historyMapper.deleteOldest(userId, count - MAX_HISTORY_COUNT);
        }
    }
}
```

**优势：**
- ✅ 存储空间固定，不会无限增长
- ✅ 查询效率高，只需扫描5条记录
- ✅ 无需定期清理任务

---

### Q14: 缓存策略是什么？

**A:** 多级缓存策略：

#### 1. 策略配置缓存（Redis）

```java
@Cacheable(
    value = "credential:policy", 
    key = "#tenantId + ':' + #policyType",
    unless = "#result == null"
)
public PolicyConfig getPolicy(Long tenantId, String policyType) {
    return policyRepository.findPolicy(tenantId, policyType);
}
```

**TTL:** 1小时  
**失效策略:** 策略更新时主动失效

#### 2. 密码过期状态缓存（本地缓存）

```java
@Cacheable(
    value = "local:expiration", 
    key = "#userId",
    cacheManager = "localCacheManager"
)
public ExpirationStatus getExpirationStatus(Long userId) {
    return expirationRepository.findByUserId(userId);
}
```

**TTL:** 5分钟  
**失效策略:** 密码修改时主动失效

#### 3. 历史密码不缓存

原因：历史密码每次修改都会变化，缓存意义不大。

---

## 安全相关

### Q15: 如何防止暴力破解？

**A:** 多层防护：

#### 1. 频率限制（Redis）

```java
@Service
public class RateLimitService {
    
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

#### 2. 账号锁定

```java
public void handleFailedLogin(String username) {
    int failedCount = getFailedLoginCount(username);
    
    if (failedCount >= 5) {
        // 锁定账号30分钟
        userService.lockAccount(username, 30, TimeUnit.MINUTES);
        
        // 发送安全警告邮件
        emailService.sendSecurityAlert(username, "账号因多次登录失败被锁定");
    }
}
```

#### 3. 验证码

```java
if (failedLoginCount >= 3) {
    // 要求输入验证码
    if (!captchaService.validate(captchaCode)) {
        throw new CaptchaInvalidException("验证码错误");
    }
}
```

---

### Q16: 审计日志如何保护？

**A:** 审计日志只读，通过定期归档管理：

#### 1. 只读接口

```java
// ✅ 提供查询接口
@GetMapping("/audit/user/{userId}")
public R<List<AuditLog>> queryLogs(@PathVariable Long userId) {
    return R.ok(auditService.queryLogs(userId));
}

// ❌ 不提供删除接口
// @DeleteMapping("/audit/{id}")  // 不存在
```

#### 2. 数据库权限

```sql
-- 审计日志表只授予 INSERT 和 SELECT 权限
GRANT INSERT, SELECT ON credential_audit_log TO 'credential_service'@'%';

-- 不授予 UPDATE 和 DELETE 权限
```

#### 3. 定期归档

```java
@Scheduled(cron = "0 0 2 1 * ?")  // 每月1号凌晨2点
public void archiveOldAuditLogs() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(6);
    
    // 1. 归档到历史表
    auditService.archiveLogsBefore(cutoffDate);
    
    // 2. 删除已归档的数据
    auditService.deleteLogsBefore(cutoffDate);
}
```

---

### Q17: 如何处理敏感数据？

**A:** 敏感数据加密和脱敏：

#### 1. 密码哈希存储

```java
// 使用 BCrypt 单向加密
String passwordHash = passwordEncoder.encode(rawPassword);

// 历史密码也存储哈希值，而不是明文
passwordHistoryService.addHistory(userId, passwordHash);
```

#### 2. 审计日志脱敏

```java
public class CredentialAuditDTO {
    private String ipAddress;        // 脱敏：192.168.1.xxx
    private String userAgent;        // 截取前100字符
    private String failureReason;    // 不包含密码明文
}

// 脱敏处理
public void recordAudit(CredentialAuditDTO audit) {
    audit.setIpAddress(maskIpAddress(audit.getIpAddress()));
    audit.setUserAgent(truncate(audit.getUserAgent(), 100));
    auditMapper.insert(audit);
}
```

#### 3. 日志过滤

```java
// logback-spring.xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <encoder>
        <!-- 过滤敏感信息 -->
        <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
    
    <!-- 不记录包含密码的日志 -->
    <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
        <evaluator>
            <expression>message.contains("password=")</expression>
        </evaluator>
        <onMatch>DENY</onMatch>
    </filter>
</appender>
```

---

## 运维相关

### Q18: 如何监控密码安全状态？

**A:** 通过定时任务和指标监控：

#### 1. 密码过期监控

```java
@Scheduled(cron = "0 0 8 * * ?")  // 每天早上8点
public void checkPasswordExpiration() {
    // 统计即将过期的密码（7天内）
    long expiringSoon = passwordExpirationService.countExpiringWithinDays(7);
    
    if (expiringSoon > 100) {
        alertService.sendAlert(
            "密码过期预警",
            String.format("有 %d 个用户的密码将在7天内过期", expiringSoon)
        );
    }
    
    // 发送 Prometheus 指标
    meterRegistry.gauge("password.expiring.count", expiringSoon);
}
```

#### 2. 弱密码扫描

```java
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点
public void scanWeakPasswords() {
    List<User> weakPasswordUsers = userService.findWeakPasswordUsers();
    
    if (!weakPasswordUsers.isEmpty()) {
        // 记录指标
        meterRegistry.gauge("password.weak.count", weakPasswordUsers.size());
        
        // 发送邮件提醒
        emailService.sendWeakPasswordWarning(weakPasswordUsers);
    }
}
```

#### 3. Grafana 仪表盘

```yaml
# prometheus.yml
- job_name: 'credential-security'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ['credential-service:9090']
```

**监控指标：**
- `password_validation_total` - 密码校验总数
- `password_validation_failure_rate` - 密码校验失败率
- `password_expiring_count` - 即将过期密码数量
- `password_weak_count` - 弱密码用户数量

---

### Q19: 如何批量修改策略？

**A:** 通过 SQL 脚本或管理接口：

#### 方式 1：SQL 批量更新

```sql
-- 批量调整所有租户的密码最小长度
UPDATE credential_policy_config 
SET policy_config = JSON_SET(policy_config, '$.minLength', 10)
WHERE policy_type = 'STRENGTH';

-- 批量启用密码过期策略
UPDATE credential_policy_config 
SET policy_config = JSON_SET(policy_config, '$.enabled', true)
WHERE policy_type = 'EXPIRATION';
```

#### 方式 2：管理接口

```java
@RestController
@RequestMapping("/api/admin/credential/policy")
public class PolicyBatchAPI {
    
    /**
     * 批量更新策略
     */
    @PostMapping("/batch-update")
    @AdminOnly
    public R<Void> batchUpdate(@RequestBody BatchUpdateDTO dto) {
        for (Long tenantId : dto.getTenantIds()) {
            policyService.updatePolicy(tenantId, dto.getPolicyType(), dto.getConfig());
        }
        return R.ok();
    }
    
    /**
     * 应用模板策略
     */
    @PostMapping("/apply-template")
    @AdminOnly
    public R<Void> applyTemplate(@RequestBody ApplyTemplateDTO dto) {
        PolicyTemplate template = templateService.getTemplate(dto.getTemplateId());
        
        for (Long tenantId : dto.getTenantIds()) {
            policyService.applyTemplate(tenantId, template);
        }
        
        return R.ok();
    }
}
```

---

### Q20: 如何处理大量过期密码？

**A:** 分批处理，避免影响系统：

```java
@Service
public class PasswordExpirationBatchService {
    
    /**
     * 批量处理过期密码（分批执行）
     */
    @Scheduled(cron = "0 0 3 * * ?")  // 每天凌晨3点
    public void processExpiredPasswords() {
        int batchSize = 1000;
        int processedCount = 0;
        
        while (true) {
            // 1. 分批查询过期用户
            List<PasswordExpiration> expired = expirationRepository
                .findExpired(LocalDateTime.now(), batchSize);
            
            if (expired.isEmpty()) {
                break;
            }
            
            // 2. 批量处理
            for (PasswordExpiration exp : expired) {
                try {
                    // 发送过期通知邮件
                    notificationService.sendPasswordExpiredNotification(exp.getUserId());
                    
                    // 标记已通知
                    exp.setLastWarned(LocalDateTime.now());
                    expirationRepository.save(exp);
                    
                    processedCount++;
                    
                } catch (Exception e) {
                    log.error("处理过期密码失败: userId={}", exp.getUserId(), e);
                }
            }
            
            // 3. 休息1秒，避免过载
            Thread.sleep(1000);
        }
        
        log.info("批量处理完成，共处理 {} 个过期密码", processedCount);
    }
}
```

---

## 扩展相关

### Q21: 如何添加新的策略类型？

**A:** 实现 `PasswordPolicy` 接口：

```java
@Component
public class PasswordEntropyPolicy implements PasswordPolicy {
    
    @Override
    public String getName() {
        return "PASSWORD_ENTROPY";
    }
    
    @Override
    public int getPriority() {
        return 25; // 在强度策略之后执行
    }
    
    @Override
    public PolicyCheckResult check(PolicyCheckContext context) {
        double entropy = calculateEntropy(context.getPassword());
        double minEntropy = getMinEntropy(context.getTenantId());
        
        if (entropy < minEntropy) {
            return PolicyCheckResult.fail(
                String.format("密码熵值不足: %.2f (要求: %.2f)", entropy, minEntropy)
            );
        }
        
        return PolicyCheckResult.pass();
    }
    
    @Override
    public boolean isEnabled(Tenant tenant) {
        return policyConfigService.isEnabled(tenant.getId(), getName());
    }
    
    private double calculateEntropy(String password) {
        // 熵值计算逻辑
        // ...
    }
}
```

**配置：**
```sql
INSERT INTO credential_policy_config VALUES
(NULL, 'ENTROPY', '{"minEntropy": 3.5}', 25, true, NOW(), NOW());
```

---

### Q22: 如何支持 MFA？

**A:** 扩展 `CredentialAuthenticator` 接口（预留设计）：

```java
// 1. 定义 MFA 认证器接口
public interface CredentialAuthenticator {
    
    /**
     * 凭证类型
     */
    CredentialType getType();  // PASSWORD, OTP, WEBAUTHN
    
    /**
     * 校验凭证
     */
    AuthenticationResult authenticate(AuthenticationContext context);
    
    /**
     * 是否需要额外验证
     */
    boolean requiresAdditionalFactor();
}

// 2. 实现 OTP 认证器
@Component
public class OtpAuthenticator implements CredentialAuthenticator {
    
    @Override
    public CredentialType getType() {
        return CredentialType.OTP;
    }
    
    @Override
    public AuthenticationResult authenticate(AuthenticationContext context) {
        String otpCode = context.getOtpCode();
        String secret = userOtpService.getSecret(context.getUserId());
        
        if (totpUtil.verify(otpCode, secret)) {
            return AuthenticationResult.success();
        }
        
        return AuthenticationResult.failure("OTP验证失败");
    }
    
    @Override
    public boolean requiresAdditionalFactor() {
        return false; // OTP 不需要额外因子
    }
}

// 3. 在 Auth Service 中集成
public Authentication authenticate(String username, String password, String otpCode) {
    // 第一步：密码认证
    passwordAuthenticator.authenticate(context);
    
    // 第二步：OTP 认证（如果启用）
    if (mfaEnabled) {
        otpAuthenticator.authenticate(context);
    }
    
    return authentication;
}
```

---

### Q23: 如何支持 Passkey？

**A:** 扩展凭证类型，支持 WebAuthn：

```java
// 1. 定义 Passkey 认证器
@Component
public class PasskeyAuthenticator implements CredentialAuthenticator {
    
    @Override
    public CredentialType getType() {
        return CredentialType.PASSKEY;
    }
    
    @Override
    public AuthenticationResult authenticate(AuthenticationContext context) {
        // 1. 验证 WebAuthn 签名
        PublicKeyCredential credential = context.getPublicKeyCredential();
        AuthenticatorResponse response = credential.getResponse();
        
        // 2. 从数据库加载公钥
        PasskeyCredential storedCredential = passkeyService
            .getCredential(context.getUserId(), credential.getId());
        
        // 3. 验证签名
        boolean verified = webAuthnService.verify(
            storedCredential.getPublicKey(),
            response.getClientDataJSON(),
            response.getAuthenticatorData(),
            response.getSignature()
        );
        
        if (verified) {
            return AuthenticationResult.success();
        }
        
        return AuthenticationResult.failure("Passkey验证失败");
    }
}

// 2. 数据模型
@Entity
public class PasskeyCredential {
    private Long id;
    private Long userId;
    private String credentialId;
    private byte[] publicKey;
    private int signCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
```

---

## 总结

本 FAQ 涵盖了凭证安全模块的常见问题，更多详细信息请参考：

- [架构设计](./ARCHITECTURE.md)
- [实施指南](./IMPLEMENTATION-GUIDE.md)
- [策略配置指南](./POLICY-GUIDE.md)
- [API 参考](./API-REFERENCE.md)
- [迁移指南](./MIGRATION-GUIDE.md)

如有其他问题，欢迎提交 Issue 或联系技术支持。
