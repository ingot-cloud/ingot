# Design

## 方案摘要

账号侧判定原因，Auth 根据当前环境配置渲染提示；文案不参与安全决策。共享枚举与 meta 为内部增量协议，外部仍返回现有 `code/message`。

## 数据模型与接口

- `ingot-commons` 增 `CredentialExpirationReason`：`INITIAL_PASSWORD_EXPIRED`、`PASSWORD_EXPIRED`、`UNKNOWN`。遵循 `@Getter`、`@RequiredArgsConstructor`、`@JsonValue`、`@EnumValue` 与 `EnumUtils` 索引契约。
- `InUserMetaKeys.CREDENTIAL_EXPIRATION_REASON` 定义 `credentialExpirationReason`，RPC 写稳定字符串；不进入 JWT。
- `LoginFailureMessageProperties` 为开放文本配置类型；前缀 `ingot.security.login.messages`。新增 `NacosConstants.IN_SECURITY_MESSAGES`；Auth 三环境 optional 导入并显式开启刷新。

## 数据流与失败处理

1. `AuthContextSupport` 沿用启用、未锁定、服务可用前提。普通校验失败仍置 false；错误码 `EXPIRED` 分类普通硬过期，其余失败分类 UNKNOWN。
2. 独立判定初始密码有效期，即使普通校验已阻断也检查分类；命中时置 false 并覆盖为 INITIAL_PASSWORD_EXPIRED。查找异常保留既有阻断及分类。
3. 合并响应已有 meta、锁定 meta 和本次原因，避免锁定信息覆盖原因。仅过期响应写原因。
4. 安全公共模块提供文案解析器，通过 `Environment`／`Binder` 在每次过期失败时新绑定类型对象，避免 `ConfigurationPropertiesRebinder` 在删除键时保留旧字段；使用 `PlaceholdersResolver.NONE` 保留 `${...}` 原文，也不解析 `{0}` 等消息模板。无手写远端拉取、缓存或策略降级。
5. 当前配置为空白／缺省时从现有安全 i18n 资源读取对应内置默认；绑定异常也回退默认，不能改变准入。
6. 授权服务自动配置仅在 `InOAuth2AuthProperties` 已装配时提供默认解析器和 checker；`@ConditionalOnMissingBean` 保留自定义覆盖。Provider 现有 MessageSource 初始化不覆盖解析器依赖。
7. checker 仅在凭证已过期时使用原因。非字符串、未知字符串或缺原因统一 UNKNOWN。保留旧构造入口，默认使用内置文案。

默认文案：

| 原因 | 文案 |
| --- | --- |
| INITIAL_PASSWORD_EXPIRED | 初始密码或重置后的密码已过期，请联系管理员重置密码后再登录。 |
| PASSWORD_EXPIRED | 密码已过期，当前无法登录，请联系管理员重置密码后再登录。 |
| UNKNOWN | 登录密码已过期，请联系管理员重置密码后再登录。 |

## 迁移与回滚

无数据库迁移。先更新 Auth，再更新 IAM/Member 原因提供方；旧响应回退通用提示，旧 Auth 忽略新增 meta。发布三环境独立 Nacos dataId；删除／清空文案回退默认。回滚代码恢复旧提示，不改变密码状态或存储。实际环境发布、重启及账号操作在人工验收中记录，不以代码编译替代。

## 测试策略

共享枚举 JSON／TypeHandler 契约；账号上下文 UTC 截止、宽限、初始／重置、双过期、依赖缺失／异常、锁定 meta 合并；checker／Environment 动态绑定与删除回退；Spring 默认／自定义装配、实际 Provider 失败及 HTTP 错误转换；BFF 平台／租户透传。运行定向回归及四服务编译，人工验证实际 Nacos 刷新及两入口登录。
