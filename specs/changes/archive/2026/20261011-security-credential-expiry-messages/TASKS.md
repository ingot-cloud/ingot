# Tasks

## 实施任务

- [x] T1：Plan 对齐及四工件落盘；实施批准来自用户明确要求实施本计划。
- [x] T2：共享枚举、meta 常量及账号侧分类／合并。
  - 依赖：T1 approved
  - 验收：初始优先、普通按错误码、未知回退，不改变阻断和锁定信息。
- [x] T3：类型化动态文案解析、默认资源与默认 checker 装配。
  - 依赖：T2
  - 验收：逐次读取当前 Environment；删除／空白回退；保留自定义 checker。
- [x] T4：三个环境独立 Nacos 文件与 Auth optional 刷新导入。
  - 依赖：T1 approved
  - 验收：三个 dataId 同结构、常量一致，不改密码策略配置。

## 验证任务

- [x] V1：枚举、账号上下文、动态配置、实际认证链／HTTP 与 BFF 定向回归。
- [x] V2：IAM、Member、Auth、BFF 编译及改动检查。
- [x] V3：实际 Nacos 发布／动态刷新及平台、租户登录人工验收；2026-10-12 用户确认测试成功，作为验收依据。

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新：凭证安全提示契约与安全配置归属
- [x] Change 已记录完成信息并归档

## 自动化验证记录（2026-10-11）

JDK 21、本机 Gradle 依赖缓存，offline 执行通过；共 62 个测试，失败／错误／跳过均为 0。

| 测试类 | 用例数 | 覆盖 |
| --- | ---: | --- |
| CredentialExpirationReasonTest | 5 | 共享枚举 JSON、未知值、MyBatis 枚举契约 |
| CredentialExpirationMessagesTest | 21 | 三场景默认、独立配置、修改／删除、空白、纯文本、异常原因／绑定、RPC JSON、原准入 |
| InUserUtcMetadataTest | 1 | 锁定 UTC 元数据回归 |
| AuthContextSupportExpirationTest | 13 | 初始／重置截止、登录不延长、宽限、双重过期优先、meta 合并、依赖缺失／异常、旧原因清理 |
| LoginFailureMessagesAuthenticationTest | 4 | Auth 装配限定、缺省、生产认证链／HTTP、平台／租户、自定义 checker 覆盖 |
| DefaultInitialPasswordServiceTest | 8 | 初始密码策略与过期判断回归 |
| PasswordExpirationUtcBoundaryTest | 1 | 普通过期 UTC 边界回归 |
| BffAuthServicePreAuthorizeErrorTest | 8 | 平台／租户提示及错误码透传、既有错误回退 |
| BffAuthServiceLoginLockTest | 1 | 锁定登录回归 |

可复现命令：

```bash
./gradlew \
  :ingot-framework:ingot-commons:test --tests '*CredentialExpirationReasonTest' \
  :ingot-framework:ingot-security:ingot-security-common:test --tests '*CredentialExpirationMessagesTest' --tests '*InUserUtcMetadataTest' \
  :ingot-framework:ingot-security:ingot-security-account:ingot-security-account-web-support:test \
  :ingot-framework:ingot-security:ingot-security-authorization-server:test --tests '*LoginFailureMessagesAuthenticationTest' \
  :ingot-framework:ingot-security:ingot-security-credential:test --tests '*DefaultInitialPasswordServiceTest' --tests '*PasswordExpirationUtcBoundaryTest' \
  :ingot-service:ingot-bff:test --tests '*BffAuthServicePreAuthorizeErrorTest' --tests '*BffAuthServiceLoginLockTest' \
  :ingot-service:ingot-iam:ingot-iam-provider:compileJava \
  :ingot-service:ingot-member:ingot-member-provider:compileJava \
  :ingot-service:ingot-auth:ingot-auth-provider:compileJava \
  :ingot-service:ingot-bff:compileJava --offline --console=plain
```

另已通过 `git diff --check`；三 Group 模板内容与配置键一致，Auth 三环境均使用 `optional:nacos:in-security-messages.yml?refreshEnabled=true`。实际刷新回调及完整 Auth 启动不以 Environment 单测替代。

## V3 人工验收清单

1. 在目标环境记录 Group、版本、验收账号及时间；先发布 Auth，再发布 IAM/Member 原因提供方。过渡期旧提供方返回通用提示。
2. Auth 使用的 Group 不发布该 dataId 时启动，确认可正常启动并返回内置提示；再将仓库对应 Group 的 `in-security-messages.yml` 发布到 Nacos。
3. 平台、租户各使用一个初始／重置密码过期账号，确认对应默认提示；普通密码宽限耗尽账号显示普通提示。有效期内的强制改密账号仍走原改密流程，宽限内普通账号仍可登录。
4. 不重启 Auth，分别修改三项文案并等待 Nacos 刷新完成，重新登录确认最新纯文本。通用场景可在 Auth 先发布、旧提供方未更新的过渡阶段验证。
5. 分别删除配置键、设置空白文案、删除整个 dataId，重新登录确认恢复对应默认；重新发布文件确认后续刷新继续生效。
6. 记录 HTTP 状态、code/message、BFF 页面提示及登录入口；V3 全部通过后勾选 REQUIREMENTS，更新 `specs/current/security/credential-security/` 并归档。

验收记录（2026-10-12）：用户确认“测试成功，归档相关spec，并且提交代码”。结合 62 个自动化测试与服务编译结果完成本 change 验收；V3 完成依据为用户确认，没有补造 Group、版本、账号及逐项日志。当前基线已同步，变更移入 `specs/changes/archive/2026/20261011-security-credential-expiry-messages/`。
