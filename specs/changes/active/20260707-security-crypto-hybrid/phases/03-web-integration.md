# Phase 03 · Web 集成与公钥端点

> 状态：code-completed（代码已完成；网关路由/放行为 Nacos 外部配置，待部署侧落地）

## 目标

打通端到端：请求解密、响应加密（保留 R 结构）、触发协议、防重放绑定与公钥下发。

## 实现要点

- `InDecryptRequestBodyAdvice`（HYBRID 分支）：
  - `supports` 命中 HYBRID 注解；读取 `X-In-Crypto-*` 头。
  - `optional` 无 `Md` 头 -> 放行明文；`strict` 缺头 -> `CRYPTO_HEADER_MISSING`。
  - `ReplayGuard.check("crypto", no, ts)` -> `HybridKeyManager.unwrap(kv, sk)` 得 CEK -> `HybridCryptoService.decrypt(cek, body, aad)`（`aad = md|kv|no|ts`）。
  - CEK 存 `request attribute`（供响应复用）。
- `InEncryptResponseBodyAdvice`（HYBRID 分支）：
  - 从 attribute 取 CEK；无 CEK 时按模式报错或放行。
  - `responseWrap=DATA_ONLY`：仅加密 `R.data`（新随机 IV），保留 `code/message`；`FULL`：整体加密。
  - 回带 `X-In-Crypto-Md`。
- `HybridPublicKeyController`：`GET /crypto/public-keys` 返回活跃/历史公钥；`@ConditionalOnProperty(publicKeyEndpointEnabled)`，经 `InCryptoConfiguration` 以 `@Bean` 注册（框架包不被业务扫描）。
- 触发链路：`HybridCryptoInterceptor.preHandle` 统一完成协议头解析、防重放、CEK 解包与上下文写入（对 GET 同样生效）；请求体/响应体 Advice 只做加解密。

### 网关路由与放行（Nacos 外部配置，非仓库代码）

认证服务（对外公开、承载公钥端点）配置：

```yaml
ingot:
  security:
    oauth2:
      resource:
        public-urls:
          - /crypto/public-keys
```

网关路由（Nacos）：将 `/crypto/public-keys` 指向认证服务（如 `lb://ingot-service-auth`），保持路径不变。

密钥集（Nacos，全局共享，示例）：

```yaml
ingot:
  crypto:
    hybrid:
      mode: optional
      active-kid: k-2026-07
      key-pairs:
        k-2026-07:
          public-key: <X509 Base64 公钥>
          private-key: <PKCS8 Base64 私钥>
```

## 退出条件

- 端到端集成测试：POST/GET 加解密成功、`R` 结构正确；篡改头 -> `CRYPTO_INTEGRITY_ERROR`；重放 -> 重放错误码；`optional`/`strict` 行为正确；公钥端点经网关匿名可访问。
- 旧模式与未标记接口回归通过。

## 回滚

- 移除注解或将 `mode` 关闭即恢复明文；端点由配置开关关闭；网关路由回退。
