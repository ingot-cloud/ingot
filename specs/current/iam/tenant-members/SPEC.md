# 租户成员入职

## 添加成员

`POST /v1/tenant/members` 权限为 `iam-tenant:member:create`。请求体是 `TenantMemberCreateInput`：`displayName`、`phone`、可选 `email`、`departments`。`phone` 为 11 位大陆手机号，不含国家码。`departments` 必须出现，允许空数组；第一项为主部门。未知字段拒绝。格式错误返回「手机号格式不正确」，不向调用方暴露正则。

响应是 `TenantMemberCreated`：`id`、`version`、`accountCreated`、`rejoined`，以及仅在新建账号时出现的 `initialPassword`。含密码的响应与平台创建账号相同，使用 data_only 信封加密。重新加入时 `accountCreated=false`、`rejoined=true`、不返回密码。

平台 `POST /v1/platform/members` 仍接收 `MemberCreateInput`，按已有账号 ID 关联。

## 手机号匹配

按未删除账号的手机号匹配：

- 没有账号：新建全局账号。登录名和登录手机号都使用该手机号，初始密码由安全框架生成，创建来源为管理员创建。`accountCreated=true`。
- 唯一且启用：加入当前组织，不修改账号资料、密码或启用状态。`accountCreated=false`，省略密码。
- 唯一且停用：「该手机号对应的账号已停用」。
- 多个账号：「该手机号对应多个账号，请联系平台管理员」。
- 该手机号已被其他账号用作登录名：「该手机号已被其他账号用作登录名」。
- 该账号已是本组织在职或暂停成员：「成员资格已存在」。
- 该账号对应本组织已移出成员：复用原成员行重新加入，成员 ID 不变，`rejoined=true`。

成员通讯录写入提交的手机号和邮箱。同一事务内完成账号匹配或创建、成员插入或重新加入、部门替换和审计。无需数据迁移。

## 重新加入

`POST /v1/tenant/members/{id}/rejoin` 权限同样为 `iam-tenant:member:create`。请求体是 `TenantMemberRejoinInput`：`expectedVersion`、`displayName`、`phone`、可选 `email`、`departments`。未知字段拒绝。当前资格不是 `REMOVED` 时返回「成员资格已存在」；版本冲突返回修订冲突。账号停用仍报「该手机号对应的账号已停用」。详情 `GET /v1/tenant/members/{id}` 仍排除已移出。

重新加入覆盖 `display_name`、`phone`、`email`，`avatar` 置空，不修改全局账号。`joinedAt` 仍取首次加入的 `created_at`。部门按本次提交整体替换，允许空。同一事务清理该成员的有效角色分配（改为 `REVOKED`）、其作为管理员的委派及派生授权、委派接收成员、用户组成员、部门负责人、人群成员和策略选择器成员；硬删关系时递增所属用户组、部门、委派、人群 version。策略选择器无独立 version。成功后授权热缓存失效。

## 任职关系保存

成员入职及任职替换共用成员部门差量仓库，在既有组织/成员锁和事务内读取完整旧关系。保留未变任职，只删除移除项、插入新增项并更新变化的主部门标记；主部门切换先解除旧主标记。关系未变化时不执行关系DML，入口原有空值/重复校验、对象范围、版本、审计与失效规则保持。

联合主键和主部门生成列唯一约束保留，完整规则见[数据结构与持久化](../database-persistence/SPEC.md)。

## 登录手机号

`MemberRecord.loginPhone` 是已关联全局账号的登录手机号，公开字段，可空，不按当前域联系手机号做字段策略脱敏。租户与平台的成员列表、详情和导出会返回它。没有账号或账号未设置手机号时省略。`phone` 仍是当前域联系手机号，可能脱敏，不回退到登录手机号。
