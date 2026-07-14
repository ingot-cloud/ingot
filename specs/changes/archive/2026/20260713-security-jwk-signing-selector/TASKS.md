# Tasks

## 实施任务

- [x] T1：创建 SDD 工件
  - 依赖：无
  - 验收：active change 目录包含 README、REQUIREMENTS、DESIGN、TASKS

- [x] T2：新增 `AuthServerJwtEncoderConfiguration`
  - 依赖：T1
  - 验收：`JwtEncoder` bean 带 `setJwkSelector`，条件为 `authServerJwkSupplier` bean 存在

- [x] T3：在 `EnableInAuthorizationServer` 注册新配置类
  - 依赖：T2
  - 验收：`@Import` 包含 `AuthServerJwtEncoderConfiguration`

- [x] T4：修复条件装配顺序
  - 依赖：T2
  - 验收：启动日志出现 `JwtEncoder configured with current-key signing selector`

## 验证任务

- [x] V1：编译 `ingot-security-authorization-server` 与 `ingot-auth`
- [x] V2：部署后多密钥 Redis 下 OAuth2 token 签发成功（2026-07-14 验收通过）

## 完成检查

- [x] 实现与 DESIGN 一致（含已记录的实施偏差）
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新：`specs/current/security/jwk-management/`
- [x] Change 已记录完成信息并归档
