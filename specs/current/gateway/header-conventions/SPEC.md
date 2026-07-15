# 网关自定义 Header SPEC

> 记录当前已验收并在线生效的 Header 约定事实。

## 1. 自定义 Header 一览

| HTTP Header | Java 常量 | 写入方 | 用途 |
|---|---|---|---|
| `In-Ca-Sig` | `BFF_DEVICE_FINGERPRINT_HEADER` | 前端 / BFF | 设备指纹；限流 DV、黑白名单 DEVICE、Session 绑定 |
| `In-Inner-Client-Real-IP` | `INNER_CLIENT_REAL_IP` | 网关 `RequestGlobalFilter` | 标准化客户端真实 IP；限流 IP、黑白名单 IP、下游 IP 解析首选 |
| `In-Inner-User-Id` | `INNER_USER_ID` | 网关 `IdentityResolveFilter` | JWT 解析后的 userId；Sentinel USER 维度 |
| `In-Inner-From` | `SECURITY_FROM` | 网关链路 | 请求来源标识 |

定义位置：`com.ingot.framework.commons.constants.HeaderConstants`。

## 2. 内部头安全约束

`GATEWAY_INTERNAL_HEADERS` 包含：

- `In-Inner-From`
- `In-Inner-Client-Real-IP`
- `In-Inner-User-Id`

`RequestGlobalFilter`（order = `HIGHEST`）在网关入口对上述 Header **统一 remove**，后续 Filter 仅写入可信值。外部客户端伪造无效。

## 3. 客户端 IP 解析

### 3.1 网关入口（`PROXY_IP_HEADERS`）

按优先级从代理头解析，不含 `In-Inner-Client-Real-IP`：

1. `X-Forwarded-For`（取最左侧可信 IP）
2. `X-Real-IP`
3. `Proxy-Client-IP`
4. `WL-Proxy-Client-IP`
5. `request.getRemoteAddress()` 回退

解析结果经 `IpUtil.normalize` 后写入 `In-Inner-Client-Real-IP`。

### 3.2 应用层（`REQUEST_SOURCE_IP_HEADERS`）

`WebUtil.getClientIP` 优先读取 `In-Inner-Client-Real-IP`，再按代理头与历史兼容头回退。

## 4. 身份维度与 Sentinel

`IdentityResolveFilter` 构建 `ClientIdentity`：

| 字段 | 来源 Header / 属性 |
|---|---|
| IP | `In-Inner-Client-Real-IP` |
| 设备 | `In-Ca-Sig` |
| userId | JWT attribute → 非空时回填 `In-Inner-User-Id` |
| UA / Referer | 标准 HTTP Header |

Sentinel `GatewayParamFlowItem.fieldName` 与限流/黑白名单 DB 短码对应关系不变：

| 维度 | DB 短码 | Header |
|---|---|---|
| IP | `IP` | `In-Inner-Client-Real-IP` |
| DEVICE | `DV` | `In-Ca-Sig` |
| USER | `UI` | `In-Inner-User-Id` |

## 5. BFF 设备指纹

- 推荐模式：从 `In-Ca-Sig` 读取前端计算的 SHA-256 指纹。
- 降级模式：Header 缺失时使用 `In-Inner-Client-Real-IP`（或 `WebUtil` 回退）+ User-Agent 计算 IP+UA 指纹。
- 网关 `SessionTokenRelayFilter` 与 BFF `BffSessionService` 均遵循相同优先级。

## 6. 已废弃 Header（不再识别）

| 旧 Header | 替代 |
|---|---|
| `X-Ca-Sig` | `In-Ca-Sig` |
| `X-In-Ca-Sig` | `In-Ca-Sig` |
| `X-Client-Real-IP` | `In-Inner-Client-Real-IP` |
| `X-User-Id` | `In-Inner-User-Id` |

## 7. 已废弃 Java 常量名

| 旧常量 | 新常量 |
|---|---|
| `CLIENT_REAL_IP` | `INNER_CLIENT_REAL_IP` |
| `X_USER_ID` | `INNER_USER_ID` |
