# JWK 多密钥签名 Selector 修复

> 状态：completed

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260713-security-jwk-signing-selector` |
| 领域 | `security` |
| 负责人 | jy |
| 创建日期 | 2026-07-13 |
| 完成日期 | 2026-07-14 |

## 目标

修复授权服务器在 JWK 密钥轮换后出现多把活跃密钥时，OAuth2 token 签发失败（`JwtEncodingException: Failed to select a key`）的问题。

## 范围

**包含：**
- 框架层为授权服务器自动配置带 `JwkSelector` 的 `JwtEncoder`
- 使用 `AuthServerJwkSupplier.getCurrentSigningKey()` 选择当前签名密钥

**不包含：**
- `ResourceServerJwkSupplier` 修改（验签路径已支持多公钥）
- 集群跨节点密钥轮换竞态修复
- Redis 数据清理或迁移

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-07-14
- 关联提交或 PR：工作区本地变更（待提交）
- 更新的 current capability：`specs/current/security/jwk-management/`
- 与原设计的差异：
  - 类级别 `@ConditionalOnBean(AuthServerJwkSupplier.class)` 改为方法级别 `@ConditionalOnBean(name = "authServerJwkSupplier")`，避免 `@Import` 早于组件扫描导致配置类被跳过
  - `authServerJwkSupplier` bean 返回类型由 `JwkSupplier` 改为 `AuthServerJwkSupplier`，确保类型注入与条件匹配
