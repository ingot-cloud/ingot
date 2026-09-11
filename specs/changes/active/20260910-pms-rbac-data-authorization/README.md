# 多租户 RBAC、菜单解耦与数据权限

> 状态：implementing

## 元数据

- Change ID：`20260910-pms-rbac-data-authorization`
- 领域：PMS / Security / MyBatis
- 负责人：jy
- 创建日期：2026-09-10
- 发布安排：一次性同步切换
- 当前阶段：T0–T7 已完成，待 V1 验收与前后端联调；验收前不更新 current。

## 目标

保留 SaaS 应用授权、平台预设角色与租户追加授权，统一菜单、按钮、API 和资源数据范围的授权事实来源。权限编码独立于菜单路由；部门角色按实际绑定部门生效；在线撤权最多 30 秒生效。

## 范围

- 包含 PMS 数据模型及管理接口、安全框架授权解析与缓存、MyBatis 数据权限、示例和一次性迁移。
- 不批量为 PMS 用户、部门、角色管理接口增加行级过滤；独立前端仓库的实现不在本仓库修改范围，但客户端契约联调是发布门禁。
- 不引入角色继承、租户拒绝平台默认权限、复杂布尔权限表达式、租户自定义业务应用设计器。
- 本变更仅有一个对外发布单元。实施批次和阶段验收写入 TASKS，不建立独立发布的 phases 工件。

## 工件

- [需求与验收](./REQUIREMENTS.md)
- [设计与迁移](./DESIGN.md)
- [决策、任务及门禁](./TASKS.md)
- [前端联调接口](./FRONTEND.md)

## 相关基线与事实差异

- [应用授权基线](../../../current/pms/application-authorization/SPEC.md)：其中“无租户应用配置默认不可用”与本次用户明确的默认开放意图冲突；目标行为以本 change 为准，验收前不修改 current。
- [会话安全基线](../../../current/security/session-safety/SPEC.md)：JWT 已是轻量结构，本次改服务端业务权限来源，不重复设计 JWT；移除会话旧字段前必须完成等价补全链路。
- [分层缓存基线](../../../current/framework/layered-cache/SPEC.md)：沿用框架，不手写缓存与降级链。
- 现有 `tenant_role_user_private.dept_id` 已表达部门任职，问题在绑定去重和授权执行链未完整保留上下文。
- 现有菜单解析与登录授权生成走不同聚合入口，需收敛；本 change 不把既有文档中的“已统一”当作实施完成证明。

## 审批及完成记录

- 审批：2026-09-10 用户确认开始实施，状态转为 implementing。
- 完成日期、提交/PR、最终差异：验收时填写。
- 计划更新的 current：`pms/application-authorization`、`security/session-safety`，新增 `pms/data-authorization` 描述业务数据范围与框架接入契约。
- 归档：验收、基线更新完成后移入 archive；取消或替代也保留工件并记录原因。
