# Current Specifications

`current` 描述已经上线并验收的系统能力，是理解系统当前行为的首选入口。

新增或更新 current 时：

1. 按 `<domain>/<capability>/` 组织。
2. 根据最终代码和验收结果编写，不直接复制 active change。
3. 只记录当前有效事实，不保留实施过程和临时兼容逻辑。
4. 在对应 archived change 中记录本次更新的 capability 路径。

## 能力基线

- [security / jwk-management](./security/jwk-management/README.md)：JWK 密钥轮换、JWT 签名选择器与多密钥验签。
- [security / transport-crypto](./security/transport-crypto/README.md)：传输层信封加密（HYBRID）与通用防重放。
- [pms / application-authorization](./pms/application-authorization/README.md)：应用中心化授权（应用、菜单、权限归属与租户授权、权限匹配语义）。

