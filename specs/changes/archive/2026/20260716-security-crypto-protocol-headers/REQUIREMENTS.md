# Requirements

## 用户场景

- 作为框架维护者，协议头名称应作为 wire protocol 常量维护，与平台 `In-*` 自定义头惯例一致，避免无意义的 YAML 配置。
- 作为前端/SDK 对接方，协议头名称固定为 `In-Crypto-*`，便于文档与实现一致。
- 作为安全/业务开发，标注 `@InCryptoHybridContext` 的端点缺协议头时应明确报错，不降级为明文。

## 业务规则

1. 七个协议头名称写死为 `In-Crypto-Md/Kv/Sk/No/Ts/Al/En`（去掉 `X-` 前缀）。
2. 协议版本由 `HybridProtocolVersion` 枚举表达，当前仅 `H1("h1")`；响应回写 `HybridProtocolVersion.current()`。
3. 删除 `ingot.security.crypto.hybrid.headers.*` 与 `mode-value` 配置项。
4. 标注 `@InCryptoHybridContext` 的端点，缺少必填协议头时返回 `crypto_header_missing`，不降级为明文处理（fail-close）。
5. AAD 格式、算法套件、`activeKid` / `keyPairs` / `replayNamespace` 等行为不变。

## 边界与非目标

- 不实现「无头则跳过加解密」的 fail-open 模式。
- 不在本期强制校验请求 `In-Crypto-Md` 值是否为已知枚举（留待 H2）。
- 不修改 archive 历史变更记录。

## 验收标准

- [x] `HybridHeaders` 定义七个常量，值为 `In-Crypto-*`
- [x] `HybridProtocolVersion.H1.wireValue()` 为 `h1`，`current()` 返回 `H1`
- [x] `InCryptoProperties.Hybrid` 无 `headers`、`modeValue` 字段
- [x] `HybridCryptoInterceptor` 使用常量读写协议头，响应回写 `h1`
- [x] 缺 `In-Crypto-Md` 或缺 `Kv/Sk` 时抛 `CRYPTO_HEADER_MISSING`
- [x] 模块单测通过；`specs/current/security/transport-crypto/` 已更新
