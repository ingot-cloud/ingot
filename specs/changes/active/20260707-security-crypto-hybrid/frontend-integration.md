# 信封加密（HYBRID）前端对接说明

> 本文交付前端实现。后端已实现自动解密/加密，前端只需按协议加解密并携带协议头。应用层加密是对 HTTPS 的补充，HTTPS 仍必须启用。

## 1. 总览

- 前端不再保存任何长期对称密钥，仅缓存服务端公钥（可公开）。
- 每次请求随机生成一次性内容密钥 CEK（AES-256-GCM，32 字节），并：
  - 用 CEK 加密请求体；
  - 用服务端公钥 `RSA-OAEP-256` 包裹 CEK 放入请求头；
  - 在内存中保留该 CEK，用于解密本次响应。
- 响应默认保留统一结构 `R`，仅 `data` 字段为密文；`code`/`message` 为明文，用于错误判断。

## 2. 协议头（默认名称，可能被后端重命名，以联调为准）

| 头 | 值 | 说明 |
|---|---|---|
| `X-In-Crypto-Md` | `h1` | 触发信封加密（v1） |
| `X-In-Crypto-Kv` | `<kid>` | 使用的公钥版本，取自公钥端点 |
| `X-In-Crypto-Sk` | `base64(RSA-OAEP-256(CEK))` | 被包裹的 CEK |
| `X-In-Crypto-No` | 随机串 | 防重放随机数，每请求唯一 |
| `X-In-Crypto-Ts` | 毫秒时间戳 | 防重放时间戳 |
| `X-In-Crypto-Al` | 可选 | 缺省即 `RSA-OAEP-256` |
| `X-In-Crypto-En` | 可选 | 缺省即 `A256GCM` |

响应头：
- `X-In-Crypto-Md: h1` 表示响应体已加密。
- `X-In-Crypto-Kv: <activeKid>` 为服务端**当前激活的公钥版本**，前端据此感知密钥轮换（见 §5.3、§6）。

## 3. 密文格式

- 密文统一为 `base64( IV[12字节] ‖ 密文 ‖ GCM Tag[16字节] )`。
- 请求体：`{ "data": "<密文>" }`（字段名默认 `data`）。
- 响应体（默认 DATA_ONLY）：`{ "code": "0", "message": "OK", "data": "<密文>" }`，解密 `data` 得到真实业务数据 JSON。

## 4. AAD（附加认证数据，必须一致）

加解密时必须绑定 AAD，否则完整性校验失败：

```
AAD = "<Md值>|<Kv值>|<No值>|<Ts值>"   // UTF-8 字节；即 "h1|<kid>|<nonce>|<ts>"
```

- 请求加密与响应解密使用**同一 AAD**（前端自己生成并保留 nonce/ts，可复现）。
- 任何一项被篡改都会导致后端返回 `crypto_integrity_error`。

## 5. 流程

### 5.1 初始化

1. 调用 `GET /crypto/public-keys`（匿名可访问），得到：
   ```json
   { "code": "0", "data": [ { "kid": "k-2026-07", "alg": "RSA-OAEP-256", "publicKey": "<X509 Base64>", "active": true } ] }
   ```
2. 缓存 `active=true` 的 `{kid, publicKey}`。当收到 `crypto_kid_unknown` 时，重新拉取并重试。

### 5.2 发送请求

1. 随机生成 32 字节 CEK 与 12 字节 IV。
2. `nonce = 随机串`，`ts = Date.now()`，`kid = 缓存的活跃 kid`。
3. `AAD = "h1|" + kid + "|" + nonce + "|" + ts`。
4. 用 CEK + IV + AAD 对请求体明文做 AES-256-GCM 加密，输出 `base64(IV‖密文‖Tag)`，作为 `{ "data": ... }`。
5. 用公钥 `RSA-OAEP-256` 包裹 CEK，`base64` 后放入 `X-In-Crypto-Sk`。
6. 设置协议头 `Md/Kv/No/Ts`，发送请求；在内存保留 CEK 与 AAD。

### 5.3 处理响应

1. 若响应头含 `X-In-Crypto-Md: h1`：用内存中的 CEK + 同一 AAD 解密 `data`（DATA_ONLY），得到业务数据；`code`/`message` 直接读取。
2. 若无该头（如迁移期或错误响应）：`data` 即明文，按普通 `R` 处理。
3. 读取响应头 `X-In-Crypto-Kv`（服务端当前激活 kid）：若与本地缓存的活跃 kid **不一致**，说明服务端已轮换密钥，应**异步**重新拉取 `GET /crypto/public-keys` 并更新缓存（本次响应仍用当前内存 CEK 正常解密，无需重试）。

### 5.4 密钥轮换（无需轮询）

前端不需要定时轮询公钥。轮换靠以下被动触发点自愈：
- 自然刷新点：App 启动、登录、页面首次加载各拉一次公钥。
- 被动感知：任一加密响应头 `X-In-Crypto-Kv` 与本地缓存不同 → 异步刷新公钥缓存（见 §5.3 第 3 点）。
- 错误兜底：若用了已下线的旧公钥，请求返回 `crypto_kid_unknown` → 重新拉取公钥并重试本次请求。

> 服务端轮换时会保留旧 kid 一段时间（新旧并存），因此感知/切换过程对用户无中断。

## 6. 错误码（明文 `R.code`）

| code | 含义 | 前端处理建议 |
|---|---|---|
| `crypto_header_missing` | 缺少协议头（strict 模式） | 检查是否携带全部必需头 |
| `crypto_kid_unknown` | 公钥版本未知/失效 | 重新拉取公钥并重试 |
| `crypto_key_unwrap_error` | CEK 解包失败 | 检查公钥/包裹算法 |
| `crypto_integrity_error` | 完整性校验失败 | 检查 AAD/密文是否被篡改 |
| `crypto_alg_unsupported` | 算法不支持 | 使用默认套件 |
| `replay_ts_expired` | 时间戳超窗 | 校准客户端时间后重试 |
| `replay_nonce_dup` | 随机数重复/重复提交 | 每请求生成新 nonce |

## 7. 各端库建议

- Web / 移动 H5 / 企业微信 H5：优先 WebCrypto（`crypto.subtle`）：`RSA-OAEP`(SHA-256) 包裹 CEK，`AES-GCM` 加解密（`additionalData` 传 AAD，`tagLength: 128`）。
- 微信小程序：无 WebCrypto，用 `jsencrypt`/`sm-crypto` 系或 `crypto-js`；注意 RSA-OAEP 需 SHA-256 且 MGF1 为 SHA-256，AES-GCM 需支持 AAD 与 128 位 tag。
- 建议封装统一的 `encryptRequest()` / `decryptResponse()`，对业务透明。

## 8. WebCrypto 关键参数

- 包裹：`{ name: "RSA-OAEP", hash: "SHA-256" }`（MGF1 亦为 SHA-256）。
- 内容：`{ name: "AES-GCM", iv, additionalData: AAD, tagLength: 128 }`；密钥 `importKey` 为 256 位。
- 输出拼接：`IV(12) ‖ ciphertextWithTag`（WebCrypto 的 GCM 密文已在末尾附带 16 字节 tag），再 `base64`。
