# Ingot Security Credential Data

联邦式数据管理模块 - 提供密码历史和密码过期的 MyBatis-Plus 通用实现。

## 模块说明

本模块是 `ingot-security-credential` 的数据层扩展，提供了基于 MyBatis-Plus 的通用 Mapper 和 Service 实现。

### 核心特性

- **开箱即用** - 只需添加依赖，自动配置生效
- **零代码集成** - 无需编写 Mapper 和 Service 实现
- **环形缓冲** - 密码历史自动实现环形缓冲算法
- **完整功能** - 包含密码历史和密码过期的所有功能

## 5 分钟快速开始

### 步骤 1：添加依赖

在需要联邦式数据管理的服务中（如 Member Service 或 PMS Service）：

```gradle
dependencies {
    // 只需要这一个依赖！
    implementation project(':ingot-framework:ingot-security-credential-data')
}
```

### 步骤 2：执行数据库脚本

```bash
# Member Service
mysql -u root -p ingot_member < databases/migrations/001_add_password_history_member.sql

# PMS Service
mysql -u root -p ingot_pms < databases/migrations/add_password_history.sql
```

### 步骤 3：直接使用

无需任何配置，直接注入使用：

```java
@Service
@RequiredArgsConstructor
public class MemberUserService {
    
    private final PasswordHistoryService passwordHistoryService;
    private final PasswordExpirationService passwordExpirationService;
    
    public void changePassword(Long userId, String newPassword) {
        // 1. 检查历史密码
        if (passwordHistoryService.isPasswordUsed(userId, newPassword, 5)) {
            throw new BusinessException("该密码已使用过");
        }
        
        // 2. 更新密码
        String newHash = passwordEncoder.encode(newPassword);
        userMapper.updatePassword(userId, newHash);
        
        // 3. 保存历史（环形缓冲，最多5条）
        passwordHistoryService.saveHistory(userId, newHash, 5);
        
        // 4. 更新过期时间（90天有效期）
        passwordExpirationService.updateLastChanged(userId, 90);
    }
}
```

## 提供的功能

### Password History Service

```java
public interface PasswordHistoryService {
    // 获取最近N条历史记录
    List<PasswordHistory> getRecentHistory(Long userId, int limit);
    
    // 保存密码历史（环形缓冲）
    void saveHistory(Long userId, String passwordHash, int maxRecords);
    
    // 检查密码是否已使用
    boolean isPasswordUsed(Long userId, String passwordHash, int checkCount);
    
    // 删除用户所有历史
    void deleteByUserId(Long userId);
}
```

### Password Expiration Service

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
    
    // 检查是否需要提醒
    boolean needsWarning(Long userId, int warningDaysBefore);
    
    // 更新下次提醒时间
    void updateNextWarning(Long userId);
    
    // 删除过期信息
    void deleteByUserId(Long userId);
}
```

## 技术细节

### 环形缓冲实现

使用 `sequence_number` 字段实现固定大小的环形缓冲：

```java
// 自动计算序号：1, 2, 3, ..., maxRecords, 1, 2, ...
int nextSeq = (int)((count % maxRecords) + 1);

// 通过唯一索引 uk_user_sequence 自动覆盖旧记录
```

### 自动配置

模块使用 Spring Boot 自动配置：

```java
@Configuration
@ComponentScan("com.ingot.framework.security.credential.data")
@MapperScan("com.ingot.framework.security.credential.data.mapper")
public class CredentialDataAutoConfiguration {
    // 自动配置 MyBatis-Plus 拦截器
    // 自动配置 PasswordEncoder（如果不存在）
}
```

## 版本说明

**当前版本：** 0.1.0

## 许可证

Copyright © 2026 Ingot Cloud
