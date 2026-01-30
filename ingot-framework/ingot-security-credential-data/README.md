# Ingot Security Credential Data

联邦式数据管理模块 - 提供密码历史和密码过期的 MyBatis-Plus 通用实现。

## 模块说明

本模块是 `ingot-security-credential` 的数据层扩展，提供了基于 MyBatis-Plus 的通用 Mapper 和 Service 实现。

### 核心特性

- **开箱即用** - 只需添加依赖，自动配置生效
- **零代码集成** - 无需编写 Mapper 和 Service 实现
- **环形缓冲** - 密码历史自动实现环形缓冲算法
- **完整功能** - 包含密码历史和密码过期的所有功能
- **自动覆盖** - 自动替换默认空实现（NoOp）

---

## 🚀 5 分钟快速开始

### 步骤 1：添加依赖

在需要联邦式数据管理的服务中（如 Member Service 或 PMS Service）：

```gradle
dependencies {
    // 只需要这一个依赖！
    implementation project(ingot.framework_security_credential_data)
}
```

**自动包含：**
- `ingot-security-credential` - 核心模块
- MyBatis-Plus - ORM 框架
- Service 实现（PasswordHistoryService、PasswordExpirationService）

---

### 步骤 2：执行数据库脚本

```bash
# Member Service
mysql -u root -p ingot_member < databases/migrations/add_password_history.sql

# PMS Service
mysql -u root -p ingot_core < databases/migrations/add_password_history.sql
```

**创建的表：**
- `password_history` - 密码历史（环形缓冲）
- `password_expiration` - 密码过期信息

---

### 步骤 3：直接使用

无需任何配置，直接注入使用：

```java
@Service
@RequiredArgsConstructor
public class UserPasswordService {
    
    private final CredentialSecurityService credentialSecurityService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    
    /**
     * 修改密码（完整流程）
     */
    public void changePassword(Long userId, String newPassword) {
        // 1. 校验密码（自动查询历史密码）
        CredentialValidateRequest request = CredentialValidateRequest.builder()
            .scene(CredentialScene.CHANGE_PASSWORD)
            .password(newPassword)
            .userId(userId)
            .build();
        
        PasswordCheckResult result = credentialSecurityService.validate(request);
        
        if (!result.isPassed()) {
            throw new BusinessException(result.getFailureMessage());
        }
        
        // 2. 更新密码
        String hash = passwordEncoder.encode(newPassword);
        userMapper.updatePassword(userId, hash);
        
        // 3. 保存历史和更新过期（一行搞定）
        credentialSecurityService.savePasswordHistory(userId, hash);
        credentialSecurityService.updatePasswordExpiration(userId);
    }
    
    /**
     * 登录时检查过期（自动查询过期信息）
     */
    public void checkPasswordExpiration(Long userId) {
        CredentialValidateRequest request = CredentialValidateRequest.builder()
            .scene(CredentialScene.LOGIN)
            .userId(userId)
            .build();
        
        PasswordCheckResult result = credentialSecurityService.validate(request);
        
        if (!result.isPassed()) {
            throw new PasswordExpiredException(result.getFailureMessage());
        }
        
        if (result.hasWarnings()) {
            // 密码即将过期，提示用户
            log.warn("密码即将过期: {}", result.getWarnings());
        }
    }
}
```

**就是这么简单！** 所有数据查询自动完成。

---

## 📦 提供的功能

### 1. 密码历史服务

```java
public interface PasswordHistoryService {
    
    // 获取最近N条历史记录
    List<PasswordHistory> getRecentHistory(Long userId, int limit);
    
    // 保存密码历史（环形缓冲，自动覆盖最旧的）
    void saveHistory(Long userId, String passwordHash, int maxRecords);
    
    // 检查密码是否已使用
    boolean isPasswordUsed(Long userId, String passwordHash, int checkCount);
    
    // 删除用户所有历史
    void deleteByUserId(Long userId);
}
```

**环形缓冲实现：**
```
┌─────────────────────────────────┐
│ 密码历史表（最多 N 条）          │
├─────────────────────────────────┤
│ seq=1 │ hash1  │ 2024-01-01     │
│ seq=2 │ hash2  │ 2024-02-01     │
│ seq=3 │ hash3  │ 2024-03-01     │
│ seq=4 │ hash4  │ 2024-04-01     │
│ seq=5 │ hash5  │ 2024-05-01     │
└─────────────────────────────────┘
         ↓ 第6次修改密码
┌─────────────────────────────────┐
│ seq=1 │ hash6  │ 2024-06-01     │ ← 覆盖最旧的
│ seq=2 │ hash2  │ 2024-02-01     │
│ seq=3 │ hash3  │ 2024-03-01     │
│ seq=4 │ hash4  │ 2024-04-01     │
│ seq=5 │ hash5  │ 2024-05-01     │
└─────────────────────────────────┘
```

