# 验收记录

日期：2026-10-08。状态：validating。实现已完成，以下记录区分已验证内容、既有失败与未执行的验收，不以构建成功代替跨服务联调。

## 实现范围

公共 API 时间模块由 core 自动装配，JSON/MVC/Feign 收发带偏移 ISO，响应 UTC Z；现有 LocalDateTime 时间点明确为 UTC。Redis/OAuth 的独立 mapper、类型信息和内部编码保留。普通业务写入、账号与凭证截止、事件链路、网关名单、验证码及缓存记录使用 UTC。数据源各环境、连接池、业务初始化脚本、开发示例已同步；按用户确认，公共 in-database.yml 移除重复的时区连接属性和会话时区初始化 SQL，UTC 连接和会话时区由各业务服务 JDBC URL 的参数设置。

BFF 事务使用公开 DTO，expiresAt 为 Instant，响应 ISO 且 OpenAPI 声明 string/date-time；存储仍为秒时间戳。TSS Spring 的 ZoneId 默认为上海，任务注册和 Cron 更新显式携带时区。前端共享包提供解析、当地展示与选择提交，已适配列表、详情、锁定、授权和名单输入。规范、AGENTS、skill、IAM active 契约及前端副本已同步。

## 已通过

| 范围 | 证据 |
|---|---|
| API 编码 | InApiTimeModuleTest：UTC/上海/纽约 JVM，偏移等价、纳秒、日期/日内时间/Duration、严格拒绝旧格式/无偏移/非法/空字符串/数字；内部 LocalDateTime 编码保留 |
| 独立业务服务 | ApiTimeAutoConfigurationTest：不依赖 IAM，自动装配 mapper，真实 MVC JSON/query/form 一致，含 Optional/集合参数与 Date 超界值，非法时间点 400；长整数与日期语义保留 |
| Feign | FeignTimeContractTest：三个 JVM 时区，参数转换和 WebFlux 独立 mapper 使用同一 UTC ISO 契约 |
| 安全边界 | LockStateUtcBoundaryTest、RedisAccountLockSignalAdapterTest、DefaultInitialPasswordServiceTest、PasswordExpirationUtcBoundaryTest：截止前/瞬间/后、永久/无限期限及宽限；锁定信号 TTL 向上取整且按 UTC 截止值判断 |
| 元数据 | InUserUtcMetadataTest：API offset/Z、内部 UTC LocalDateTime、内部 ISO T 字符串及 epoch millis 不随 JVM 时区漂移 |
| 真实 MySQL | MySQL 8.0.44，UtcJdbcContractTest：三个 JVM 时区验证 UTC 会话、DATETIME(6)/TIMESTAMP(6)、默认 CURRENT_TIMESTAMP、真实 MyBatis-Plus 实体读写、生产 InJdbcRegisteredClientRepository 的签发/密钥截止时间；独立临时数据库、账号与临时表全部清理 |
| 真实 Redis | VerificationCodeUtcTest 与 RedisAuthorizationTimeContractTest：验证码 Instant/TTL、授权码及 Access/Refresh 索引、授权快照、数字元数据、会话续期不改 sid/issuedAt、刷新后 jti、撤销与 TTL；独立测试键及注册表成员全部清理 |
| JWT/OAuth | JwtTimeProtocolTest：生产 JWT 签名装配及验签，三个 JVM 时区下 iat/nbf/exp 为 NumericDate，OAuth expires_in 为数字；API Instant 同时输出 UTC ISO |
| 鉴权组件回归 | authorization-server 的身份绑定、授权码转换、会话注册/撤销、并发策略与踢出回归通过；BFF 的绑定、CSRF、登录锁定、完成、退出、事务视图回归通过 |
| IAM | 常规全量 388 项：373 通过、15 条件跳过；随后启用 RoleWorkspaceMySqlHttpTest，15 项真实 HTTP + 隔离 MySQL 8.4 全部通过，测试服务器 mapper 与生产时间契约一致 |
| 其他后端 | commons、core、cache、Feign、gateway-rule-client、credential、credential-data、account-core/adapter、event-store-mysql、event-transport-feign、TSS、Security provider 的相关回归通过；Member provider 无现有测试用例，编译通过 |
| TSS | SpringTaskTimeZoneTest：三个 JVM 时区注册上海 09:00 → UTC 01:00，更新 09:30 → UTC 01:30，非法 ZoneId 启动失败 |
| XXL Admin | 本地运行的 XXL-Job Admin 3.3.2，实际登录读取 Cron 预览并退出：09:00/09:30 分别对应 UTC 01:00/01:30；未创建或运行业务任务，运行容器保持既有状态 |
| 前端 | shared 时间工具 4 项通过；插件 229 项、admin-common 93 项、http-client 27 项、auth-core 16 项、vite-config 45 项通过；admin-core 420 项通过、1 项既有失败（见下方） |
| 浏览器 | Chromium 的 UTC/上海/纽约独立上下文，默认与显式展示时区、本地 09:00 提交、毫秒选择器视图、原始纳秒模型保留、纽约夏令时转换通过 |
| 构建/检查 | IAM/Auth/Member/Security/BFF bootJar 通过；前端 packages/plugins/apps 类型检查、共享包与四应用生产构建、改动文件 lint、分层边界与文档检查通过；apps 单元任务无测试文件 |

