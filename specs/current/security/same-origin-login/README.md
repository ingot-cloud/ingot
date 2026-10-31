# 内部后台同源登录

平台与租户各使用一个独立 origin，登录页和管理页同源。两个管理前端直接提供 `/login/`；BFF 在本次认证成功后下发当前应用的正式 Cookie，不再经独立登录站和浏览器 ticket/complete 交接。

2026-10-31 用户确认平台、租户的登录、退出、切换组织和多标签页本地验收通过，本能力作为已验收开发基线收口。该确认不表示生产已经发布。

- [当前规格](./SPEC.md)：入口、接口、状态、Cookie、并发与安全约束。
- [归档变更](../../../changes/archive/2026/20261031-security-same-origin-login/README.md)。
- [权威 API](../../../changes/archive/2026/20261031-security-same-origin-login/API.md)。
- [部署与回滚手册](../../../changes/archive/2026/20261031-security-same-origin-login/DEPLOYMENT.md)。
- [验收记录](../../../changes/archive/2026/20261031-security-same-origin-login/VALIDATION.md)。
- [目标环境发布验收](../../../changes/active/20261031-security-same-origin-release-acceptance/README.md)：客户 CA、真实休眠及生产演练另行记录。

所有者：Gateway 负责可信入口与会话中继，BFF 负责事务与 Cookie，Auth 负责凭证/身份/sid，两个管理宿主负责页面与身份状态。
