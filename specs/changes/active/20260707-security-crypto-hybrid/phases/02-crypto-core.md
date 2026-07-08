# Phase 02 · crypto 基础设施

> 状态：completed（编译 + 往返/篡改/未知 kid 单测通过）

## 目标

在不改动 Web 触发链路的前提下，补齐信封加密所需的类型、算法工具、密钥管理、加解密服务、配置与错误码。

## 实现要点

- `AESUtil`：新增 `encryptGCM(byte[] plain, byte[] key, byte[] aad)` 与 `decryptGCM(byte[] combined, byte[] key, byte[] aad)` 重载；沿用 `base64(iv[12]‖ct‖tag[16])`。原 `String` key 重载保留。
- `CryptoType.HYBRID("hybrid","HYBRID")`；`@InCryptoHybrid` 组合 `@InDecrypt(HYBRID)+@InEncrypt(HYBRID)`。
- `InCryptoProperties`：新增内嵌 `Hybrid`（`mode`、`responseWrap`、`activeKid`、`Map<String,KeyPair> keyPairs`、`Headers headers`、`modeValue`、`publicKeyEndpointEnabled`）。
- `HybridKeyManager`：启动加载 `keyPairs`（Base64 -> `PublicKey/PrivateKey`，用 `RSAUtil`）；`publicKeyInfos()`；`unwrapCek(kid, wrapped)`（RSA-OAEP-256）；密钥集为不可变快照，监听 `RefreshScopeRefreshedEvent` 调用 `refresh()` 原子重载。
- `HybridCryptoService`：`unwrapCek(kid, wrapped)`、`encrypt(byte[] cek, byte[] plain, byte[] aad)`、`decrypt(byte[] cek, byte[] blob, byte[] aad)`；算法由 `alg/enc` 决定，本期固定默认套件，非法则 `CRYPTO_ALG_UNSUPPORTED`。
- `CryptoErrorCode` 新增：`CRYPTO_HEADER_MISSING`、`CRYPTO_KID_UNKNOWN`、`CRYPTO_KEY_UNWRAP_ERROR`、`CRYPTO_INTEGRITY_ERROR`、`CRYPTO_ALG_UNSUPPORTED`（重放错误用 replay 模块的错误码）。
- crypto `pom.xml` 依赖 `ingot-security-replay`。

## 退出条件

- 全部类编译通过。
- 单测：AAD 匹配可解密、AAD 篡改解密失败；RSA 包裹/解包往返正确；多 kid 选择正确。

## 回滚

- 新增类型与服务未被 Advice 引用，删除新增类/配置即可，不影响既有模式。
