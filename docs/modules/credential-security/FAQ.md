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

1. **数据库配置（推荐）** - 直接修改 credential_policy_config 表
2. **代码扩展** - 实现 PasswordPolicy 接口并注册为 Spring Bean

详见技术文档。

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

**A:** 通过以下步骤实现：

1. 管理员重置密码时设置 `force_change` 标记
2. Gateway 拦截请求，检查 Token 中的强制修改标记
3. 前端登录后检查并跳转到修改密码页面

详见技术文档。

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

**A:** Auth Service 只集成 Framework（本地调用，无远程依赖），在认证流程中增加密码策略校验和过期检查。

**优势：**
- ✅ 无远程调用，性能最优
- ✅ 不依赖外部服务，可用性高

---

### Q10: 如何在 Gateway 控制路由？

**A:** Gateway 通过 GlobalFilter 检查 Token 中的凭证状态标记（如 force_change_password），限制用户只能访问修改密码和登出接口。

---

### Q11: 如何记录审计日志？

**A:** 通过异步方式记录（使用 @Async），不影响主业务流程。审计记录失败不会影响主流程执行。

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

**A:** 使用环形缓冲策略，固定存储数量，自动清理旧记录。

**优势：**
- ✅ 存储空间固定，不会无限增长
- ✅ 查询效率高，只需扫描固定条数
- ✅ 无需定期清理任务

---

### Q14: 缓存策略是什么？

**A:** 多级缓存策略：

1. **策略配置缓存（Redis）** - TTL: 1小时，策略更新时主动失效
2. **密码过期状态缓存（本地）** - TTL: 5分钟，密码修改时主动失效
3. **历史密码不缓存** - 数据变化频繁，缓存意义不大

---

## 安全相关

### Q15: 如何防止暴力破解？

**A:** 多层防护：

1. **频率限制（Redis）** - 限制每个用户15分钟内最多尝试5次
2. **账号锁定** - 失败5次后锁定账号30分钟，并发送安全警告邮件
3. **验证码** - 失败3次后要求输入验证码

---

### Q16: 审计日志如何保护？

**A:** 三层保护：

1. **只读接口** - 只提供查询接口，不提供删除接口
2. **数据库权限** - 只授予 INSERT 和 SELECT 权限
3. **定期归档** - 每月归档6个月前的数据到历史表

---

### Q17: 如何处理敏感数据？

**A:** 三层保护：

1. **密码哈希存储** - 使用 BCrypt 单向加密，历史密码也存储哈希值
2. **审计日志脱敏** - IP地址脱敏，User Agent截取，不记录密码明文
3. **日志过滤** - 过滤包含密码的日志内容

---

## 运维相关

### Q18: 如何监控密码安全状态？

**A:** 通过定时任务和指标监控：

1. **密码过期监控** - 每天统计即将过期密码，超过阈值发送告警
2. **弱密码扫描** - 每天扫描弱密码用户，发送提醒邮件
3. **Grafana 仪表盘** - 展示密码安全相关指标（校验总数、失败率、过期数量等）

---

### Q19: 如何批量修改策略？

**A:** 有两种方式：

1. **SQL 批量更新** - 直接通过 SQL 批量修改 policy_config 字段
2. **管理接口** - 通过批量更新接口或模板应用接口

---

### Q20: 如何处理大量过期密码？

**A:** 分批处理，避免影响系统：

每天凌晨3点执行定时任务，分批查询过期用户（每批1000条），逐个发送通知邮件，批次间休息1秒避免过载。

---

## 扩展相关

### Q21: 如何添加新的策略类型？

**A:** 三步完成：

1. 实现 `PasswordPolicy` 接口（定义策略名称、优先级、校验逻辑）
2. 注册为 Spring Bean（使用 @Component）
3. 配置策略参数（数据库或配置文件）

---

### Q22: 如何支持 MFA？

**A:** 通过扩展 `CredentialAuthenticator` 接口实现多因子认证：

1. 定义 MFA 认证器接口（凭证类型、校验方法）
2. 实现具体认证器（OTP、WebAuthn等）
3. 在认证流程中集成多因子校验

详见未来规划文档。

---

### Q23: 如何支持 Passkey？

**A:** 通过扩展凭证类型支持 WebAuthn：

1. 定义 Passkey 认证器（实现 WebAuthn 签名验证）
2. 创建数据模型存储公钥和凭证信息
3. 实现注册和认证流程

详见未来规划文档。

---

## 总结

本 FAQ 涵盖了凭证安全模块的常见问题，更多详细信息请参考：

- [架构设计](./ARCHITECTURE.md)
- [实施指南](./IMPLEMENTATION-GUIDE.md)
- [策略配置指南](./POLICY-GUIDE.md)
- [API 参考](./API-REFERENCE.md)
- [迁移指南](./MIGRATION-GUIDE.md)

如有其他问题，欢迎提交 Issue 或联系技术支持。
