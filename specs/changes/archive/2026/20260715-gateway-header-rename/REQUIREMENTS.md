# Requirements

## 用户场景

1. **前端集成**：前端在每次请求中携带 `In-Ca-Sig` 设备指纹，BFF 与网关据此完成会话绑定与限流。
2. **网关标准化**：`RequestGlobalFilter` 剥离外部伪造的内部头，写入可信的 `In-Inner-Client-Real-IP`。
3. **已登录限流**：`IdentityResolveFilter` 从 JWT 解析 userId 并回填 `In-Inner-User-Id`，供 Sentinel USER 维度独立计数。

## 业务规则

| Header | 写入方 | 读取方 | 规则 |
|---|---|---|---|
| `In-Ca-Sig` | 前端/BFF | BFF、网关 | 设备指纹；缺失时降级 IP+UA |
| `In-Inner-Client-Real-IP` | 网关 | 下游服务、限流、黑白名单 | 入口剥离后由网关重写 |
| `In-Inner-User-Id` | 网关 | Sentinel USER 维度 | 入口剥离；JWT 解析后回填 |

旧 Header（`X-Ca-Sig`、`X-In-Ca-Sig`、`X-Client-Real-IP`、`X-User-Id`）不再识别。

## 边界与非目标

- 不保留旧 Header 别名兼容。
- 不修改数据库 schema 或 Sentinel 规则短码（IP/DV/UI 不变）。
- CORS 配置由运维按文档自行更新 `allowedHeaders`。

## 验收标准

- [x] `HeaderConstants` 三个值与 `INNER_CLIENT_REAL_IP` / `INNER_USER_ID` 常量名正确
- [x] 业务代码无 `CLIENT_REAL_IP`、`X_USER_ID` 及旧 Header 字面量残留
- [x] `./gradlew` 编译 `ingot-commons`、`ingot-gateway`、`ingot-bff` 通过
- [x] 相关文档（GATEWAY-RATE-LIMIT、DEVICE-FINGERPRINT、BFF-AUTH-FLOW、E2E）已同步