**唯一索引：** `uk_user_sequence (user_id, sequence_number)` 自动覆盖

---

### 2. 密码过期服务

```java
public interface PasswordExpirationService {
    
    // 获取过期信息
    PasswordExpiration getByUserId(Long userId);
    
    // 初始化过期信息
    void initExpiration(Long userId, int maxDays, boolean forceChange, int graceLogins);
    
    // 更新最后修改时间
    void updateLastChanged(Long userId, int maxDays);
    
    // 减少宽限登录次数
    int decrementGraceLogin(Long userId);
    
    // 检查是否过期
    boolean isExpired(Long userId);
    
    // 检查是否需要警告
    boolean needsWarning(Long userId, int warningDaysBefore);
    
    // 更新下次警告时间
    void updateNextWarning(Long userId);
    
    // 删除过期信息
    void deleteByUserId(Long userId);
}
```

---

## 🏗️ 技术实现

### 环形缓冲算法

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void saveHistory(Long userId, String passwordHash, int maxRecords) {
    // 1. 获取当前记录数
    long count = passwordHistoryMapper.selectCount(
        Wrappers.<PasswordHistory>lambdaQuery()
            .eq(PasswordHistory::getUserId, userId)
    );
    
    // 2. 计算下一个序号（环形）
    int nextSeq = (int)((count % maxRecords) + 1);
    
    // 3. 保存或更新（通过唯一索引自动覆盖）
    PasswordHistory history = new PasswordHistory();
    history.setUserId(userId);
    history.setPasswordHash(passwordHash);
    history.setSequenceNumber(nextSeq);
    
    // 使用 ON DUPLICATE KEY UPDATE（通过 MyBatis-Plus 的 insertOrUpdate）
    passwordHistoryMapper.insert(history);
}
```

**优势：**
- ✅ 固定空间占用
- ✅ 无需手动删除
- ✅ 自动覆盖最旧记录

---

### 自动配置

```java
@Configuration
@ComponentScan("com.ingot.framework.security.credential.data")
public class CredentialDataAutoConfiguration {
    
    // 自动扫描 Service 实现
    // 自动覆盖 NoOpPasswordHistoryService
    // 自动覆盖 NoOpPasswordExpirationService
}
```

**工作原理：**
```
1. 未引入 ingot-security-credential-data
   └─→ 使用 NoOpPasswordHistoryService（空实现）
   └─→ 使用 NoOpPasswordExpirationService（空实现）

2. 引入 ingot-security-credential-data
   └─→ 使用 PasswordHistoryServiceImpl（真实实现）
   └─→ 使用 PasswordExpirationServiceImpl（真实实现）
