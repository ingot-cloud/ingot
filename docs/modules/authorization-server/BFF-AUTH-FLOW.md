# BFF 登录流程与调用链

## 概述

BFF（Backend for Frontend）为内部前端系统提供安全的会话管理，封装了完整的 OAuth2 预授权→授权码→Token 流程。前端不接触任何 OAuth2 参数或 JWT，仅通过 HttpOnly Cookie 维持会话，网关自动将 Cookie Session 转换为 Bearer JWT 转发给下游微服务。

## 架构总览

```
┌──────────┐      Cookie       ┌──────────┐   Session→JWT    ┌──────────────┐   Bearer JWT   ┌──────────────┐
│          │ ◄──────────────── │          │ ───────────────► │              │ ─────────────► │              │
│  前端 SPA │                   │   网关    │                  │  BFF 服务     │                │  Auth 服务    │
│          │ ──────────────── ►│ (Gateway) │ ◄─────────────── │ (ingot-bff)  │ ◄───────────── │ (ingot-auth) │
└──────────┘   只传业务参数      └──────────┘   Feign RPC      └──────────────┘   JSON 响应     └──────────────┘
                                     │
                                     │ Bearer JWT
                                     ▼
                              ┌──────────────┐
                              │  下游微服务    │
                              │ (PMS/Member)  │
                              └──────────────┘
```

**核心原则：**
- 前端只传业务参数（账号密码、租户选择），不传 OAuth2 参数
- 前端不接收 JWT，只接收 HttpOnly Session Cookie（`IN_SESSION`）
- 所有 OAuth2 参数（PKCE、state、redirect_uri）由 BFF 内部生成，无需 client_secret
- 网关对下游透明注入 `Authorization: Bearer JWT`，下游服务权限逻辑零改动

## 涉及的服务与组件

| 组件 | 服务 | 核心类 | 职责 |
|------|------|--------|------|
| BFF API | `ingot-service-bff` | `BffAuthAPI` | REST 接口，接收前端请求 |
| BFF 编排 | `ingot-service-bff` | `BffAuthService` | OAuth2 流程编排 |
| BFF Session | `ingot-service-bff` | `BffSessionService` | Session 生命周期管理 |
| Auth Feign | `ingot-service-bff` | `AuthClient` | 通过 Nacos 服务发现调用 auth |
| 网关 Filter | `ingot-service-gateway` | `SessionTokenRelayFilter` | Session→JWT 转换 + 指纹校验 |
| 来源校验 | `ingot-service-bff` | `BffOriginFilter` | Origin/Referer 白名单 |
| 共享模型 | `ingot-commons` | `BffSession` | Redis 中的会话数据模型 |
| 共享常量 | `ingot-commons` | `CacheConstants` | Redis key 构建、Cookie 名称 |
| 指纹工具 | `ingot-commons` | `FingerprintUtil` | 设备指纹 / IP+UA 降级计算 |

## 登录流程详解

### 第一步：预授权登录（账号密码 → 可选租户列表）

**前端请求：**

```http
POST /bff/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456",
  "vcCode": "可选验证码"
}
```

**内部调用链：**