测试使用独立 Gradle JVM 进程；IAM 大量 MyBatis fixture 在默认 512 MB 下内存不足，验证时设置 maxHeapSize=2g、forkEvery=40 后完整通过，未改动项目默认测试堆配置。

## 可重复命令

后端核心：执行 commons/core/Feign/credential/account-core/authorization-server/TSS 的 test 任务；完整发布构建执行五个 provider/BFF 的 bootJar。Gradle 可使用临时 init script 设置测试堆与进程轮换。真实数据库测试参数见 [接入规范](../../../../docs/guides/TIME-CONTRACT.md)，不得指向需要迁移的历史业务数据。

```bash
# 独立真实 MySQL 实体与 OAuth JDBC 验证
./gradlew :ingot-service:ingot-auth:ingot-auth-provider:test --tests '*UtcJdbcContractTest'
# 真实 Redis 授权/会话与验证码；配置 TIME_TEST_REDIS_* 后执行
./gradlew :ingot-service:ingot-auth:ingot-auth-provider:test --tests '*RedisAuthorizationTimeContractTest'
./gradlew :ingot-framework:ingot-verification-code:test --tests '*VerificationCodeUtcTest'
# 仓库原有真实 IAM HTTP/MySQL 测试，要求已有 mysql:8.4 镜像
IAM_MYSQL_HTTP_TEST=true ./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --tests '*RoleWorkspaceMySqlHttpTest'
```

前端使用仓库声明的 Node 22.17.0 / pnpm 10.12.4：`pnpm build`、`pnpm type-check:packages`、`pnpm type-check:plugins`、`pnpm type-check:apps`、`pnpm test:plugins`、admin-common 的 test:unit，以及 `pnpm test:time-browser`。浏览器测试需要 Chromium，可通过 PLAYWRIGHT_CHROMIUM_EXECUTABLE 指定已有二进制。

## 既有失败

1. security-common / InUserIdentityTest.callerCannotMutateBoundDepartmentList：现有 getDeptIds 返回可变 ArrayList，测试期待 UnsupportedOperationException。该测试及列表复制/返回实现与 HEAD 一致，本次仅修改 UTC 元数据转换；没有扩大为身份集合重构。
2. admin-core / defaultTokens.test.ts 的“公开 Token 名与浅色 CSS 声明一致”：tokens.css 已有 --in-permission-panel-bg，但 IN_THEME_TOKEN_NAMES 缺少该名称。测试、Token 清单与 CSS 均未在本次改动。

这两项保留为全量回归未通过事实，不将相关时间验证结果称为整个仓库全部通过。

## 尚未验收

- [ ] 在成套启动的新版本 Auth/IAM/Member/Security/BFF/Gateway 与前端的全新环境，通过实际浏览器完成平台/租户登录、授权码兑换、Access/Refresh、退出、撤销和并发踢出，检查期限、Cookie、索引及 TTL。
- [ ] 对上述两项既有失败完成独立处理或验收决议。
- [ ] 成套发布条件确认后更新 current/framework/time-contract 及关联能力基线，再归档双方 change。

当前只有代码构建、组件/真实数据库测试和已有中间件的只读联调；未部署或替换本机已有业务服务。未处理历史数据库、Redis、队列迁移。本次按用户要求创建代码与规范提交；变更仍为 validating，待验收项保持开放。
