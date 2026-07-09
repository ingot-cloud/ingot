# Requirements

## 用户场景

- 作为多端前端（纯浏览器 Web / 移动 H5 / 企业微信 H5 / 微信小程序 / 未来 APP），在向后端提交敏感请求体、或接收敏感响应数据时，需要对传输内容加密，且**不在前端长期保存任何对称密钥**。
- 作为后端服务，需要对被标记的接口自动完成请求解密与响应加密，且不影响未标记接口与现有 `AES/AES_GCM/RSA` 模式。
- 作为安全/运维，需要集中管理密钥、支持不停机轮换，并能防止请求被篡改或重放。
- 作为业务开发，需要一个通用的防重放/幂等能力（`@Idempotent`），用于下单、支付等重要操作，而不必与加密耦合。

## 业务规则

1. 前端仅持有服务端公钥（可公开），每请求随机生成 32 字节 CEK（AES-256-GCM），用完即弃；CEK 由服务端公钥 `RSA-OAEP-256` 包裹传输。
2. 响应复用请求 CEK（换新随机 IV），默认保留统一 `R` 结构，仅加密 `R.data`，`code`/`message` 保持明文以支持统一错误处理。
2.1. 加解密支持三种粒度：整体加密、`DATA_ONLY`（仅 `R.data`）、字段级（仅注解字段）。字段级：`@InCryptoHybridContext` + `@InEncryptField(HYBRID)`/`@InDecryptField(HYBRID)`；整体：`@InCryptoHybridContext` + `@InDecrypt(HYBRID)`/`@InEncrypt(HYBRID)`。
3. 触发条件：接口标注 `@InCryptoHybridContext`，且请求携带完整协议头（`X-In-Crypto-Md/Kv/Sk/No/Ts`）。缺头返回 `CRYPTO_HEADER_MISSING`。
4. 协议头统一前缀 `X-In-Crypto-`，名称与模式值可通过配置重命名。
5. nonce、ts、kid（及 alg/enc）以明文头传输，但必须绑定进 AES-GCM 的 AAD，防止被篡改绕过防重放。
6. 防重放为独立通用能力：时间戳窗口校验 + nonce 去重（Redis），按 `namespace` 隔离场景；加密、验签、业务幂等共用。
7. 密钥集全局共享（配置中心下发），多 `kid` 并存支持平滑轮换；公钥经统一端点下发，私钥不出服务端。
8. 加密/防重放相关错误一律以明文 `R` 返回，前端可据 `code` 分支处理（如公钥失效自动刷新重试）。

## 边界与非目标

- 应用层加密是对 HTTPS 的纵深防御补充，HTTPS 仍强制，不可替代。
- 本期不实现前端加解密、不纳入国密算法、不实现强幂等"返回上次结果"。
- 仅建议对敏感接口启用，避免全站加密带来的性能与联调成本。
- 依赖 Redis 作为 nonce 存储；Redis 不可用时按配置降级策略处理（见 DESIGN）。

## 验收标准

- [x] 标注 `@InCryptoHybridContext` + `@InDecrypt(HYBRID)` + `@InEncrypt(HYBRID)` 的接口，携带合法协议头与信封报文时，请求可正确解密、响应按 `R` 结构加密，前端用同一 CEK 可解密 `R.data`。
- [x] GET 请求（无 body）也能正确加密响应。
- [x] 篡改 nonce/ts/kid 任一明文头会导致 AAD 校验失败并返回 `CRYPTO_INTEGRITY_ERROR`。
- [x] 重放原始报文（相同 nonce）在窗口内被拒绝，返回 `REPLAY_NONCE_DUPLICATE`；超窗返回 `REPLAY_TIMESTAMP_EXPIRED`。
- [x] 标注 `@InCryptoHybridContext` 的端点缺协议头返回 `CRYPTO_HEADER_MISSING`。
- [x] 多 kid 并存时，按请求头 kid 选对应私钥解密；更新配置可完成轮换而不停机。
- [x] `GET /crypto/public-keys` 经网关可匿名访问，返回活跃与历史公钥列表（含 kid、alg）。
- [x] 既有 `AES/AES_GCM/RSA` 模式与未标记接口行为不受影响。
- [x] 标注 `@InCryptoHybridContext` 且字段含 `@InEncryptField(HYBRID)`/`@InDecryptField(HYBRID)` 的接口，仅对注解字段加解密，其余字段保持明文；篡改任一密文字段返回 `CRYPTO_INTEGRITY_ERROR`。
- [x] `ingot-security-replay` 提供的 `@Idempotent` 在重复 key（TTL 内）时拒绝重复请求。
- [x] 输出可交付前端的对接说明文档，覆盖协议头、报文格式、加解密步骤与错误处理。