```
前端                  网关                    BFF                                Auth 服务                Redis
 │                    │                      │                                    │                       │
 │ POST /bff/auth/    │                      │                                    │                       │
 │ login              │                      │                                    │                       │
 │ ──────────────►    │                      │                                    │                       │
 │                    │ 白名单放行             │                                    │                       │
 │                    │ ────────────────►     │                                    │                       │
 │                    │                      │ 1. 生成 PKCE:                       │                       │
 │                    │                      │    code_verifier (随机32字节)        │                       │
 │                    │                      │    code_challenge = SHA256(verifier) │                       │
 │                    │                      │    state (随机8字节)                 │                       │
 │                    │                      │    redirect_uri (从配置读取)         │                       │
 │                    │                      │                                    │                       │
 │                    │                      │ 2. Feign: POST /oauth2/pre_authorize│                       │
 │                    │                      │    (PKCE 公开客户端, 无 Basic Auth)                          │
 │                    │                      │    Query: user_type, pre_grant_type=password,                │
 │                    │                      │           client_id, code_challenge,                         │
 │                    │                      │           response_type=code, redirect_uri,                  │
 │                    │                      │           scope, state                                       │
 │                    │                      │    Body: username, password, _vc_code                        │
 │                    │                      │ ──────────────────────────────────► │                       │
 │                    │                      │                                    │ 验证账号密码            │
 │                    │                      │                                    │ 查询可登录的租户列表     │
 │                    │                      │                                    │ 创建 SecurityContext    │
 │                    │                      │                                    │ ──────────────────►    │
 │                    │                      │                                    │ SET in:security_context│
 │                    │                      │                                    │    :{sessionId}        │
 │                    │                      │                                    │                       │
 │                    │                      │ ◄──────────────────────────────── │                       │
 │                    │                      │    R<{allows: [{id,name,avatar}]}>  │                       │
 │                    │                      │    Set-Cookie: JSESSIONID=xxx       │                       │
 │                    │                      │                                    │                       │
 │                    │                      │ 3. 创建 BFF Session:                │                       │
 │                    │                      │    sessionId = UUID                 │                       │
 │                    │                      │    fingerprint = In-Ca-Sig Header    │                       │
 │                    │                      │    accessToken = code_verifier (暂存)│                      │
 │                    │                      │    refreshToken = state|redirect_uri │                      │
 │                    │                      │ ──────────────────────────────────────────────────────►    │
 │                    │                      │                      SET in:bff_session:{sessionId} 7天TTL  │
 │                    │                      │                                    │                       │
 │                    │ ◄──────────────────  │                                    │                       │
 │ ◄──────────────── │                      │                                    │                       │
 │  R<{allows:[...]}>│                      │                                    │                       │
 │  Set-Cookie:      │                      │                                    │                       │
 │   IN_SESSION=xxx  │                      │                                    │                       │
 │   HttpOnly        │                      │                                    │                       │
 │   SameSite=Lax    │                      │                                    │                       │
```

**响应示例：**

```json
{
  "code": "S0200",
  "data": {
    "allows": [
      { "id": "1", "name": "IngotCloud", "avatar": "url", "main": true },
      { "id": "2", "name": "测试组织", "avatar": "url", "main": false }
    ]
  }
}
```

响应头包含 `Set-Cookie: IN_SESSION=xxx; Path=/; HttpOnly; SameSite=Lax`。

### 第二步：选择租户（完成授权码+Token换取）

**前端请求：**

```http
POST /bff/auth/tenant/select
Content-Type: application/json
Cookie: IN_SESSION=xxx

{
  "tenantId": "1"
}
```

**内部调用链：**

