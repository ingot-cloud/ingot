# Tasks

> 状态：approved（按阶段逐步实施，见 phases/）

## 阶段总览

| 阶段 | 内容 | 依赖 |
|---|---|---|
| P1 | 新建 `ingot-security-replay` 通用防重放模块 + `@Idempotent` | 无 |
| P2 | crypto 基础设施（类型/工具/密钥/服务/配置/错误码） | P1 |
| P3 | Advice 集成、触发协议、R 结构响应、公钥端点与网关路由 | P2 |
| P4 | 联调、灰度切换、前端对接文档、current 基线 | P3 |

## 实施任务

### P1 通用防重放模块（[phases/01](./phases/01-replay-module.md)）· 已完成

- [x] T1.1：创建模块 `ingot-framework/ingot-security/ingot-security-replay`（build.gradle、settings 聚合、依赖别名）
- [x] T1.2：`NonceStore` + `RedisNonceStore`（`tryAcquire(key, ttl)`，Redis `setIfAbsent`）
- [x] T1.3：`ReplayGuard`/`DefaultReplayGuard` + `ReplayProperties` + `ReplayErrorCode`（超窗/重复/存储不可用错误码，namespace 隔离，单测通过）
- [x] T1.4：`@Idempotent` + `IdempotentAspect`（SpEL key）
- [x] T1.5：`ReplayAutoConfiguration`（`@ConditionalOnMissingBean`）+ AutoConfiguration.imports 注册

### P2 crypto 基础设施（[phases/02](./phases/02-crypto-core.md)）· 已完成

- [x] T2.1：`AESUtil` 新增 `byte[]` CEK + `aad` 的 GCM 加解密重载（AAD 不匹配解密失败，单测覆盖）
- [x] T2.2：`CryptoType.HYBRID` + `@InCryptoHybrid`（并补齐 CryptoSerializer/Deserializer 的 HYBRID case）
- [x] T2.3：`InCryptoProperties.hybrid` 配置项（mode/responseWrap/keyPairs/headers/modeValue/replayNamespace）
- [x] T2.4：`HybridKeyManager`（多 kid 加载/活跃公钥列表/RSA-OAEP-256 解包；不可变密钥快照 + 监听 `RefreshScopeRefreshedEvent` 原子重载 `refresh()`）
- [x] T2.5：`HybridCryptoService`（encrypt/decrypt + AAD + 算法校验）
- [x] T2.6：`CryptoErrorCode` 新增加密类错误码；crypto 依赖 replay 模块（往返测试通过）

### P3 集成与端点（[phases/03](./phases/03-web-integration.md)）· 代码已完成

- [x] T3.1：新增 `HybridCryptoInterceptor`（preHandle 统一解析协议头、`ReplayGuard`、解包 CEK 存 attribute，支持 GET）；`InDecryptRequestBodyAdvice` HYBRID 分支用上下文解密请求体（optional/strict）
- [x] T3.2：`InEncryptResponseBodyAdvice` HYBRID 分支（取 CEK、`responseWrap=DATA_ONLY` 保留 R 仅加密 data、回带响应头）
- [x] T3.3：`HybridPublicKeyController` + `@ConditionalOnProperty` 开关，经 `InCryptoConfiguration` 以 `@Bean` 注册
- [~] T3.4：网关统一路由 `/crypto/public-keys` 与放行白名单——属 Nacos 外部配置（见 phases/03），需在部署侧配置：
  - 认证服务 `ingot.security.oauth2.resource.public-urls` 增加 `/crypto/public-keys`
  - 网关路由将 `/crypto/public-keys` 指向认证服务
  - 验证（V 阶段）：端到端 POST/GET 加解密、篡改/重放被拒、公钥端点匿名可访问

### P4 灰度与交付（[phases/04](./phases/04-rollout-doc.md)）

- [x] T4.1：前端对接说明文档（[frontend-integration.md](./frontend-integration.md)：协议头/报文/AAD/加解密步骤/错误处理/多端库建议）
- [ ] T4.2：示例接入一个敏感接口，`optional` 模式联调（需运行环境）
- [ ] T4.3：灰度切 `strict` 的检查清单与旧模式下线计划
- [ ] T4.4：验收后创建/更新 `specs/current/security/transport-crypto/`

## 验证任务

- [ ] V1：单元测试（replay、AESUtil、HybridCryptoService）通过
- [ ] V2：集成测试（端到端、多 kid、模式切换、篡改/重放、网关放行）通过
- [ ] V3：兼容回归（旧模式与未标记接口）通过

## 完成检查

- [ ] 实现与 DESIGN 一致
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] Current 已更新
- [ ] Change 已记录完成信息并归档

## 已定默认决策

- 公钥端点由框架提供，部署在对外公开的认证服务，网关统一路由；密钥集经配置中心全局共享。
- RSA 密钥长度默认 2048；CEK 为 AES-256；GCM IV 12 字节、tag 128 位（对齐现有 `AESUtil`）。
- `mode` 默认 `optional`，`responseWrap` 默认 `DATA_ONLY`，时间窗默认 ±5 分钟。
- Redis 不可用默认 `fail-close`。
