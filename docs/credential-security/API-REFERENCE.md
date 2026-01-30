# 凭证安全 API 参考文档

## API 概述

Ingot Cloud 凭证安全模块提供三类 API：

| API 类型 | 使用场景 | 调用方式 |
|---------|---------|---------|
| **RPC 接口** | 服务间调用（Member/PMS → Credential Service） | Feign Client |
| **REST 接口** | 管理后台（策略配置、审计查询） | HTTP REST |
| **Framework API** | Auth Service 本地集成 | 直接调用 |

## RPC 接口

### RemoteCredentialService

服务间凭证校验接口。

#### 1. validatePasswordStrength

校验密码强度是否符合策略要求。

**方法签名：**
```java
R<PasswordCheckResult> validatePasswordStrength(
    String password, 
    String username, 
    Long tenantId
);
```

**参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| password | String | ✅ | 原始密码（明文） |
| username | String | ✅ | 用户名（用于检测包含用户名） |
| tenantId | Long | ✅ | 租户ID |

**返回值：**

```java
public class PasswordCheckResult {
    private boolean passed;              // 是否通过
    private List<String> failureReasons; // 失败原因列表
    private List<String> warnings;       // 警告信息列表
    private Map<String, Object> metadata; // 元数据
}
```

**调用示例：**

```java
@Service
@RequiredArgsConstructor
public class MemberUserService {
    
    private final RemoteCredentialService credentialService;
    
    public void register(RegisterDTO dto) {
        // 1. 校验密码强度
        R<PasswordCheckResult> result = credentialService.validatePasswordStrength(
            dto.getPassword(),
            dto.getUsername(),
            SecurityContext.getTenantId()
        );
        
        if (!result.isSuccess() || !result.getData().isPassed()) {
            throw new BusinessException("密码强度不符合要求: " + 
                result.getData().getFailureMessage());
        }
        
        // 2. 保存用户
        // ...
    }
}
```

**错误示例：**

```json
{
  "code": 400,
  "message": "密码强度校验失败",
  "data": {
    "passed": false,
    "failureReasons": [
      "密码长度不足，至少需要8个字符",
      "密码必须包含至少一个大写字母"
    ],
    "warnings": [],
    "metadata": {
      "currentLength": 6,
      "requiredLength": 8
    }
  }
}
```

---

#### 2. isPasswordReused

检查密码是否与历史密码重复。

**方法签名：**
```java
R<Boolean> isPasswordReused(
    Long userId, 
    String userType, 
    String password
);
```

**参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| userId | Long | ✅ | 用户ID |
| userType | String | ✅ | 用户类型：PMS / MEMBER |
| password | String | ✅ | 原始密码（明文） |

**返回值：**

```java
{
  "code": 200,
  "data": true  // true=重复使用，false=未重复
}
```

**调用示例：**

```java
public void changePassword(ChangePasswordDTO dto) {
    Long userId = SecurityContext.getUserId();
    
    // 1. 检查历史密码
    R<Boolean> reusedResult = credentialService.isPasswordReused(
        userId,
        "MEMBER",
        dto.getNewPassword()
    );
    
    if (reusedResult.getData()) {
        throw new BusinessException("该密码已在最近使用过，请更换新密码");
    }
    
    // 2. 更新密码
    // ...
}
```

---

#### 3. checkPasswordExpiration

检查密码是否过期。

**方法签名：**
```java
R<ExpirationStatus> checkPasswordExpiration(
    Long userId, 
    String userType
);
```

**参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| userId | Long | ✅ | 用户ID |
| userType | String | ✅ | 用户类型：PMS / MEMBER |

**返回值：**

```java
public class ExpirationStatus {
    private boolean expired;              // 是否已过期
    private LocalDateTime expireAt;       // 过期时间
    private int daysUntilExpire;          // 距离过期天数（负数表示已过期）
    private int graceLoginRemaining;      // 剩余宽限登录次数
    private boolean forceChange;          // 是否强制修改
    private String message;               // 提示信息
}
```

