# Design

## 方案摘要

租户创建成员改为独立入参 `TenantMemberCreateInput` 与出参 `TenantMemberCreated`。新服务 `TenantMemberOnboarding` 在同一事务内按手机号匹配或新建全局账号，再写入租户成员与部门。平台路径继续使用 `MemberCreateInput`。

## 数据模型与接口

- `TenantMemberCreateInput`：`displayName`、`phone`、`email`、`departments`
- `TenantMemberCreated`：`id`、`version`、`accountCreated`、`initialPassword`
- `POST /v1/tenant/members` 改为上述契约；破坏既有按 `accountId` 的租户创建客户端
- 响应含密码时使用与平台创建账号相同的信封加密

## 数据流与失败处理

1. 校验未知字段、手机号形态和部门形状。
2. 按手机号列出未删除账号：多个拒绝；唯一且停用拒绝；零条则新建账号。
3. 新建时若登录名已被占用，拒绝。
4. 字段写入校验后插入成员、替换部门、写审计。
5. 唯一约束冲突视为成员资格已存在。

## 迁移与回滚

无需数据迁移。回滚需恢复租户创建接口对 `MemberCreateInput` 的接收。

## 测试策略

单元测试覆盖新建、复用、重复、多账号、停用；手工更新契约 `routes.json`、`schemas.json`、`openapi.json`。
