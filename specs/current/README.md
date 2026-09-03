# Current Specifications

`current` 描述已经上线并验收的系统能力，是理解系统当前行为的首选入口。

上游产品意图与路线图见 [docs/requirements/VISION.md](../../docs/requirements/VISION.md) 与 [ROADMAP.md](../../docs/requirements/ROADMAP.md)。

新增或更新 current 时：

1. 按 `<domain>/<capability>/` 组织。
2. 根据最终代码和验收结果编写，不直接复制 active change。
3. 只记录当前有效事实，不保留实施过程和临时兼容逻辑。
4. 在对应 archived change 中记录本次更新的 capability 路径。

## 能力基线

- [security / jwk-management](./security/jwk-management/README.md)：JWK 密钥轮换、JWT 签名选择器与多密钥验签。
- [security / transport-crypto](./security/transport-crypto/README.md)：传输层信封加密（HYBRID）与通用防重放。
- [security / credential-security](./security/credential-security/README.md)：凭证（密码）安全引擎与登录/改密闭环。
- [security / account-protection](./security/account-protection/README.md)：账号登录失败锁定、边沿安全事件、Redis 锁定信号与 BFF/Gateway 短路。
- [security / access-protection](./security/access-protection/README.md)：网关策略执行面、登录失败保护与 remote 弹性。
- [security / challenge-verification](./security/challenge-verification/README.md)：412 + PassToken 挑战验证，登录与敏感接口图形/滑块闭环。
- [security / security-event-center](./security/security-event-center/README.md)：统一安全事件模型、中心入库与跨模块上报接入。
- [security / session-safety](./security/session-safety/README.md)：sid 会话模型、强制下线、安全中心会话管理面与可降级并发策略。
- [gateway / header-conventions](./gateway/header-conventions/README.md)：网关自定义 Header 命名与内部头安全约定。
- [pms / application-authorization](./pms/application-authorization/README.md)：应用中心化授权（应用、菜单、权限归属与租户授权、权限匹配语义；菜单 `view_path` 为页面注册键）。

