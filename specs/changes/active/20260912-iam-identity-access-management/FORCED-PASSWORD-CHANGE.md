# 强制改密授权闭环（2026-10-06）

> 状态：validating；用户已批准“开始补全逻辑”，approved → implementing。

## 需求与边界

账号首次登录、管理员重置及安全策略要求改密时，允许认证但只获得 `in:init_pwd`。除原有公开接口外，仅可访问当前身份的密码状态 GET 与改密 PUT；业务、仅登录接口和超级管理员捷径均不可绕过。改密失败不解除限制，成功后撤销全部既有会话，重新登录恢复正常资格。平台和租户登录遵循同一账号状态，不改变原密码强度、历史和公开访问规则。

## 设计与契约

- 登录初始 scope 在必须改密时立即返回唯一改密 scope，不再装配角色权限。
- IAM API 自动装配通用在线授权来源，覆盖未接入数据范围 SDK 的 Auth/BFF；SDK 复用同一求值实现，IAM 本地来源优先，不发生自调用。
- 资源服务认证过滤链在可信身份及在线会话校验后检查账号改密状态；精确标记的密码端点保留认证要求。公开端点沿用原匹配规则，其余请求返回 HTTP 403 `PasswordChangeRequired`。可信状态不可用时返回 503，禁止回退到旧 token 或超级管理员声明。
- IAM 求值在热缓存和管理员分支之前检查账号状态，在线授权快照补充必需的 `passwordChangeRequired`。远端可信权限源必须识别该状态；受限时仅提供改密权限。按请求复用在线快照以避免同请求重复 RPC，不跨请求缓存改密状态。
- 新增 `GET /v1/me/password`，仅返回当前授权上下文及 `mustChangePassword`。现有 PUT 请求结构不变；服务端以账号真实状态决定首次改密资格并校验两次密码一致。成功事务提交后撤销已有访问及刷新会话。
- 正常前端 bootstrap 保持一次请求。收到改密 403 后读取最小密码状态，清理业务菜单、能力与缓存并进入 `/init`，不请求业务 bootstrap/capabilities。改密完成清理浏览器会话后回登录；通用错误处理不得递归刷新权限。
- 无 DDL 或兼容开关；共同发布 IAM、认证/资源服务框架及 SDK 消费服务、前端。缺少新快照状态时拒绝授权。

## 任务与验收

- [x] FP01：已批准需求、接口、设计和验收，写入两端 active change；不更新 current、不提交。
- [x] FP02：初始 scope、可信授权、共享 HTTP 门禁、最小密码状态和提交后会话撤销。
- [x] FP03：前端受限状态、首次跳转、错误处理与改密后重新登录。
- [x] FP04：契约生成及来源同步；后端/SDK/前端定向回归、类型、lint、依赖与构建。
- [ ] FP05（人工）：新账号及重置账号登录只可 GET/PUT 密码；普通及超级管理员直接调用业务/仅登录接口均 403；公开入口正常。错误确认及弱密码仍受限；改密后旧 token/刷新会话不可使用，新登录恢复业务。租户及远端资源服务相同，故障时 503。人工结果单独记录。
- [x] FP06（批准范围内缺陷修复）：密码 PUT 保持既有 HYBRID 整包契约，在上下文建立后真正解密请求体，再绑定与校验 CurrentPasswordInput。补充实际协议头、RSA 包裹及 AES-GCM 报文的 MVC 回归，覆盖强制／普通改密、空密码、缺少协议头及篡改；不放宽必填和改密资格。浏览器重测仍在 FP05，未勾选。

## 实施与自动化结果

2026-10-06：FP02–FP04 开发及限定自动化完成，FP05 人工验收未执行。IAM/Auth/BFF 编译通过；安全门禁/表达式/JWT 16 项、SDK 13 项、密码用例 24 项、公共模型 61 项、IAM 定向 52 项、IAM API 自动装配 3 项合计 169 项通过。自动装配测试覆盖 Auth/BFF 无 SDK 时的在线状态、超级管理员限制、本地来源优先以及 RPC 失败关闭。隔离 MySQL/真实 HTTP `RoleWorkspaceMySqlHttpTest` 15 项通过（容器已自动清理；包括平台/租户即时改密状态及原角色工作区回归）。当前 Controller MockMvc 验证实际密码注解仅开放 GET/PUT，拒绝 bootstrap/profile/capabilities，并确认不加载业务 SessionService。

