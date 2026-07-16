# Design

## 方案摘要

协议头名称从 `InCryptoProperties.Headers` 迁移至 `HybridHeaders` 常量类；协议版本从 `modeValue` 配置迁移至 `HybridProtocolVersion` 枚举。拦截器 fail-close 语义不变。

关键决策：

- D1 头名 `X-In-Crypto-*` → `In-Crypto-*`（breaking，与 `HeaderConstants` 中 `In-*` 惯例对齐）
- D2 删除可配置 `headers.*` 与 `mode-value`
- D3 缺协议头 fail-close，不引入 optional 模式

## 数据模型与接口

### HybridHeaders（常量）

| 常量 | 头名称 |
|---|---|
| `MODE` | `In-Crypto-Md` |
| `KID` | `In-Crypto-Kv` |
| `WRAPPED_KEY` | `In-Crypto-Sk` |
| `NONCE` | `In-Crypto-No` |
| `TIMESTAMP` | `In-Crypto-Ts` |
| `KEY_ALG` | `In-Crypto-Al` |
| `CONTENT_ENC` | `In-Crypto-En` |

### HybridProtocolVersion（枚举）

```java
H1("h1")  // 信封加密 v1
```

- `wireValue()`：线上传输值
- `parse(String)`：解析已知版本，未知返回 empty
- `current()`：服务端默认回写版本，当前为 `H1`

## 数据流与失败处理

与现链路一致：`HybridCryptoInterceptor.preHandle` 读取 `HybridHeaders` → 缺头 `CRYPTO_HEADER_MISSING` → 解包 CEK → 写 attribute → 回写 `In-Crypto-Md: h1` 与 `In-Crypto-Kv: activeKid`。

## 迁移与回滚

- 前端/SDK 须同步将请求/响应头名从 `X-In-Crypto-*` 改为 `In-Crypto-*`
- 回滚：恢复旧版框架与旧头名（需前后端同时回滚）

## 测试策略

- `HybridProtocolVersion` 单元测试
- `HybridCryptoInterceptor`：合法头建立上下文；缺头 fail-close
- 既有 round-trip / AAD 测试无需改动（AAD 值仍为 `h1|...`）