```
前端                  网关                    BFF                                Auth 服务                Redis
 │                    │                      │                                    │                       │
 │ POST /bff/auth/    │                      │                                    │                       │
 │ tenant/select      │                      │                                    │                       │
 │ Cookie: IN_SESSION │                      │                                    │                       │
 │ ──────────────►    │                      │                                    │                       │
 │                    │ 白名单放行             │                                    │                       │
 │                    │ ────────────────►     │                                    │                       │
 │                    │                      │                                    │                       │
 │                    │                      │ 1. 从 Cookie 获取 sessionId         │                       │
 │                    │                      │ ◄──────────────────────────────────────────────────────── │
 │                    │                      │    GET in:bff_session:{sessionId}                          │
 │                    │                      │    → 恢复 code_verifier, state, redirect_uri               │
 │                    │                      │    → 校验客户端指纹                                         │
 │                    │                      │                                    │                       │
 │                    │                      │ 2. Feign: GET /oauth2/authorize     │                       │
 │                    │                      │    Cookie: JSESSIONID=xxx (转发auth session)                │
 │                    │                      │    Query: pre_grant_type=session,                           │
 │                    │                      │           org={tenantId}, client_id,                        │
 │                    │                      │           code_challenge, response_type=code,               │
 │                    │                      │           redirect_uri, scope, state                        │
 │                    │                      │ ──────────────────────────────────► │                       │
 │                    │                      │                                    │ 从 SecurityContext     │
 │                    │                      │                                    │ 恢复预授权认证信息      │
 │                    │                      │                                    │ 校验 tenant 在 allow  │
 │                    │                      │                                    │ list 中               │
 │                    │                      │                                    │ 生成授权码 code        │
 │                    │                      │ ◄──────────────────────────────── │                       │
 │                    │                      │    R<{code:"abc", state:"xyz"}>     │                       │
 │                    │                      │    ⚠️ 因为有 pre_grant_type 参数,     │                       │
 │                    │                      │    auth 直接返回 JSON 而非 302 重定向  │                       │
 │                    │                      │                                    │                       │
 │                    │                      │ 3. Feign: POST /oauth2/token        │                       │
 │                    │                      │    (PKCE 公开客户端, 无 Basic Auth)                          │
 │                    │                      │    Body: code=abc,                  │                       │
 │                    │                      │          grant_type=authorization_code,                      │
 │                    │                      │          code_verifier={pkce_verifier},                      │
 │                    │                      │          client_id, redirect_uri    │                       │
 │                    │                      │ ──────────────────────────────────► │                       │
 │                    │                      │                                    │ 校验 code             │
 │                    │                      │                                    │ 校验 PKCE             │
 │                    │                      │                                    │ 签发 JWT              │
 │                    │                      │ ◄──────────────────────────────── │                       │
 │                    │                      │    R<{accessToken, refreshToken, expiresIn}>                 │
 │                    │                      │                                    │                       │
 │                    │                      │ 4. 更新 BFF Session:                │                       │
 │                    │                      │    accessToken = JWT (真正的)        │                       │
 │                    │                      │    refreshToken = refresh_token      │                       │
 │                    │                      │    expiresAt = now + expiresIn       │                       │
 │                    │                      │    tenantId = "1"                   │                       │
 │                    │                      │ ──────────────────────────────────────────────────────►    │
 │                    │                      │                      SET in:bff_session:{sessionId}          │
 │                    │                      │                                    │                       │
 │                    │ ◄──────────────────  │                                    │                       │
 │ ◄──────────────── │                      │                                    │                       │
 │  R<ok>            │   ⚠️ 注意: 不返回     │                                    │                       │
 │                    │   JWT 给前端！         │                                    │                       │
```

**响应示例：**

```json
{
  "code": "S0200"
}
```

此时登录完成。JWT 存储在 Redis 的 BFF Session 中，前端不可见。

### 第三步：业务请求（网关自动注入 JWT）

**前端请求（任意业务接口）：**

```http
GET /api/pms/user/list
Cookie: IN_SESSION=xxx
```

**网关 SessionTokenRelayFilter 处理流程：**

```
前端                  网关 (SessionTokenRelayFilter)              Redis               下游微服务 (PMS)
 │                    │                                           │                    │
 │ GET /api/pms/...   │                                           │                    │
 │ Cookie: IN_SESSION │                                           │                    │
 │ ──────────────►    │                                           │                    │
 │                    │ 1. 检查 Authorization 头                    │                    │
 │                    │    → 无 Bearer token                       │                    │
 │                    │                                           │                    │
 │                    │ 2. 读 Cookie IN_SESSION                    │                    │
 │                    │    → sessionId = xxx                       │                    │
 │                    │                                           │                    │
 │                    │ 3. 查 Redis                                │                    │
 │                    │ ──────────────────────────────────────►    │                    │
 │                    │    GET in:bff_session:{sessionId}          │                    │
 │                    │ ◄──────────────────────────────────────    │                    │
 │                    │    → 反序列化为 BffSession 对象              │                    │
 │                    │                                           │                    │
 │                    │ 4. 指纹校验                                 │                    │
 │                    │    stored = session.fingerprint             │                    │
 │                    │    current = In-Ca-Sig Header (设备指纹)     │                    │
 │                    │    → 不匹配则返回 401                       │                    │
 │                    │                                           │                    │
 │                    │ 5. 注入 JWT                                 │                    │
 │                    │    Authorization: Bearer {session.accessToken}                   │
 │                    │ ──────────────────────────────────────────────────────────────► │
 │                    │                                           │                    │
 │                    │                                           │          JWT 校验    │
 │                    │                                           │          权限检查    │
 │                    │                                           │          业务处理    │
 │                    │ ◄──────────────────────────────────────────────────────────── │
 │ ◄──────────────── │                                           │                    │
 │    业务响应         │                                           │                    │
```

