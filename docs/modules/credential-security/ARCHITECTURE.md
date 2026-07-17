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

`PasswordPolicy` 接口定义了密码策略的统一行为，不同的具体策略实现不同的校验规则。通过策略模式，系统可以灵活地添加、删除或修改密码策略。

### 2. 责任链模式 (Chain of Responsibility)

密码校验通过责任链传递，每个策略按优先级依次校验。责任链模式使得策略的执行顺序可配置，并支持阻断式策略。

### 3. 联邦式数据架构

策略集中管理（Credential Service），数据分散存储（各服务独立数据库）。这种架构实现了策略的统一管理和数据的服务自治。

## 数据模型设计

### 集中式：策略配置表

Credential Service 数据库存储策略配置和审计日志，实现策略的集中管理。

**核心表：**
- `credential_policy_config` - 策略配置表（支持租户级和全局配置）
- `credential_audit_log` - 审计日志表

### 联邦式：密码历史和过期表

各服务独立数据库存储密码历史和过期信息，实现数据的服务自治。

**核心表：**
- `password_history` - 密码历史表（环形缓冲设计）
- `password_expiration` - 密码过期表

**数据库脚本位置：**
- `databases/ingot_security.sql` - Credential Service
- `databases/migrations/add_password_history.sql` - 其他服务

## 扩展新的策略

系统支持灵活地扩展新策略，只需：

1. 实现 `PasswordPolicy` 接口
2. 注册为 Spring Bean
3. 配置策略参数（数据库或配置文件）

新策略会自动加入校验链，无需修改现有代码。详见技术文档。

## 性能优化

### 1. 策略配置缓存

使用 Redis 缓存策略配置（TTL: 1小时），策略更新时主动失效。

### 2. 密码历史查询优化

使用索引优化查询，环形缓冲自动清理旧记录，只保留最近N条。

### 3. 批量校验优化

批量加载策略配置避免N+1查询，支持并行校验提升性能。

### 4. 异步审计日志

审计日志异步记录，不影响主业务流程，使用独立线程池。

## 安全设计

### 1. 访问控制

策略配置修改需要管理员权限，通过权限注解控制访问。

### 2. 审计日志保护

审计日志只读，不提供删除接口，通过定期归档管理历史数据。

### 3. 密码哈希保护

历史密码使用 BCrypt 加密存储，使用安全的密码比对方法。

### 4. 防止暴力破解

使用 Redis 限制密码校验频率，防止暴力破解攻击。

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
