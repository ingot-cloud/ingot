# 设计：收掉租户普通通讯录

## 接口

删除：

- `GET /v1/directory/members`
- `GET /v1/directory/members/{id}`
- `GET /v1/directory/departments`
- `GET /v1/directory/context`
- `GET/PUT /v1/tenant/policies/directory`
- `POST /v1/tenant/policies/preview`

保留 `GET/PUT /v1/tenant/policies/fields`。字段规则的 `scenario` 只允许 `MANAGEMENT`。

## 模型

- `SelectionPurpose` 去掉 `DIRECTORY`。
- `PolicyScenario` 只保留 `MANAGEMENT`。
- `DefaultPolicyKind` 只保留 `FIELD`。
- 删除通讯录策略草稿、可见性求值和预览结果中只服务通讯录的类型；若类型仍被字段策略引用则保留字段部分。
- 资源目录不再注册 `iam-tenant/directory`。角色字段固化不再为该资源开分支。

## 数据

新库：

- `004` 不建 `iam_directory_policy`、`iam_directory_rule`。
- 默认策略种类检查只允许 `FIELD`，字段规则场景检查只允许 `MANAGEMENT`。
- `006` 不插入通讯录菜单、资源 `directory` / `directory-policy` / `policy`、对应动作和 DIRECTORY 默认版本。治理模板不授予这些动作。成员访问策略菜单只绑 `iam-tenant:field-policy:read`。

已有库：`databases/iam/migrations/033_remove_directory_browse.sql` 按编码删除上述菜单、授权、动作、资源、DIRECTORY 规则和默认版本，并删除两张通讯录策略表。脚本可重复执行。不改 020 及更早补丁。

建租户不再锁定 DIRECTORY 默认版本。

## 兼容

项目未上线。不保留停用动作，也不保留 DIRECTORY 场景的读取兼容。
