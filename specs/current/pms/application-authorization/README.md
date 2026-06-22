# PMS 应用中心化授权

> 能力域：`pms` / `application-authorization`

## 摘要

PMS 以**应用**为资源归属与租户授权边界的统一授权能力。应用是资源容器，菜单负责导航，权限负责访问能力，菜单与权限均归属于应用。权限匹配支持精确、单段通配 `:*` 与 Ant 子树通配 `:**`，禁止隐式父级包含。

## 边界

- 应用是资源归属和租户授权边界，不退化为任意菜单文件夹。
- 菜单托管 `NAVIGATION` 权限随菜单生命周期自动创建/同步，在权限管理中只读。
- 平台预设角色共享使用，租户只能追加权限，不能削减平台默认权限。
- 前端继续接收菜单树，根菜单先按应用排序，再按菜单排序，不增加应用层节点。

## 所有者

- 模块：`ingot-pms`
- 消费侧：`ingot-auth`（登录权限快照）、前端平台配置与角色授权页面

## 关联模块

| 职责 | 路径 |
|---|---|
| 应用中心化资源服务 | `ingot-pms-provider/.../authorization/resource/ApplicationResourceServiceImpl.java` |
| 权限匹配器 | `ingot-pms-provider/.../authorization/engine/PermissionMatcher.java` |
| 有效权限计算 | `ingot-pms-provider/.../authorization/engine/EffectiveAuthorizationService.java` |
| 菜单树生成 | `ingot-pms-provider/.../authorization/engine/ApplicationMenuTreeBuilder.java` |
| 授权解析入口 | `ingot-pms-provider/.../authorization/ApplicationAuthorizationResolver.java` |
| 应用中心化 API | `ingot-pms-provider/.../web/v1/platform/config/PlatformApplicationAPI.java` |
| 只读全量树 API | `.../web/v1/platform/config/PlatformMenuAPI.java`、`PlatformPermissionAPI.java` |
| 只读审计 | `ingot-pms-provider/.../audit/AuthorizationDataAuditService.java` |

## 文档索引

- [SPEC](./SPEC.md)：当前数据模型、接口、规则与约束
- 来源变更：
  - `specs/changes/archive/2026/20260612-pms-application-authorization/`（发布 A：应用中心化模型）
  - `specs/changes/archive/2026/20260622-pms-authorization-ddl-cleanup/`（发布 B：旧字段破坏性清理）
