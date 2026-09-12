# PMS 应用中心化授权

> 能力域：`pms` / `application-authorization`

## 摘要

PMS 以**应用**为资源归属与租户授权边界。应用是资源容器，菜单负责导航，权限负责访问能力，三者均归属于应用。权限编码独立于菜单路由；菜单只保存可见性关联，不托管权限生命周期。权限匹配支持精确、单段通配 `:*` 与 Ant 子树通配 `:**`，创建接口只接受 `:**`。无租户覆盖时默认开放，可按应用关闭为申请制。

业务数据范围、授权快照与行过滤见 [data-authorization](../data-authorization/README.md)。

## 边界

- 应用是资源归属和租户授权边界，不退化为任意菜单文件夹。
- 菜单不再托管 `NAVIGATION` 权限；受保护页面通过 `platform_menu_permission` 关联已有 ACTION，匹配 ANY/ALL。
- 平台预设角色共享使用，租户只能追加权限，不能削减平台默认权限。
- 前端继续接收菜单树，根菜单先按应用排序，再按菜单排序，不增加应用层节点；用户菜单与配置树均不返回 Button。
- 登录会话只写入角色码，不把业务权限码写入 JWT / `OnlineToken`。

## 所有者

- 模块：`ingot-pms`
- 消费侧：`ingot-auth`（登录角色码）、资源服务（授权快照过滤器）、前端平台配置与角色授权页面

## 关联模块

| 职责 | 路径 |
|---|---|
| 应用中心化资源服务 | `ingot-pms-provider/.../authorization/resource/ApplicationResourceServiceImpl.java` |
| 权限匹配器 | `ingot-pms-provider/.../authorization/engine/PermissionMatcher.java` |
| 有效权限计算 | `ingot-pms-provider/.../authorization/engine/EffectiveAuthorizationService.java` |
| 菜单树生成 | `ingot-pms-provider/.../authorization/engine/ApplicationMenuTreeBuilder.java` |
| 菜单解析入口 | `ingot-pms-provider/.../authorization/ApplicationAuthorizationResolver.java` |
| 应用中心化 API | `ingot-pms-provider/.../web/v1/platform/config/PlatformApplicationAPI.java` |
| 当前用户权限/菜单 | `ingot-pms-provider/.../web/v1/auth/AuthUserAPI.java` |
| 只读全量树 API | `.../web/v1/platform/config/PlatformMenuAPI.java`、`PlatformPermissionAPI.java` |
| 登录角色码 | `ingot-pms-provider/.../identity/IdentityUtil.java` |
| 只读审计 | `ingot-pms-provider/.../audit/AuthorizationDataAuditService.java` |

## 文档索引

- [SPEC](./SPEC.md)：当前数据模型、接口、规则与约束
- 关联能力：[data-authorization](../data-authorization/README.md)
- 前端联调契约（归档）：`specs/changes/archive/2026/20260910-pms-rbac-data-authorization/FRONTEND.md`
- 来源变更：
  - `specs/changes/archive/2026/20260612-pms-application-authorization/`（发布 A：应用中心化模型）
  - `specs/changes/archive/2026/20260622-pms-authorization-ddl-cleanup/`（发布 B：旧字段破坏性清理）
  - `specs/changes/archive/2026/20260903-pms-menu-view-path/`（`view_path` 为页面注册键，下线 `customViewPath`）
  - `specs/changes/archive/2026/20260910-pms-rbac-data-authorization/`（菜单解耦、默认开放、权限仅 GROUP/ACTION）