**调用示例：**

```java
// Auth Service 登录时检查
public Authentication authenticate(String username, String password) {
    // 1. 校验密码正确性
    // ...
    
    // 2. 检查密码过期
    R<ExpirationStatus> expirationResult = credentialService
        .checkPasswordExpiration(userId, "PMS");
    
    ExpirationStatus status = expirationResult.getData();
    
    if (status.isExpired()) {
        if (status.getGraceLoginRemaining() > 0) {
            // 允许登录但添加警告
            authentication.addWarning("PASSWORD_EXPIRED", 
                "密码已过期，剩余宽限登录次数: " + status.getGraceLoginRemaining());
            authentication.setForceChangePassword(true);
        } else {
            throw new PasswordExpiredException("密码已过期，请重置密码");
        }
    } else if (status.getDaysUntilExpire() <= 7) {
        // 即将过期提醒
        authentication.addWarning("PASSWORD_EXPIRING", 
            "密码将在 " + status.getDaysUntilExpire() + " 天后过期");
    }
    
    return authentication;
}
```

---

#### 4. recordAudit

记录凭证操作审计日志。

**方法签名：**
```java
R<Void> recordAudit(CredentialAuditDTO audit);
```

**参数说明：**

```java
public class CredentialAuditDTO {
    private Long userId;           // 用户ID
    private String userType;       // 用户类型：PMS / MEMBER
    private String action;         // 操作：PASSWORD_CHANGE, PASSWORD_RESET, PASSWORD_VALIDATE
    private Long operatorId;       // 操作人ID（管理员重置时）
    private String operatorType;   // 操作人类型
    private String result;         // 结果：SUCCESS, FAILURE
    private String failureReason;  // 失败原因
    private String ipAddress;      // IP地址
    private String userAgent;      // User Agent
}
```

**调用示例：**

```java
public void changePassword(ChangePasswordDTO dto) {
    Long userId = SecurityContext.getUserId();
    
    try {
        // 1. 修改密码
        userService.updatePassword(userId, dto.getNewPassword());
        
        // 2. 记录审计日志（成功）
        CredentialAuditDTO audit = new CredentialAuditDTO();
        audit.setUserId(userId);
        audit.setUserType("MEMBER");
        audit.setAction("PASSWORD_CHANGE");
        audit.setResult("SUCCESS");
        audit.setIpAddress(RequestUtil.getIpAddress());
        audit.setUserAgent(RequestUtil.getUserAgent());
        
        credentialService.recordAudit(audit);
        
    } catch (Exception e) {
        // 3. 记录审计日志（失败）
        CredentialAuditDTO audit = new CredentialAuditDTO();
        audit.setUserId(userId);
        audit.setUserType("MEMBER");
        audit.setAction("PASSWORD_CHANGE");
        audit.setResult("FAILURE");
        audit.setFailureReason(e.getMessage());
        audit.setIpAddress(RequestUtil.getIpAddress());
        
        credentialService.recordAudit(audit);
        
        throw e;
    }
}
```

---

### RemotePolicyService

策略配置管理接口。

#### 1. getPolicyConfig

获取指定租户的策略配置。

**方法签名：**
```java
R<PolicyConfig> getPolicyConfig(Long tenantId, String policyType);
```

**参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| tenantId | Long | ✅ | 租户ID |
| policyType | String | ✅ | 策略类型：STRENGTH / EXPIRATION / HISTORY |

**返回值：**

```java
public class PolicyConfig {
    private Long id;
    private Long tenantId;
    private String policyType;
    private JSONObject policyConfig;  // 策略配置JSON
    private int priority;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**调用示例：**

```java
// 获取当前租户的强度策略
R<PolicyConfig> result = policyService.getPolicyConfig(
    SecurityContext.getTenantId(),
    "STRENGTH"
);

