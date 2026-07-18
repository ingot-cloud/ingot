# 传输层信封加密 SPEC

> 记录当前已验收并在线生效的框架能力事实。

## 1. 算法与密文格式

| 项 | 值 |
|---|---|
| CEK | 每请求随机 32 字节 AES-256 密钥 |
| 内容加密 | AES-256-GCM，IV 12 字节，Tag 128 位 |
| 密钥包裹 | RSA-OAEP-256（SHA-256，MGF1 SHA-256） |
| 密文编码 | `base64(IV[12] ‖ ciphertext ‖ Tag[16])` |
| AAD | `"<Md>|<Kv>|<No>|<Ts>"` UTF-8 字节，请求与响应复用 |

## 2. 协议头（固定名称，见 `HybridHeaders`）

| 头 | 方向 | 说明 |
|---|---|---|
| `In-Crypto-Md` | 请求必填 / 响应回带 | 协议版本，当前 `h1`（`HybridProtocolVersion.H1`） |
| `In-Crypto-Kv` | 请求必填 / 响应回带 | 公钥版本 kid |
| `In-Crypto-Sk` | 请求必填 | `base64(RSA-OAEP-256(CEK))` |
| `In-Crypto-No` | 请求必填 | 防重放随机数 |
| `In-Crypto-Ts` | 请求必填 | 毫秒时间戳 |
| `In-Crypto-Al` | 可选 | 缺省 `RSA-OAEP-256` |
| `In-Crypto-En` | 可选 | 缺省 `A256GCM` |

协议头名称不可配置；协议版本由 `HybridProtocolVersion` 枚举维护，服务端响应回写 `HybridProtocolVersion.current()`。

## 3. 报文形态

| 粒度 | 请求 | 响应 |
|---|---|---|
| 整体 POST | `{"<body-key>":"<密文>"}` | `response-wrap=FULL` 时整段密文；`DATA_ONLY` 时 `R{code,message,data:密文}` |
| URL GET | `?<param-key>=<密文>` | 同整体响应规则 |
| 字段级 | 正常 JSON，标注字段值为密文 | 正常 `R` JSON，标注字段值为密文 |

默认 `body-key` / `param-key` 均为 `data`。

## 4. 注解组合（HYBRID 为默认类型）

| 场景 | 控制器注解 | DTO/VO 注解 |
|---|---|---|
| 整体双向 POST | `@InCryptoHybridContext` + `@InDecrypt` + `@InEncrypt` | — |
| 仅解密请求 | `@InCryptoHybridContext` + `@InDecrypt` | — |
| 仅加密响应 | `@InCryptoHybridContext` + `@InEncrypt` | — |
| GET 参数解密 | `@InCryptoHybridContext` + 参数 `@InDecrypt` | — |
| 字段级 | `@InCryptoHybridContext` | `@InDecryptField` / `@InEncryptField` |

约束：

- `@InCryptoHybridContext` 为拦截器唯一触发点，仅标在类或方法上。
- 字段级端点不得再加 `@InDecrypt` / `@InEncrypt`。
- Controller 入参直接接收业务 DTO，Advice 解密后已是明文 JSON。

## 5. 配置（`ingot.crypto`）

```yaml
ingot:
  crypto:
    body-key: data
    param-key: data
    secret-keys: {}          # 仅 AES/RSA 传统模式
    hybrid:
      response-wrap: DATA_ONLY   # DATA_ONLY | FULL
      active-kid: <kid>
      public-key-endpoint-enabled: true
      replay-namespace: crypto
      key-pairs:
        <kid>:
          public-key: <X509 Base64>
          private-key: <PKCS8 Base64>

  replay:
    enabled: true
    window: 5m
    clock-skew: 5m
    key-prefix: "replay:"
    fail-open: false
```

密钥轮换：配置中心更新 `in-security-crypto.yml` 中 `key-pairs` 与 `active-kid`，`HybridKeyManager` 监听 `NacosConfigRefreshEvent` 原子重载。

## 6. 公钥端点

- `GET /crypto/public-keys` → `R<List<{kid, alg, publicKey, active}>>`
- 由 `ingot.crypto.hybrid.public-key-endpoint-enabled` 控制（默认开启）
- 网关须匿名放行并路由至对外认证/BFF 服务

## 7. 处理链路

1. `HybridCryptoInterceptor.preHandle`：校验协议头 → `ReplayGuard` → RSA 解包 CEK → 写 CEK/AAD attribute → 回带响应头 `Md`/`Kv`
2. 请求解密：整体走 Advice / URL 走 ParamResolver / 字段走 Jackson Deserializer
3. 响应加密：整体走 Advice（`response-wrap`）/ 字段走 Jackson Serializer

缺协议头或缺 CEK 上下文：返回 `crypto_header_missing`（明文 `R`），**不降级为明文处理**（fail-close）。

## 8. 错误码

| code | 含义 |
|---|---|
| `crypto_header_missing` | 缺少协议头 |
| `crypto_kid_unknown` | kid 未知或已失效 |
| `crypto_key_unwrap_error` | CEK 解包失败 |
| `crypto_integrity_error` | AAD/密文完整性校验失败 |
| `crypto_alg_unsupported` | 算法不支持 |
| `replay_ts_expired` | 时间戳超窗 |
| `replay_nonce_dup` | nonce 重复 |

## 9. 向后兼容

- 未标注 `@InCryptoHybridContext` 的接口：行为不变。
- 显式 `@InDecrypt(AES|AES_GCM|RSA)` / `@InEncrypt(...)` 的传统模式继续可用。
- 已删除 `ingot.crypto.hybrid.mode`（原 optional/strict）；标注端点统一要求协议头。
- 已删除 `ingot.crypto.hybrid.headers.*` 与 `mode-value`；协议头固定为 `In-Crypto-*`（自 20260716 变更起，替代原 `X-In-Crypto-*`）。

## 10. 关联能力

- 协议头常量：`ingot-security-crypto/.../hybrid/HybridHeaders.java`
- 协议版本枚举：`ingot-security-crypto/.../hybrid/HybridProtocolVersion.java`
- 防重放通用模块：`ingot-security-replay`，提供 `ReplayGuard` 与 `@Idempotent`。
- 字段级 Jackson 异常经 `GlobalExceptionHandlerResolver` 还原 `BizException` 错误码。