**关键点：** 下游微服务（PMS、Member 等）的 JWT 校验和权限逻辑**完全不需要改动**，它们看到的就是标准的 `Authorization: Bearer JWT` 请求。

### 第四步：登出

**前端请求：**

```http
DELETE /bff/auth/logout
Cookie: IN_SESSION=xxx
```

**内部调用链：**

```
前端                  BFF                                Auth 服务                Redis
 │                    │                                    │                       │
 │ DELETE /bff/auth/  │                                    │                       │
 │ logout             │                                    │                       │
 │ ──────────────►    │                                    │                       │
 │                    │ 1. 从 Session 取出 accessToken      │                       │
 │                    │ ◄──────────────────────────────────────────────────────── │
 │                    │    GET in:bff_session:{sessionId}                          │
 │                    │                                    │                       │
 │                    │ 2. Feign: DELETE /token              │                       │
 │                    │    Authorization: Bearer {jwt}      │                       │
 │                    │ ──────────────────────────────────► │                       │
 │                    │                                    │ 删除 OAuth2Authorization│
 │                    │                                    │ 撤销 SecurityContext   │
 │                    │                                    │ 移除 OnlineToken       │
 │                    │                                    │ ──────────────────►    │
 │                    │ ◄──────────────────────────────── │                       │
 │                    │                                    │                       │
 │                    │ 3. 清除 BFF Session                 │                       │
 │                    │ ──────────────────────────────────────────────────────►    │
 │                    │    DEL in:bff_session:{sessionId}                          │
 │                    │                                    │                       │
 │ ◄──────────────── │                                    │                       │
 │  R<ok>            │                                    │                       │
 │  Set-Cookie:      │                                    │                       │
 │   IN_SESSION=;    │                                    │                       │
 │   Max-Age=0       │                                    │                       │
```

## Redis 数据结构

### BFF Session

- **Key**: `in:bff_session:{sessionId}`（通过 `CacheConstants.bffSessionKey(sessionId)` 构建）
- **TTL**: 7 天（可配置 `ingot.bff.session-ttl`）
- **Value**: JSON 序列化的 `BffSession` 对象

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "xxxxxx",
  "expiresAt": 1743580800,
  "tenantId": "1",
  "userId": 100,
  "clientId": "ingot-bff",
  "createdAt": 1743494400,
  "fingerprint": "a1b2c3d4e5f6..."
}
```

> **注意**: 在登录第一步（预授权）完成后、选租户之前，`accessToken` 暂存 PKCE `code_verifier`，`refreshToken` 暂存 `state|redirect_uri`。选租户成功后这两个字段会被真正的 JWT 和 refresh_token 覆盖。

### Auth SecurityContext

- **Key**: `in:security_context:{sessionId}`
- **用途**: auth 服务的预授权 session，存储已认证的用户信息和可选租户列表
- **生命周期**: 由 auth 服务管理，BFF 不直接操作

## 安全机制

### 1. Cookie 安全属性

| 属性 | 说明 | 开发环境 | 生产环境 |
|------|------|----------|----------|
| `HttpOnly` | JS 无法读取 Cookie | ✅ 固定开启 | ✅ 固定开启 |
| `Secure` | 仅 HTTPS 传输 | `false` | `true` |
| `SameSite` | 跨站请求策略 | `Lax` | `Lax` 或 `Strict` |
| `Domain` | Cookie 作用域 | 不设置（仅当前域） | `.ingotcloud.top`（子域共享） |
| `Path` | Cookie 路径 | `/` | `/` |

### 2. 客户端设备指纹校验

采用**前端设备指纹**方案（推荐），取代传统的 IP+UA 方式。前端通过浏览器 API 计算一个稳定的设备标识，每次请求通过 `In-Ca-Sig` Header 传递。

**为什么不用 IP+UA？**

| 问题 | 说明 |
|------|------|
| Docker/K8s 部署 | 容器看到的是内网 IP 或代理 IP，非真实客户端 IP |
| 反向代理多层嵌套 | X-Forwarded-For 可被伪造或丢失 |
| 移动端 WiFi↔4G 切换 | IP 频繁变化导致合法用户被踢 |
| 企业多出口 NAT | 同一用户不同请求走不同出口 IP |

**设备指纹如何工作？**

```
前端登录时:
  1. 采集浏览器特征(UA + 语言 + 分辨率 + 时区 + Canvas + WebGL + ...)
  2. SHA-256 → 设备指纹值 (如 "a1b2c3d4...")
  3. 每次请求带上 Header: In-Ca-Sig: a1b2c3d4...