PolicyConfig config = result.getData();
int minLength = config.getPolicyConfig().getIntValue("minLength");
```

---

#### 2. updatePolicyConfig

更新策略配置（需要管理员权限）。

**方法签名：**
```java
R<Void> updatePolicyConfig(PolicyConfigDTO config);
```

**参数说明：**

```java
public class PolicyConfigDTO {
    private Long tenantId;           // 租户ID，NULL表示全局
    private String policyType;       // 策略类型
    private JSONObject policyConfig; // 策略配置
    private int priority;            // 优先级
    private boolean enabled;         // 是否启用
}
```

**调用示例：**

```java
PolicyConfigDTO config = new PolicyConfigDTO();
config.setTenantId(SecurityContext.getTenantId());
config.setPolicyType("STRENGTH");
config.setPolicyConfig(JSONObject.parseObject("{\"minLength\": 10, ...}"));
config.setEnabled(true);

policyService.updatePolicyConfig(config);
```

---

#### 3. listTenantPolicies

获取租户的所有策略配置。

**方法签名：**
```java
R<List<PolicyVO>> listTenantPolicies(Long tenantId);
```

**返回值：**

```java
public class PolicyVO {
    private String policyType;       // 策略类型
    private String policyName;       // 策略名称
    private JSONObject policyConfig; // 策略配置
    private boolean enabled;         // 是否启用
    private boolean inherited;       // 是否继承自全局
}
```

**调用示例：**

```java
R<List<PolicyVO>> result = policyService.listTenantPolicies(
    SecurityContext.getTenantId()
);

for (PolicyVO policy : result.getData()) {
    System.out.println(policy.getPolicyType() + ": " + 
        (policy.isInherited() ? "继承" : "自定义"));
}
```

---

## REST API

### 策略管理 API

#### 1. 查询当前租户策略

```http
GET /v1/credential/policy
Authorization: Bearer {token}
```

**响应示例：**

```json
{
  "code": 200,
  "data": [
    {
      "policyType": "STRENGTH",
      "policyName": "密码强度策略",
      "policyConfig": {
        "minLength": 8,
        "requireUppercase": true,
        "requireLowercase": true,
        "requireDigit": true,
        "requireSpecialChar": true
      },
      "enabled": true,
      "inherited": false
    },
    {
      "policyType": "EXPIRATION",
      "policyName": "密码过期策略",
      "policyConfig": {
        "enabled": true,
        "maxDays": 90,
        "warningDaysBefore": 7
      },
      "enabled": true,
      "inherited": true
    }
  ]
}
```

---

#### 2. 更新策略配置

```http
PUT /v1/credential/policy/{policyType}
Authorization: Bearer {token}
Content-Type: application/json

{
  "minLength": 10,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": true
}
```

**权限要求：** `platform:credential:policy:update`

**响应示例：**

```json
{
  "code": 200,
  "message": "策略更新成功"
}
```

---

#### 3. 测试密码强度

```http
POST /v1/credential/policy/test
Authorization: Bearer {token}
Content-Type: application/json

