# Design

## 方案摘要

统一自定义 Header 命名：

| Java 常量 | 新值 |
|---|---|
| `BFF_DEVICE_FINGERPRINT_HEADER` | `In-Ca-Sig` |
| `INNER_CLIENT_REAL_IP`（原 `CLIENT_REAL_IP`） | `In-Inner-Client-Real-IP` |
| `INNER_USER_ID`（原 `X_USER_ID`） | `In-Inner-User-Id` |

与 `SECURITY_FROM = "In-Inner-From"` 形成 `In-Inner-*` 内部头族；设备指纹使用 `In-Ca-Sig` 对外约定。

## 数据模型与接口

- **破坏性变更**：HTTP Header 名称变更，无 API 路径或 JSON 字段变化。
- **常量重命名**：`CLIENT_REAL_IP` → `INNER_CLIENT_REAL_IP`，`X_USER_ID` → `INNER_USER_ID`。
- **数组更新**：`REQUEST_SOURCE_IP_HEADERS`、`GATEWAY_INTERNAL_HEADERS` 引用新常量名。

## 数据流与失败处理

```
浏览器 --In-Ca-Sig--> Gateway(RequestGlobalFilter 剥离 In-Inner-*)
                    --> 写入 In-Inner-Client-Real-IP
                    --> IdentityResolveFilter 回填 In-Inner-User-Id
                    --> BFF / 微服务
```

- 外部伪造 `In-Inner-*` 在入口被 `RequestGlobalFilter` 移除。
- 缺失 `In-Ca-Sig` 时沿用现有 IP+UA 降级逻辑，行为不变。

## 迁移与回滚

- **上线顺序**：后端与文档同步发布；前端须在同一版本窗口改发 `In-Ca-Sig`。
- **回滚**：恢复 `HeaderConstants` 旧值并 redeploy；前端同步回退 Header 名。

## 测试策略

- Gradle 编译验证
- `rg` 残留检查排除归档 spec
- 手动/E2E：带 `In-Ca-Sig` 请求、网关 IP 标准化、USER 维度限流
