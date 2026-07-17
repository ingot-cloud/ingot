# 网关自定义 Header 约定

> 能力域：`gateway` / `header-conventions`

## 摘要

平台自定义 HTTP Header 统一采用 `In-*` 命名体系：前端/BFF 设备指纹使用 `In-Ca-Sig`；网关内部可信头使用 `In-Inner-*` 前缀（与 `In-Inner-From` 对齐）。常量定义在 `HeaderConstants`，网关入口由 `RequestGlobalFilter` 剥离外部伪造的内部头后按需重写。

## 边界

- 旧 Header（`X-Ca-Sig`、`X-In-Ca-Sig`、`X-Client-Real-IP`、`X-User-Id`）不再识别，无向后兼容别名。
- `In-Inner-*` 仅允许网关链路写入；外部传入在入口被移除。
- 设备指纹 Header 缺失时，BFF 与网关降级为 IP+UA 计算（行为不变）。
- 前端须同步改发 `In-Ca-Sig`，与后端同版本窗口发布。

## 所有者

- 常量：`ingot-framework/ingot-commons/.../HeaderConstants.java`
- 网关写入：`ingot-service/ingot-gateway/.../RequestGlobalFilter`、`IdentityResolveFilter`
- BFF 读取：`ingot-service/ingot-bff/.../BffSessionService`

## 关联模块

| 职责 | 路径 |
|---|---|
| Header 常量 | `ingot-commons/.../HeaderConstants.java` |
| 入口剥离与 IP 标准化 | `ingot-gateway/.../RequestGlobalFilter.java` |
| 身份聚合与 userId 回填 | `ingot-gateway/.../IdentityResolveFilter.java` |
| Sentinel 维度 Header 绑定 | `ingot-gateway/.../SentinelGatewayConfiguration.java` |
| 限流/黑白名单维度说明 | `ingot-gateway-rule-client/.../RateLimitDimension.java`、`IpKeyType.java` |
| 客户端 IP 工具 | `ingot-commons/.../WebUtil.java`、`ClientIpResolver.java` |

## 文档索引

- [SPEC](./SPEC.md)：Header 名称、写入方、Java 常量与数据流
- 网关限流：[GATEWAY-RATE-LIMIT.md](../../../../docs/modules/security-center/GATEWAY-RATE-LIMIT.md)
- BFF 指纹：[DEVICE-FINGERPRINT.md](../../../../docs/modules/authorization-server/DEVICE-FINGERPRINT.md)、[BFF-AUTH-FLOW.md](../../../../docs/modules/authorization-server/BFF-AUTH-FLOW.md)
- 来源变更：`specs/changes/archive/2026/20260715-gateway-header-rename/`
