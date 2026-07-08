# Phase 04 · 灰度、前端文档与基线

> 状态：in-progress（前端对接文档已产出 [frontend-integration.md](../frontend-integration.md)；灰度联调与 current 基线待验收）

## 目标

完成前端交付文档、灰度接入与收敛，并在验收后更新 current 基线。

## 实现要点

- 前端对接说明（交付前端/大模型实现）：
  - 初始化拉取并缓存 `GET /crypto/public-keys` 的活跃公钥（`crypto_kid_unknown` 时强制刷新）。
  - 每请求：随机 CEK + IV -> AES-256-GCM 加密 body（`base64(iv‖ct‖tag)` 放 `data`）-> RSA-OAEP 包裹 CEK 放 `X-In-Crypto-Sk` -> 设 `Md/Kv/No/Ts` 头 -> 把 `md|kv|no|ts` 作为 AAD -> 内存保留 CEK。
  - 响应：见 `X-In-Crypto-Md` 则用 CEK 解密 `R.data`；`code/message` 明文用于错误判断。
  - 多端库：Web/H5/企业微信 H5 用 WebCrypto；小程序用 `jsencrypt`+`crypto-js`（或 `sm-crypto`）；封装统一 `encrypt/decrypt`。
  - 安全：CEK 仅内存、每请求新生成；继续走 HTTPS。
- 灰度：先在 `optional` 模式接入一个敏感接口联调；稳定后按客户端版本切 `strict`，逐步下线旧固定密钥模式。
- 验收后创建/更新 `specs/current/security/transport-crypto/`（README + SPEC），归档本 change。

## 退出条件

- 前端文档评审通过、示例接口联调成功。
- current 基线已更新，change 记录完成信息并归档。

## 回滚

- 保持 `optional`，前端回退不加密即可；不涉及数据变更。
