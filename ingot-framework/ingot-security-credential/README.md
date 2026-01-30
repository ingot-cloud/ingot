# Ingot Security Credential

凭证安全模块 - 统一的密码策略管理和凭证生命周期控制。

## 模块简介

Ingot Security Credential 是 Ingot Cloud 的凭证安全管理模块，提供统一的密码策略校验、凭证生命周期管理和安全审计功能。

### 核心特性

- **策略化校验** - 密码强度、过期、历史等策略统一管理
- **多租户支持** - 每个租户可独立配置安全策略
- **联邦式架构** - 数据分散存储，服务自治
- **Framework + Service** - 本地校验与远程服务结合，性能与灵活性兼顾
- **易于扩展** - 支持自定义策略，预留 MFA、Passkey 扩展点

## 5 分钟快速开始

### 步骤 1：添加依赖

#### Auth Service（本地集成）

```gradle
dependencies {
    // 只依赖 Framework
    implementation project(':ingot-framework:ingot-security-credential')
}
```

#### Member / PMS Service（完整集成）

```gradle
dependencies {
    // Framework（本地校验）
    implementation project(':ingot-framework:ingot-security-credential')
    
    // API（RPC调用）
    implementation project(':ingot-service:ingot-credential:ingot-credential-api')
}
```

---

### 步骤 2：配置

```yaml
ingot:
  credential:
    # Credential Security Service 地址
    service:
      url: http://localhost:9090
    
    # 本地策略配置（可选，用于降级）
    policy:
      strength:
        enabled: true
        min-length: 8
        require-uppercase: true
        require-lowercase: true
        require-digit: true
        require-special-char: false
```

---

### 步骤 3：使用

#### 用户注册时校验密码

```java
@Service
@RequiredArgsConstructor
public class RegisterService {
    
    private final RemoteCredentialService credentialService;
    
    public void register(RegisterDTO dto) {
        // 校验密码强度
        R<PasswordCheckResult> result = credentialService.validatePasswordStrength(
            dto.getPassword(),
            dto.getUsername(),
            SecurityContext.getTenantId()
        );
        
        if (!result.getData().isPassed()) {
            throw new BusinessException(result.getData().getFailureMessage());
        }
        
        // 保存用户...
    }
}
```

#### 用户修改密码时检查历史

```java
public void changePassword(ChangePasswordDTO dto) {
    // 检查历史密码
    R<Boolean> reused = credentialService.isPasswordReused(
        userId,
        "MEMBER",
        dto.getNewPassword()
    );
    
    if (reused.getData()) {
        throw new BusinessException("该密码已使用过，请更换新密码");
    }
    
    // 更新密码...
}
```

#### Auth Service 登录时检查过期

```java
@Override
protected void additionalAuthenticationChecks(UserDetails user, 
    OAuth2UserDetailsAuthenticationToken token) {
    
    // 密码正确性校验
    if (!passwordEncoder.matches(presentedPassword, user.getPassword())) {
        throw new BadCredentialsException("密码不正确");
    }
    
    // 本地检查密码过期
    PasswordCheckResult result = credentialChecker.check(buildContext(user));
    
    if (result.isExpired()) {
        throw new PasswordExpiredException("密码已过期");
    }
    
    if (result.isForceChange()) {
        token.setAdditionalParameter("force_change_password", true);
    }
}
```

---

## 模块结构

```
ingot-credential-security/
├── ingot-security-credential/        # Framework 层
│   ├── policy/                            # 策略接口
│   │   ├── PasswordPolicy
│   │   ├── PasswordStrengthPolicy
│   │   ├── PasswordExpirationPolicy
│   │   └── PasswordHistoryPolicy
│   ├── validator/                         # 校验器
│   │   ├── PasswordValidator
│   │   └── ValidatorChain
│   ├── model/                             # 数据模型
│   │   ├── PasswordCheckResult
│   │   ├── PolicyCheckContext
│   │   └── CredentialStatus
│   └── exception/                         # 异常
│       ├── PasswordWeakException
│       ├── PasswordExpiredException
│       └── PasswordReusedException
│
└── ingot-credential/     # Service 层
    ├── api/                               # 对外接口
    │   ├── dto/
    │   ├── vo/
    │   └── rpc/
    │       ├── RemoteCredentialService
    │       └── RemotePolicyService
    └── provider/                          # 服务实现
        ├── service/
        ├── mapper/
        └── web/
```

---

## 主要功能

### 1. 密码强度策略

控制密码的复杂度要求：

```json
{
  "minLength": 8,
  "maxLength": 32,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": true,
  "forbiddenPatterns": ["password", "123456", "admin"],
  "forbidUserAttributes": true
}
```

### 2. 密码过期策略

管理密码的生命周期：

```json
{
  "enabled": true,
  "maxDays": 90,
  "warningDaysBefore": 7,
  "graceLoginCount": 3,
  "forceChangeAfterReset": true
}
```

### 3. 密码历史策略

防止密码重复使用：

```json
{
  "enabled": true,
  "keepRecentCount": 5,
  "checkCount": 5
}
```

### 4. 审计日志

记录所有凭证相关操作：

```java
CredentialAuditDTO audit = new CredentialAuditDTO();
audit.setUserId(userId);
audit.setUserType("MEMBER");
audit.setAction("PASSWORD_CHANGE");
audit.setResult("SUCCESS");

credentialService.recordAudit(audit);
```

---

## 配置示例