{
  "password": "Test@1234",
  "username": "zhangsan"
}
```

**响应示例：**

```json
{
  "code": 200,
  "data": {
    "passed": true,
    "score": 85,
    "level": "STRONG",
    "failureReasons": [],
    "warnings": [],
    "suggestions": [
      "密码强度良好",
      "长度达到12位会更安全"
    ]
  }
}
```

---

### 审计日志 API

#### 1. 查询用户审计日志

```http
GET /v1/credential/audit/user/{userId}?page=1&size=20
Authorization: Bearer {token}
```

**权限要求：** `platform:audit:query`

**响应示例：**

```json
{
  "code": 200,
  "data": {
    "total": 50,
    "records": [
      {
        "id": 1,
        "userId": 123,
        "userType": "MEMBER",
        "action": "PASSWORD_CHANGE",
        "result": "SUCCESS",
        "ipAddress": "192.168.1.100",
        "userAgent": "Mozilla/5.0...",
        "createdAt": "2026-01-21T10:30:00"
      },
      {
        "id": 2,
        "userId": 123,
        "userType": "MEMBER",
        "action": "PASSWORD_VALIDATE",
        "result": "FAILURE",
        "failureReason": "密码长度不足",
        "ipAddress": "192.168.1.100",
        "createdAt": "2026-01-20T15:20:00"
      }
    ]
  }
}
```

---

#### 2. 导出审计日志

```http
GET /v1/credential/audit/export?startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59
Authorization: Bearer {token}
```

**权限要求：** `platform:audit:export`

**响应：** Excel 文件下载

---

## Framework API

### PasswordValidator

密码校验器（本地调用，无网络开销）。

#### 1. validate

校验密码是否符合策略。

**方法签名：**
```java
PasswordCheckResult validate(PolicyCheckContext context);
```

**参数说明：**

```java
public class PolicyCheckContext {
    private String password;          // 待校验密码
    private String username;          // 用户名
    private String phone;             // 手机号
    private String email;             // 邮箱
    private Long tenantId;            // 租户ID
    private String userType;          // 用户类型
    private List<String> oldPasswordHashes; // 历史密码哈希（可选）
}
```

**调用示例：**

```java
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    
    private final PasswordValidator passwordValidator;
    
    public void validatePassword(String password, UserDetails user) {
        // 构建校验上下文
        PolicyCheckContext context = PolicyCheckContext.builder()
            .password(password)
            .username(user.getUsername())
            .phone(user.getPhone())
            .tenantId(user.getTenantId())
            .build();
        
        // 执行校验
        PasswordCheckResult result = passwordValidator.validate(context);
        
        if (!result.isPassed()) {
            throw new PasswordWeakException(result.getFailureMessage());
        }
    }
}
```

---

#### 2. validateWithHistory

校验密码（包含历史密码检查）。

**方法签名：**
```java
PasswordCheckResult validateWithHistory(
    String password, 
    UserDetails user, 
    List<String> oldPasswordHashes
);
```

**调用示例：**

```java
// 修改密码时
public void changePassword(Long userId, String newPassword) {
    // 1. 查询历史密码
    List<String> oldHashes = passwordHistoryService.getRecentHashes(userId, 5);
    
    // 2. 校验新密码
    UserDetails user = userService.getById(userId);
    PasswordCheckResult result = passwordValidator.validateWithHistory(
        newPassword,
        user,
        oldHashes
    );
    
    if (!result.isPassed()) {
        throw new PasswordReusedException(result.getFailureMessage());
    }
    
    // 3. 更新密码
    // ...
}
```

---

### PasswordPolicy

策略接口（可扩展自定义策略）。

**接口定义：**

```java
public interface PasswordPolicy {
    
    /**
     * 策略名称
     */
    String getName();
    
    /**
     * 优先级（数字越小优先级越高）
     */
    int getPriority();
    
    /**
     * 校验密码
     */
    PolicyCheckResult check(PolicyCheckContext context);
    
    /**
     * 是否启用（租户级）
     */
    boolean isEnabled(Tenant tenant);
}
```

**自定义策略示例：**

```java
@Component
public class CustomPasswordPolicy implements PasswordPolicy {
    
    @Override
    public String getName() {
        return "CUSTOM_ENTROPY_CHECK";
    }
    
    @Override
    public int getPriority() {
        return 25; // 在强度策略之后执行
    }
    
    @Override
    public PolicyCheckResult check(PolicyCheckContext context) {
        // 计算密码熵值
        double entropy = calculateEntropy(context.getPassword());
        
        if (entropy < 3.5) {
            return PolicyCheckResult.fail("密码复杂度不足，熵值: " + entropy);
        }
        
        return PolicyCheckResult.pass();
    }
    
    @Override
    public boolean isEnabled(Tenant tenant) {
        // 从配置中读取
        return policyConfigService.isEnabled(tenant.getId(), getName());
    }
    