后端:
  1. 登录时: session.fingerprint = request.getHeader("In-Ca-Sig")
  2. 后续: 对比 session.fingerprint == request.getHeader("In-Ca-Sig")
```

**安全性分析：**

| 威胁 | 防护 | 说明 |
|------|------|------|
| MITM（中间人） | HTTPS/TLS | 指纹不防 MITM，这是 HTTPS 的职责 |
| XSS 窃取 Cookie | 设备指纹 | 攻击者的设备特征不同，指纹必然不匹配 |
| Cookie 泄露（日志/共享电脑） | 设备指纹 | 其他设备上无法计算出相同指纹 |
| 伪造 In-Ca-Sig Header | HTTPS | 攻击者看不到 Header 值（HTTPS 加密） |

> **Header 使用 `In-Ca-Sig`（非语义化名称），不暴露用途。**
> 与网关内部头 `In-Inner-Client-Real-IP`（客户端 IP 标准化）区分：前者由前端携带，后者仅网关写入。

校验发生在**两个层级**（纵深防御）：

| 校验层 | 位置 | 保护范围 |
|--------|------|----------|
| **网关** `SessionTokenRelayFilter` | 每次 Session→JWT 转换前 | **所有** 通过 session 访问的请求 |
| **BFF** `BffSessionService.getSession()` | BFF 自身读取 session 时 | BFF 服务的 API |

两层都优先从 `In-Ca-Sig` 读取设备指纹，Header 不存在时降级为 IP+UA 计算（兼容未改造的前端）。

**配置项：**

```yaml
ingot:
  bff:
    security:
      fingerprint-enabled: true
      # device（推荐）: 前端设备指纹 | ip_ua: 服务端 IP+UA（降级方案）
      fingerprint-mode: device
```

### 3. 请求来源校验

`BffOriginFilter` 校验 `Origin` / `Referer` Header 是否在白名单中：

```yaml
ingot:
  bff:
    security:
      allowed-origins:
        - https://admin.ingotcloud.top
        - https://console.ingotcloud.top