### 标准企业配置

```sql
INSERT INTO credential_policy_config VALUES
(NULL, 'STRENGTH', '{
  "minLength": 8,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": false
}', 10, true, NOW(), NOW()),
(NULL, 'EXPIRATION', '{
  "enabled": true,
  "maxDays": 90
}', 20, true, NOW(), NOW()),
(NULL, 'HISTORY', '{
  "enabled": true,
  "keepRecentCount": 5
}', 30, true, NOW(), NOW());
```

### 高安全配置（金融/医疗）

```sql
INSERT INTO credential_policy_config VALUES
(1, 'STRENGTH', '{
  "minLength": 12,
  "requireSpecialChar": true,
  "minEntropyBits": 50
}', 10, true, NOW(), NOW()),
(1, 'EXPIRATION', '{
  "enabled": true,
  "maxDays": 60,
  "graceLoginCount": 1
}', 20, true, NOW(), NOW()),
(1, 'HISTORY', '{
  "enabled": true,
  "keepRecentCount": 10
}', 30, true, NOW(), NOW());
```

### C端应用配置

```sql
INSERT INTO credential_policy_config VALUES
(2, 'STRENGTH', '{
  "minLength": 6,
  "requireSpecialChar": false
}', 10, true, NOW(), NOW()),
(2, 'EXPIRATION', '{
  "enabled": false
}', 20, true, NOW(), NOW()),
(2, 'HISTORY', '{
  "enabled": true,
  "keepRecentCount": 3
}', 30, true, NOW(), NOW());
```

---

## API 概览

### RPC 接口

```java
// 密码强度校验
R<PasswordCheckResult> validatePasswordStrength(
    String password, 
    String username, 
    Long tenantId
);

// 历史密码检查
R<Boolean> isPasswordReused(
    Long userId, 
    String userType, 
    String password
);

// 密码过期检查
R<ExpirationStatus> checkPasswordExpiration(
    Long userId, 
    String userType
);

// 审计日志记录
R<Void> recordAudit(CredentialAuditDTO audit);
```

### REST 接口

```http
# 查询策略配置
GET /v1/credential/policy

# 更新策略配置
PUT /v1/credential/policy/{policyType}

# 测试密码强度
POST /v1/credential/policy/test

# 查询审计日志
GET /v1/credential/audit/user/{userId}
```

---

## 相关文档

### 核心文档

- [架构设计](../../docs/credential-security/ARCHITECTURE.md) - 完整的架构说明
- [实施指南](../../docs/credential-security/IMPLEMENTATION-GUIDE.md) - 分阶段实施步骤
- [策略配置指南](../../docs/credential-security/POLICY-GUIDE.md) - 策略配置详解

### 参考文档

- [API 参考](../../docs/credential-security/API-REFERENCE.md) - 完整的 API 文档
- [迁移指南](../../docs/credential-security/MIGRATION-GUIDE.md) - 从现有系统迁移
- [常见问题](../../docs/credential-security/FAQ.md) - FAQ

---

## 设计理念

### 职责分离

- **Framework** - 策略校验引擎（无状态、本地调用）
- **Service** - 数据管理、策略配置、审计日志（有状态、远程服务）

### 联邦式数据

- **集中管理** - 策略配置在 Credential Service
- **分散存储** - 密码历史在各自服务的数据库

### 降级友好

- Auth Service 只依赖 Framework
- Service 不可用时自动降级到本地配置

---

## 性能指标

| 场景 | 耗时 | QPS |
|-----|------|-----|
| 本地 Framework 校验 | 5-10ms | 20,000+ |
| RPC 调用 Service | 20-30ms | 5,000+ |
| 包含历史密码检查 | 30-50ms | 3,000+ |

---

## 扩展性

### 自定义策略

```java
@Component
public class CustomPasswordPolicy implements PasswordPolicy {
    
    @Override
    public String getName() {
        return "CUSTOM_POLICY";
    }
    
    @Override
    public PolicyCheckResult check(PolicyCheckContext context) {
        // 自定义校验逻辑
        return PolicyCheckResult.pass();
    }
}
```

### MFA 扩展点（预留）

```java
public interface CredentialAuthenticator {
    CredentialType getType();  // PASSWORD, OTP, WEBAUTHN
    AuthenticationResult authenticate(AuthenticationContext context);
    boolean requiresAdditionalFactor();
}
```

---

## 版本说明

**当前版本：** 0.1.0

### 版本历史

- `0.1.0` (2026-01-21)
  - 初始版本
  - 密码强度策略
  - 密码过期策略
  - 密码历史策略
  - 审计日志

### 路线图

- `0.2.0` - MFA 基础支持
- `0.3.0` - Passkey / WebAuthn 支持
- `0.4.0` - 零信任凭证管理
- `1.0.0` - 生产就绪版本

---

## 许可证

Copyright © 2026 Ingot Cloud

本模块采用与 Ingot Cloud 项目相同的许可证。

---

## 技术支持

- 📚 [完整文档](../../docs/credential-security/)
- 🐛 [提交 Issue](https://github.com/ingot-cloud/ingot/issues)
- 💬 技术交流群

---

**快速链接：**
- [5分钟快速开始](#5-分钟快速开始)
- [完整架构设计](../../docs/credential-security/ARCHITECTURE.md)
- [API 参考文档](../../docs/credential-security/API-REFERENCE.md)
- [常见问题解答](../../docs/credential-security/FAQ.md)
