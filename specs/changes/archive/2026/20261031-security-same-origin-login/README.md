# 内部后台同源登录与双入口部署

> 状态：completed
> 创建日期：2026-10-31
> 2026-10-31 用户确认两端登录、退出、切换组织和多标签页本地验收通过，批准归档并提交；不代表生产已发布。

## 元数据与来源

- Change ID：20261031-security-same-origin-login。
- 领域：security；涉及 BFF、Gateway、Auth 接入与部署配置。
- 负责人：jy；目标发布日期：确认实施与发布窗口后确定。
- 来源：本对话的调用链分析、安全评估、最终双入口方案，以及「部署」生产内网环境部署与Cookie隔离讨论。
- 范围最终确认：平台、租户两个应用入口；各自登录与管理同源；支持双域名、纯 IP 双端口；不保留四域名模式。
- 前端配套：[20261031-ingot-login-same-origin](../../../../../../ingot-admin/specs/changes/archive/2026/20261031-ingot-login-same-origin/README.md)。

## 目标

稳定的 /login/ 可以直接访问、收藏和长期停留。点击登录时才准备本次事务；login/select 成功后由当前 origin 直接下发正式会话 Cookie，取消浏览器 ticket 与 complete 交接。平台和租户仍独立认证、独立退出。

## 工件

- [需求与业务边界](./REQUIREMENTS.md)
- [后端设计与安全约束](./DESIGN.md)
- [浏览器接口权威契约](./API.md)
- [部署、切换与回滚](./DEPLOYMENT.md)
- [联合验收场景](./ACCEPTANCE.md)
- [决策与实施任务](./TASKS.md)

## 基线与并行变更

- [SDD 工作流](../../../../README.md)。
- [既有 BFF 契约](../20260912-iam-identity-access-management/BFF-LOGIN.md)：当前四站流程与本 change 的目标不同；本 change 批准实施并验收前，不改写其历史设计。
- [会话安全](../../../../current/security/session-safety/SPEC.md)、[挑战验证](../../../../current/security/challenge-verification/SPEC.md)、[传输加密](../../../../current/security/transport-crypto/SPEC.md)、[凭证安全](../../../../current/security/credential-security/SPEC.md)。
- 制定 Spec 时核对的旧行为：事务与绑定默认 600 秒；CSRF 重签会轮换绑定；complete 校验 adminBindingId；ticket 读取与删除分离。旧行为已由本 change 的同源流程替代，本地验证见 VALIDATION.md。
- 不接管 IAM 主 change 的业务验收、租户工作台或前端外观 change 的剩余验收。复用已完成登录外观，迁移其运行宿主。
- challenge、会话与可信入口 current 已按双入口最终实现同步；既有 IAM 的四站设计只保留历史，不作为运行契约。

## 范围与非目标

包括浏览器接口、事务与会话并发语义、Cookie 命名、可信入口识别、长停留恢复、旧入口迁移、纯 IP HTTPS、前后端联合验收。
不包括对外 OAuth2/OIDC、SSO、四域名兼容、跨域准备/CORS 握手、平台代入租户、扫码/短信登录、业务 RBAC 改造及其他模块重构。BFF 内部 Auth 授权码与 PKCE 保留。

## 规格与实施状态

- 用户已确认完整 Spec，代码与配套配置已实施；本地测试和边界见 [验证记录](./VALIDATION.md)。
- 本地自动化与用户真实联调已验收；目标环境剩余证据已转入独立发布验收 change。
- 接口真相归本 change 的 API.md；前端保留逐字副本及来源摘要。
- current 已按最终实现更新，目录已归档；用户本轮明确授权创建本地提交。


## 实施批准（2026-10-31）

用户明确确认这两份 Spec 并授权前后端共同实施、按验收场景验证，暂不提交代码。需求、设计、接口、安全、部署、任务与异常恢复细化一并获批。

2026-10-31：批准门禁与两侧任务决策已核对，进入 implementing。current 保持既有基线；实施、验证证据逐项记录。

2026-10-31：进入 validating。前后端实现及本地验证完成，部署联调/用户验收未完成，暂不更新 current 或归档。

## 完成与验收记录（2026-10-31）

用户原文：“我按清单测了一轮，平台和租户的登录、退出、切换组织和多标签页都正常了，更新一下验收记录和前后端 Spec，归档相关内容，然后提交代码”。据此确认实际本地联调与归档/提交授权；没有推断客户 CA、真实休眠或生产回滚已完成。

- 更新 current：security/same-origin-login、security/session-safety、security/challenge-verification、gateway/header-conventions。
- 目标环境剩余验收：[发布验收 change](../../../active/20261031-security-same-origin-release-acceptance/README.md)；原 T09 本地联调完成，V03 未覆盖部分定界移交。
- 最终实现与批准设计一致；联调修正包括权威 sid 身份绑定，以及前端重复确认/挂载、并发退出/改密和旧响应隔离。
- 关联实现提交：后端 `afb352cf72c0e09f0099c19c009b45f5d86ab0fd`、前端 `48700ff3f83d135e9296663261492c03acc2b13e`；文档提交由本 README 所在提交追溯。
- 验收细节与已有测试限制见 VALIDATION.md；未接管 IAM 其他业务或独立视觉验收。
