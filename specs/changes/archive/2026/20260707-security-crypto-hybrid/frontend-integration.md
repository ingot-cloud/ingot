# 信封加密（HYBRID）前端对接说明

> 本文交付前端实现。后端已实现自动解密/加密，前端只需按协议加解密并携带协议头。应用层加密是对 HTTPS 的补充，HTTPS 仍必须启用。

## 1. 总览

- 前端不再保存任何长期对称密钥，仅缓存服务端公钥（可公开）。
- 每次请求随机生成一次性内容密钥 CEK（AES-256-GCM，32 字节），并：
  - 用 CEK 加密请求体；
  - 用服务端公钥 `RSA-OAEP-256` 包裹 CEK 放入请求头；
  - 在内存中保留该 CEK，用于解密本次响应。
- 响应默认保留统一结构 `R`，仅 `data` 字段为密文；`code`/`message` 为明文，用于错误判断。

## 1.1 模式总览（重点）

握手与协议头对所有模式完全一致（都要携带 `Md/Kv/Sk/No/Ts` 并用 RSA-OAEP-256 包裹 CEK），差异只在于"哪部分是密文"。当前共 5 种粒度，请求 2 种、响应 3 种，可自由组合：

请求（前端加密、后端解密）：

| 模式 | 请求体形态 | 前端处理 |
|---|---|---|
| 整体请求体加密 | `{"data":"<密文>"}`，密文为整个业务 JSON | 用 CEK+AAD 加密整段 JSON，放入 `data` |
| URL 参数加密（GET） | `GET ?data=<密文>`，密文为整个业务 JSON | 用 CEK+AAD 加密整段 JSON，URL 编码后放入 query 参数 `data`（`ingot.crypto.param-key` 默认 `data`） |
| 字段级加密 | 正常业务 JSON，仅约定字段的值为 `<密文>` | 只对约定字段各自用 CEK+AAD 加密（每字段独立 IV），其余明文 |

响应（后端加密、前端解密）：

| 模式 | 响应体形态 | 前端处理 | 响应头 |
|---|---|---|---|
| `DATA_ONLY`（默认） | `{"code","message","data":"<密文>"}` | 用请求的 CEK+AAD 解密 `data`；`code/message` 明文 | `Md`,`Kv` |
| `FULL` | 整个响应体为 `<密文>` | 用请求的 CEK+AAD 解密整段，得到 `R` JSON | `Md`,`Kv` |
| 字段级 | 正常 `R` JSON，仅约定字段的值为 `<密文>` | 只对约定字段各自用 CEK+AAD 解密，其余明文 | `Md`,`Kv` |

要点：

- 全部模式的密文格式统一为 `base64(IV[12]‖密文‖Tag[16])`。
- 全部模式的 AAD 统一为握手 AAD（见 §4），请求加密与响应解密复用同一 AAD。
- 字段级"哪些字段是密文"由前后端按接口约定固定（后端在 DTO/VO 字段上加注解），前端按同一份字段清单处理。
- 具体某接口用哪种模式由后端决定，前端以联调约定为准；判断响应是否加密统一看响应头 `Md` 是否为 `h1`。

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

- 密文统一为 `base64( IV[12字节] ‖ 密文 ‖ GCM Tag[16字节] )`（所有模式一致）。
- 请求体（整体）：`{ "data": "<密文>" }`（字段名默认 `data`）。
- 响应体（默认 DATA_ONLY）：`{ "code": "0", "message": "OK", "data": "<密文>" }`，解密 `data` 得到真实业务数据 JSON。
- 字段级：请求/响应体为正常 JSON，仅约定字段的值是上述密文串（见 §5.4）。

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
4. 用 CEK + IV + AAD 对请求体明文做 AES-256-GCM 加密，输出 `base64(IV‖密文‖Tag)`。
   - **POST**：作为 `{ "data": ... }` 请求体。
   - **GET**：作为 query 参数 `data`（需 URL 编码），协议头与 POST 相同。
5. 用公钥 `RSA-OAEP-256` 包裹 CEK，`base64` 后放入 `X-In-Crypto-Sk`。
6. 设置协议头 `Md/Kv/No/Ts`，发送请求；在内存保留 CEK 与 AAD。

### 5.3 处理响应

1. 若响应头含 `X-In-Crypto-Md: h1`：用内存中的 CEK + 同一 AAD 解密 `data`（DATA_ONLY），得到业务数据；`code`/`message` 直接读取。
2. 若无该头（错误响应或未标注加密的接口）：`data` 即明文，按普通 `R` 处理。
3. 读取响应头 `X-In-Crypto-Kv`（服务端当前激活 kid）：若与本地缓存的活跃 kid **不一致**，说明服务端已轮换密钥，应**异步**重新拉取 `GET /crypto/public-keys` 并更新缓存（本次响应仍用当前内存 CEK 正常解密，无需重试）。

### 5.4 字段级加解密

字段级模式下请求/响应体是正常 JSON，只有接口约定的敏感字段的值是密文串。握手与协议头、CEK/AAD 与整体模式完全相同，仅加解密范围不同。

- 请求：按接口约定，仅对需要加密的字段值用 CEK + AAD 做 AES-256-GCM 加密（每个字段各自生成随机 IV），输出 `base64(IV‖密文‖Tag)` 作为该字段的字符串值；未约定的字段保持明文。整个请求体不再包 `{data:...}`，直接发送正常 JSON。
- 响应：响应头含 `Md: h1` 时，按接口约定对相应字段值用请求的 CEK + 同一 AAD 解密；未约定字段直接读取。
- AAD 与整体模式一致（`h1|kid|nonce|ts`），前端保留本次请求生成的 nonce/ts 即可复现。
- 任一密文字段被篡改会导致后端返回 `crypto_integrity_error`（请求方向在反序列化阶段即失败）。

> 字段清单由前后端按接口约定固定。前端建议将"接口 -> 加密字段列表"配置化，封装进统一的请求/响应处理逻辑。

### 5.5 密钥轮换（无需轮询）

前端不需要定时轮询公钥。轮换靠以下被动触发点自愈：
- 自然刷新点：App 启动、登录、页面首次加载各拉一次公钥。
- 被动感知：任一加密响应头 `X-In-Crypto-Kv` 与本地缓存不同 → 异步刷新公钥缓存（见 §5.3 第 3 点）。
- 错误兜底：若用了已下线的旧公钥，请求返回 `crypto_kid_unknown` → 重新拉取公钥并重试本次请求。

> 服务端轮换时会保留旧 kid 一段时间（新旧并存），因此感知/切换过程对用户无中断。

## 6. 错误码（明文 `R.code`）

| code | 含义 | 前端处理建议 |
|---|---|---|
| `crypto_header_missing` | 缺少协议头 | 检查是否携带全部必需头 |
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