前端 store/路由/错误处理 20 项通过；类型检查、只读 lint、依赖边界及平台/租户两管理台构建通过。OpenAPI 132 路径/200 操作及 3 项内部操作生成检查、Java schema 一致性、7 项 Python 契约校验通过；前端契约与来源哈希同步。未运行真实用户浏览器登录、实际 token/refresh 会话及端到端密码策略验收。

扩大运行 security-common 全部 43 项时，已有 `InUserIdentityTest.callerCannotMutateBoundDepartmentList` 失败：HEAD 的 `InUser.copyDeptIds` 返回 ArrayList，而该历史测试期待不可修改；两个文件与 HEAD 一致且本轮未改。本轮相关 16 项均通过，未调整这个与改密无关的既有列表契约。构建仍有既有 UnoCSS 图标、浏览器 crypto 外置和 bundle 体积提示，不影响构建完成。

后端命令在 ingot 执行：

```sh
./gradlew :ingot-framework:ingot-security:ingot-security-common:test --tests '*InTokenAuthFilterTest' --tests '*InSecurityExpression*' --tests '*JwtInUserConverter*' :ingot-framework:ingot-data:ingot-data-mybatis-scope:test :ingot-framework:ingot-security:ingot-security-account:ingot-security-account-core:test :ingot-framework:ingot-commons:test :ingot-service:ingot-iam:ingot-iam-provider:test --tests '*AccountIdentityServiceTest' --tests '*CurrentAccountServiceTest' --tests '*LocalAuthorizationSnapshotLoaderTest' --tests '*AuthorizationEvaluatorTest' --tests '*PasswordChangeBoundaryTest' --offline --console=plain
./gradlew :ingot-service:ingot-iam:ingot-iam-api:test :ingot-service:ingot-iam:ingot-iam-provider:compileJava :ingot-service:ingot-auth:ingot-auth-provider:compileJava :ingot-service:ingot-bff:compileJava --offline --console=plain
IAM_MYSQL_HTTP_TEST=true ./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --tests '*RoleWorkspaceMySqlHttpTest' --offline --console=plain
python3 tools/iam/build_contract.py --check
python3 tools/iam/build_internal_contract.py --check
python3 -m unittest discover -s tools/iam -p test_contract.py
```

前端命令在 ingot-admin 执行（Node 22.17.0）：

```sh
pnpm --filter @ingot/admin-core exec vitest run src/stores/modules/auth.test.ts src/net/failure.test.ts src/router/guard/userGuard.test.ts
pnpm --filter @ingot/admin-core type-check
pnpm check:boundaries
pnpm --filter @ingot/admin-platform-app build
pnpm --filter @ingot/admin-app build
```

实际人工步骤见 RESOURCE-EXTENSION-VERIFICATION 的 FP05；用户实际环境未重启、不执行 SQL、不创建提交、未更新 current。

## FP06 用户验收缺陷（2026-10-06；validating）

用户执行密码 PUT 返回 S0003 两次“不能为空”。前端 whole 模式发送 `{data: 密文}`，控制器只有 `@InCryptoHybridContext`；该注解只建立 CEK/AAD，不能解密请求体。缺少整包解密注解导致 newPassword/confirmPassword 绑定为 null。修复属于既有“请求体加密”设计的实现补齐，沿用已批准范围；无接口字段、前端报文、DDL 或密码规则变化。此前门禁 MVC 测试未装配真实加密链路，不能验证整包解密，本轮补齐该缺口。

已在密码 PUT 补齐 `@InDecrypt(CryptoType.HYBRID)`。新 MVC 回归在修复前 5 项中 3 项失败，复现有效强制／普通改密均 400，篡改报文也跳过完整性校验；修复后 5 项全部通过。合并当前账号服务 3 项、HTTP 门禁 1 项及加密框架 19 项，共 28 项通过。密码用例收到实际解密后的正确参数；空密码仍执行必填校验，缺少协议头及篡改不调用改密。没有修改前端 API 报文、实际账号或数据库；用户环境重启 IAM 后重测 FP05。

```sh
./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --tests '*CurrentPasswordEncryptionTest' --tests '*PasswordChangeBoundaryTest' --tests '*CurrentAccountServiceTest' :ingot-framework:ingot-security:ingot-security-crypto:test --offline --console=plain
```