    private double calculateEntropy(String password) {
        // 熵值计算逻辑
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : password.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        
        double entropy = 0.0;
        int length = password.length();
        for (int count : freq.values()) {
            double p = (double) count / length;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        
        return entropy;
    }
}
```

---

## 数据模型

### DTO 定义

#### ValidatePasswordDTO

```java
public class ValidatePasswordDTO {
    @NotBlank(message = "密码不能为空")
    private String password;
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    private String phone;
    private String email;
}
```

#### ChangePasswordDTO

```java
public class ChangePasswordDTO {
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;
    
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
    
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
```

#### PolicyConfigDTO

```java
public class PolicyConfigDTO {
    private Long tenantId;
    
    @NotBlank(message = "策略类型不能为空")
    private String policyType;
    
    @NotNull(message = "策略配置不能为空")
    private JSONObject policyConfig;
    
    private int priority = 0;
    private boolean enabled = true;
}
```

---

### VO 定义

#### PasswordCheckResultVO

```java
public class PasswordCheckResultVO {
    private boolean passed;              // 是否通过
    private int score;                   // 评分 0-100
    private String level;                // 等级：WEAK, MEDIUM, STRONG, VERY_STRONG
    private List<String> failureReasons; // 失败原因
    private List<String> warnings;       // 警告信息
    private List<String> suggestions;    // 改进建议
}
```

#### PolicyVO

```java
public class PolicyVO {
    private String policyType;       // 策略类型
    private String policyName;       // 策略名称
    private JSONObject policyConfig; // 策略配置
    private boolean enabled;         // 是否启用
    private boolean inherited;       // 是否继承自全局
    private LocalDateTime updatedAt; // 更新时间
}
```

#### AuditLogVO

```java
public class AuditLogVO {
    private Long id;
    private Long userId;
    private String userType;
    private String action;
    private String actionName;       // 操作名称（中文）
    private String result;
    private String resultName;       // 结果名称（中文）
    private String failureReason;
    private String ipAddress;
    private String location;         // IP归属地
    private LocalDateTime createdAt;
}
```

---

### 枚举类型

#### CredentialStatus

```java
public enum CredentialStatus {
    ACTIVE("正常"),
    EXPIRED("已过期"),
    FORCE_CHANGE("强制修改"),
    LOCKED("已锁定"),
    DISABLED("已禁用");
    
    private final String description;
}
```

#### PolicyType

```java
public enum PolicyType {
    STRENGTH("密码强度策略"),
    EXPIRATION("密码过期策略"),
    HISTORY("密码历史策略");
    
    private final String description;
}
```

#### AuditAction

```java
public enum AuditAction {
    PASSWORD_CHANGE("修改密码"),
    PASSWORD_RESET("重置密码"),
    PASSWORD_VALIDATE("密码校验"),
    POLICY_UPDATE("策略更新");
    
    private final String description;
}
```

---

## 错误码规范

### 错误码格式

```
CRED_{类型}{序号}
```

### 错误码列表

| 错误码 | HTTP状态 | 说明 | 处理建议 |
|-------|---------|------|---------|
| CRED_1001 | 400 | 密码长度不足 | 增加密码长度 |
| CRED_1002 | 400 | 密码缺少大写字母 | 添加大写字母 |
| CRED_1003 | 400 | 密码缺少小写字母 | 添加小写字母 |
| CRED_1004 | 400 | 密码缺少数字 | 添加数字 |
| CRED_1005 | 400 | 密码缺少特殊字符 | 添加特殊字符 |
| CRED_1006 | 400 | 密码包含禁止模式 | 更换密码 |
| CRED_1007 | 400 | 密码包含用户属性 | 不要使用用户名/手机号 |
| CRED_2001 | 400 | 密码与历史重复 | 使用未使用过的密码 |
| CRED_3001 | 401 | 密码已过期 | 修改密码后重新登录 |
| CRED_3002 | 401 | 密码需强制修改 | 修改密码 |
| CRED_4001 | 403 | 无权限修改策略 | 联系管理员 |
| CRED_5001 | 500 | 策略加载失败 | 检查配置或联系技术支持 |

### 错误响应格式

```json
{
  "code": "CRED_1001",
  "message": "密码长度不足",
  "detail": "密码长度必须至少8个字符，当前长度: 6",
  "timestamp": "2026-01-21T10:30:00",
  "path": "/api/v1/user/change-password"
}
```

---

## 调用示例

### 完整的用户注册流程

```java
@Service
@RequiredArgsConstructor
public class RegisterService {
    