```

未配置时跳过校验（适用于开发环境），生产环境**必须配置**。

### 4. PKCE 保护

OAuth 2.1 对授权码模式强制要求 PKCE（Proof Key for Code Exchange），预授权流程最终也是走授权码模式，因此 BFF 全程使用 PKCE：

1. **login** 阶段生成 `code_verifier`（随机 32 字节），计算 `code_challenge = SHA256(verifier)`
2. `code_challenge` 随 `pre_authorize` 请求发送给 auth
3. **selectTenant** 阶段用 `code_verifier` 随 token 请求发送
4. auth 服务校验 `SHA256(code_verifier) == code_challenge`

确保授权码不会被中间人截获后使用。

## 关于 redirect_uri

`redirect_uri` 通过 `BffProperties.redirectUri` 配置，必须与 `oauth2_registered_client` 表中注册的值保持一致。

**为什么不动态推导？**
在 Docker/K8s 环境中，`request.getServerName()` 返回的是容器内部 IP（如 `172.18.0.5`），而非外部域名；开发环境中也可能拿到局域网 IP（如 `192.168.1.130`）而非 `localhost`，与客户端注册的 `redirect_uris` 不匹配会导致 Auth 服务校验失败。因此采用配置方式，由环境变量按环境覆盖。

**重要：这个地址不需要实际的接收端点。** 原因在于 auth 服务的 `AuthorizationCodeAuthenticationSuccessHandler`：

```java
// 如果包含 pre_grant_type，代表是预授权过来的
if (parameters.containsKey(InOAuth2ParameterNames.PRE_GRANT_TYPE)) {
    sendResponse(request, response, authentication);  // 直接返回 JSON
    return;
}
defaultRedirect(request, response, authentication);   // 标准 302 重定向
```

BFF 的请求都携带 `pre_grant_type=session`，所以 auth 服务**直接在 HTTP Body 中返回 JSON**（包含授权码），不发生 302 重定向。`redirect_uri` 仅用于 OAuth2 协议校验（authorize 和 token 请求中必须一致，且在客户端注册的 `redirect_uris` 列表中）。

## 配置参考

### BFF 服务（`ingot-service-bff`）

```yaml
ingot:
  bff:
    client-id: ingot-bff
    # 无需 client-secret（PKCE 公开客户端）
    redirect-uri: ${BFF_REDIRECT_URI:http://localhost:5400/bff/auth/callback}
    scope: system
    user-type: "0"
    session-ttl: 604800  # 7天
    cookie:
      domain: ${BFF_COOKIE_DOMAIN:}           # 生产: .ingotcloud.top
      secure: ${BFF_COOKIE_SECURE:false}       # 生产: true
      same-site: ${BFF_COOKIE_SAME_SITE:Lax}
    security:
      fingerprint-enabled: ${BFF_FINGERPRINT_ENABLED:true}
      fingerprint-mode: ${BFF_FINGERPRINT_MODE:device}
      allowed-origins:                         # 生产必须配置
        - https://admin.ingotcloud.top
```

### 网关路由

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: ingot-service-bff
          uri: lb://ingot-service-bff
          predicates:
            - Path=/bff/**
```

### OAuth2 Client 注册

BFF 专属客户端需要在 `oauth2_registered_client` 表中注册，参见 `databases/bff_client_init.sql`。

| 字段 | 值 | 说明 |
|------|-----|------|
| `client_id` | `ingot-bff` | |
| `client_secret` | `NULL` | PKCE 公开客户端，无需密钥 |
| `client_authentication_methods` | `none,pre_auth` | `none` 用于 token 端点，`pre_auth` 用于预授权 |
| `authorization_grant_types` | `pre_authorization_code,authorization_code` | 公开客户端不签发 refresh_token |
| `redirect_uris` | `http://localhost:5400/bff/auth/callback` | 生产环境需更新 |
| `require-proof-key` | `true` | 强制 PKCE |

## 前端设备指纹集成

前端需要在每次请求中携带 `In-Ca-Sig` Header，值为设备指纹。详见 `docs/modules/authorization-server/DEVICE-FINGERPRINT.md`。

**快速集成（axios 示例）：**

```javascript
import { generateFingerprint } from '@/utils/fingerprint';

// 应用启动时生成一次，缓存在内存中
let cachedFingerprint = null;

axios.interceptors.request.use(async (config) => {
  if (!cachedFingerprint) {
    cachedFingerprint = await generateFingerprint();
  }
  config.headers['In-Ca-Sig'] = cachedFingerprint;
  return config;
});
```

## 接口速查

| 接口 | 方法 | 说明 | 请求参数 | 响应 |
|------|------|------|----------|------|
| `/bff/auth/login` | POST | 账号密码登录 | `{"username","password","vcCode"}` | 可选租户列表 + Set-Cookie |
| `/bff/auth/tenant/select` | POST | 选择租户完成登录 | `{"tenantId"}` | `R<ok>` |
| `/bff/auth/logout` | DELETE | 登出 | 无（Cookie 自动携带） | `R<ok>` + 清除 Cookie |
| `/bff/auth/me` | GET | 当前用户信息 | 无（Cookie 自动携带） | `{tenantId, userId, clientId}` |