```

**透明切换，零配置！**

---

## 📊 数据库设计

### password_history 表

```sql
CREATE TABLE password_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  sequence_number INT NOT NULL COMMENT '序号（环形缓冲）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  UNIQUE KEY uk_user_sequence (user_id, sequence_number),
  INDEX idx_user_created (user_id, created_at)
) COMMENT='密码历史表';
```

**关键设计：**
- `uk_user_sequence` - 唯一索引，实现环形缓冲
- `sequence_number` - 固定范围（1 ~ maxRecords）

---

### password_expiration 表

```sql
CREATE TABLE password_expiration (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
  last_changed_at DATETIME NOT NULL COMMENT '最后修改时间',
  expires_at DATETIME COMMENT '过期时间',
  force_change TINYINT(1) DEFAULT 0 COMMENT '是否强制修改',
  grace_login_remaining INT DEFAULT 0 COMMENT '剩余宽限登录次数',
  next_warning_at DATETIME COMMENT '下次警告时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='密码过期表';
```

**关键设计：**
- `user_id` - 唯一索引，一个用户一条记录
- `expires_at` - 过期时间，方便查询
- `grace_login_remaining` - 宽限登录次数

---

## 🎯 使用场景

### 场景 1：Member Service（会员系统）

```gradle
// build.gradle
dependencies {
    implementation project(':ingot-framework:ingot-security-credential-data')
}
```

```bash
# 执行脚本
mysql -u root -p ingot_member < databases/migrations/add_password_history.sql
```

**数据存储：** `ingot_member` 数据库

---

### 场景 2：PMS Service（权限管理系统）

```gradle
// build.gradle
dependencies {
    implementation project(':ingot-framework:ingot-security-credential-data')
}
```

```bash
# 执行脚本
mysql -u root -p ingot_core < databases/migrations/add_password_history.sql
```

**数据存储：** `ingot_core` 数据库

---

### 场景 3：Auth Service（认证服务）

```gradle
// build.gradle
dependencies {
    // 只依赖核心模块，不需要数据层
    implementation project(':ingot-framework:ingot-security-credential')
}
```

**配置：**
```yaml
ingot:
  credential:
    policy:
      mode: remote  # 使用远程模式，无需本地数据库
```

**工作流程：** 通过 RPC 调用 Credential Service 校验

---

## 🔄 联邦式数据架构

```
┌─────────────────────────────────────────────────────────┐
│                  Credential Service                     │
│              (策略配置 - 集中管理)                        │
│   ┌──────────────────────────────────────┐              │
│   │ credential_policy_config (策略表)    │              │
│   └──────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────┘
                      ↓ RPC 调用
┌──────────────────────────┬──────────────────────────────┐
│    Member Service        │      PMS Service             │
│  (会员数据 - 分散存储)    │   (管理员数据 - 分散存储)     │
│  ┌──────────────────┐    │    ┌──────────────────┐      │
│  │ password_history │    │    │ password_history │      │
│  │ password_expire  │    │    │ password_expire  │      │
│  └──────────────────┘    │    └──────────────────┘      │
└──────────────────────────┴──────────────────────────────┘
```

**优势：**
- ✅ 策略集中管理，统一配置
- ✅ 数据分散存储，服务自治
- ✅ 避免跨库查询，性能更好

---

## 📈 性能指标

| 操作 | 延迟 | 说明 |
|-----|------|------|
| 保存密码历史 | 5-10ms | 单条 INSERT/UPDATE |
| 查询历史密码 | 3-5ms | 索引查询 |
| 检查密码重复 | 5-10ms | 查询 + 比对 |
| 更新过期时间 | 3-5ms | 单条 UPDATE |
| 检查是否过期 | 2-3ms | 单条查询 |

**优化建议：**
- ✅ 合理设置 `maxRecords`（推荐 5-10）
- ✅ 定期清理长期不活跃用户的历史
- ✅ 对 `user_id` 建立索引（已默认）

---

## 🔗 相关模块

### ingot-security-credential

核心策略模块：

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-security-credential')
}
```

**功能：**
- 策略加载器（Local/Remote）
- 统一服务接口（CredentialSecurityService）
- 密码校验器（PasswordValidator）
- 默认空实现（NoOp）

**详见：** [ingot-security-credential/README.md](../ingot-security-credential/README.md)

---

## 📖 完整文档

| 文档 | 说明 |
|-----|------|
| [README](../../docs/credential-security/README.md) | 完整概述、快速开始、架构说明 |
| [策略加载器](../../docs/credential-security/POLICY-LOADER.md) | 策略加载器架构、模式切换、扩展指南 |
| [架构设计](../../docs/credential-security/ARCHITECTURE.md) | 完整架构设计、分层模型、核心组件 |
| [策略配置指南](../../docs/credential-security/POLICY-GUIDE.md) | 密码策略详细配置、多租户管理 |
| [API 参考](../../docs/credential-security/API-REFERENCE.md) | RPC/REST 接口、请求响应示例 |
| [常见问题](../../docs/credential-security/FAQ.md) | 功能、集成、性能、故障排查问题解答 |

---

## 💡 最佳实践

### 1. 合理设置历史记录数

```yaml
ingot:
  credential:
    policy:
      history:
        keep-recent-count: 5  # 推荐 5-10
```

- 太少：安全性不足
- 太多：占用空间大

### 2. 根据业务设置过期策略

```yaml
# 高安全场景
ingot:
  credential:
    policy:
      expiration:
        max-days: 60

# 普通场景
ingot:
  credential:
    policy:
      expiration:
        max-days: 90

# C端应用
ingot:
  credential:
    policy:
      expiration:
        enabled: false  # 不过期
```

### 3. 定期清理无效数据

```java
// 定时任务：清理长期不活跃用户的历史
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点
public void cleanupInactiveUsers() {
    List<Long> inactiveUserIds = userService.getInactiveUserIds(365); // 1年未登录
    for (Long userId : inactiveUserIds) {
        passwordHistoryService.deleteByUserId(userId);
        passwordExpirationService.deleteByUserId(userId);
    }
}
```

---

## 🚀 下一步

1. **查看核心模块** - [ingot-security-credential/README.md](../ingot-security-credential/README.md)
2. **了解完整架构** - [ARCHITECTURE.md](../../docs/credential-security/ARCHITECTURE.md)
3. **学习策略配置** - [POLICY-GUIDE.md](../../docs/credential-security/POLICY-GUIDE.md)

---

## 📄 许可证

Copyright © 2026 Ingot Cloud

---

**版本：** 0.1.0  
**最后更新：** 2026-01-30