    private final RemoteCredentialService credentialService;
    private final MemberUserService userService;
    private final PasswordHistoryService passwordHistoryService;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        Long tenantId = SecurityContext.getTenantId();
        
        // 1. 校验密码强度
        R<PasswordCheckResult> validateResult = credentialService
            .validatePasswordStrength(
                dto.getPassword(),
                dto.getUsername(),
                tenantId
            );
        
        if (!validateResult.isSuccess() || !validateResult.getData().isPassed()) {
            throw new BusinessException(
                "密码强度不符合要求: " + 
                validateResult.getData().getFailureMessage()
            );
        }
        
        // 2. 保存用户
        MemberUser user = new MemberUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        userService.save(user);
        
        // 3. 记录密码历史
        passwordHistoryService.addHistory(
            user.getId(),
            user.getPassword()
        );
        
        // 4. 记录审计日志
        CredentialAuditDTO audit = new CredentialAuditDTO();
        audit.setUserId(user.getId());
        audit.setUserType("MEMBER");
        audit.setAction("PASSWORD_SET");
        audit.setResult("SUCCESS");
        audit.setIpAddress(RequestUtil.getIpAddress());
        
        credentialService.recordAudit(audit);
    }
}
```

### 完整的修改密码流程

```java
@Service
@RequiredArgsConstructor
public class PasswordChangeService {
    
    private final RemoteCredentialService credentialService;
    private final MemberUserService userService;
    private final PasswordHistoryService passwordHistoryService;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = SecurityContext.getUserId();
        MemberUser user = userService.getById(userId);
        
        // 1. 校验旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        
        // 2. 校验新密码强度
        R<PasswordCheckResult> strengthResult = credentialService
            .validatePasswordStrength(
                dto.getNewPassword(),
                user.getUsername(),
                user.getTenantId()
            );
        
        if (!strengthResult.getData().isPassed()) {
            throw new BusinessException(
                "新密码强度不符合要求: " + 
                strengthResult.getData().getFailureMessage()
            );
        }
        
        // 3. 检查历史密码
        R<Boolean> reusedResult = credentialService.isPasswordReused(
            userId,
            "MEMBER",
            dto.getNewPassword()
        );
        
        if (reusedResult.getData()) {
            throw new BusinessException("该密码已在最近使用过，请更换新密码");
        }
        
        // 4. 更新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setInitPwd(false);
        userService.updateById(user);
        
        // 5. 记录密码历史
        passwordHistoryService.addHistory(userId, user.getPassword());
        
        // 6. 记录审计日志
        CredentialAuditDTO audit = new CredentialAuditDTO();
        audit.setUserId(userId);
        audit.setUserType("MEMBER");
        audit.setAction("PASSWORD_CHANGE");
        audit.setResult("SUCCESS");
        audit.setIpAddress(RequestUtil.getIpAddress());
        
        credentialService.recordAudit(audit);
    }
}
```

---

## 总结

Ingot Cloud 凭证安全模块提供了完整的 API 体系：

- ✅ **RPC 接口** - 服务间高效调用，支持密码校验、历史检查、过期检查、审计记录
- ✅ **REST 接口** - 管理后台使用，支持策略配置、审计查询、批量操作
- ✅ **Framework API** - 本地集成使用，无网络开销，性能最优
- ✅ **统一数据模型** - 清晰的 DTO/VO 定义，类型安全
- ✅ **规范错误码** - 完整的错误码体系，便于问题排查

根据不同场景选择合适的 API 类型，实现最佳的性能和用户体验。
