# 阶段 6：旧模型清理

> 状态：发布 A 已完成（含 P6.2 access_mode 单一来源、P6.5 双轨删除）——代码级收尾、legacy/shadow 双轨删除、enable_permission 读写分支移除已完成；仅剩破坏性 DDL（P6.6）推迟，待前置条件满足后独立发布。

## 0. 实施记录（代码级收尾）

本轮在不触碰破坏性 DDL 的前提下完成功能收尾：

- 对象转换统一为 MapStruct（`ApplicationConvert`），更新接口改为按非空字段部分更新（仅需 `id` + 改动字段）。
- 清理无用代码：删除未使用的 `EffectiveAuthorizationService` 冗余匹配分支与未用局部变量/导入、`ApplicationMenuTreeBuilder` 冗余方法与导入，规整 `ApplicationAuthorizationResolver`/`AuthorizationObservationService` 的全限定类型引用。
- 为本次新增类（DTO/VO/枚举/转换器/领域服务/鉴权引擎/审计/配置/Web 入口）及本次改动的领域实体补齐符合 `docs/code-standards/Javadoc.md` 的类级 JavaDoc。
- 编译通过，`authorization.*` 与 `BizMenuUtilsTest` 单测通过。

### 0.1 发布 A 实施记录（决策 D1–D4，见 §9）

- **T6.3（已完成）**：删除 `AppTypeEnum`，应用类型统一为 `OrgTypeEnum`（`app_type` 列值 `0`/`1` 与 `OrgTypeEnum` 一致，DB 兼容）。涉及 `PlatformApp`、`AppCreateDTO`、`AppDetailVO`、`ApplicationResourceServiceImpl`（`resolveOrgType` 退化删除）、`EffectiveAuthorizationService`、`BizPlatformAppServiceImpl`、`BizAppServiceImpl`。
- **T6.1（已完成，列无关化）**：去除对 `platform_app.menu_id/permission_id` 的读取。
  - `AuthorizationDataAuditService`：应用根校验改为「该 app_id 下存在 `source_type=SYSTEM` 且 `pid<=ROOT` 的系统根权限」。
  - `BizPlatformMenuServiceImpl` / `BizPlatformPermissionServiceImpl` 的删除守卫改为按 `app_id + source_type + pid` 识别应用根（不再 `eq menu_id`）；`resolveAppId` 移除 `menu_id` 兜底。
  - 过渡期影响：legacy 应用（`/base` 创建、根菜单权限为 `MENU` 来源）的根菜单不再被该守卫拦截；`/base` 已废弃，按 D3 暂留，可接受。
- **T6.4（占位，无改动）**：按 D2 保留 `getPlatformRole()` 与 `platform_role` 列。

### 0.2 发布 A 实施记录（T6.5 双轨删除，决策 D4）

`APPLICATION` 模式已验证通过，按 D4 一并删除 legacy/shadow 双轨：

- 删除 `LegacyAuthorizationResolver`、`AuthorizationObservationService`、`ShadowComparisonService`（含 `ShadowComparisonServiceTest`）。
- 删除 snapshot/diff：`AuthorizationSnapshotService`、`AuthorizationSnapshotVO`、`AuthorizationDiffVO`。
- 删除模型开关：`AuthorizationProperties`、`AuthorizationConfiguration`、`AuthorizationModelEnum`，以及 `application.yml` 中 `ingot.pms.authorization` 配置块。
- `ApplicationAuthorizationResolver` 改为直接返回菜单树（`resolveMenus(List<String>)`），去除 `AuthorizationOutcome` 中间结构。
- 登录/菜单链路（`BizAuthServiceImpl.getUserMenus`）直调 `ApplicationAuthorizationResolver`；`UsernameIdentityResolver`/`SocialIdentityResolver` 移除 shadow `observeLogin` 调用。
- `DevAuthorizationAuditAPI` 仅保留只读 `audit` 端点，移除 `snapshot`/`compare-user`/`observe-login`。
- **保留** 旧 `/base` 接口（D3）与 `AuthorizationDataAuditService`（已列无关化的只读审计）。
- 编译通过，`authorization.*` 与 `core.*` 单测通过。

### 0.3 发布 A 实施记录（T6.2 access_mode 单一来源）

`access_mode` 成为菜单可见性与访问控制的唯一来源，移除 `enable_permission` 的读写/派生分支：

- `ApplicationMenuTreeBuilder.isMenuVisible`：仅按 `accessMode == OPEN` 判定开放，去除 `resolveAccessMode` 与 `enablePermission` 读取（含 `BooleanUtil` 导入）。
- `BizMenuUtils.filterMenus`：过滤条件改为 `accessMode == OPEN`，不再读 `enablePermission`。
- `ApplicationResourceServiceImpl`：`syncAccessMode` 不再由 `enablePermission` 派生、不再写回 `enablePermission`（未指定时默认 `OPEN`）；`updateMenu` 移除 `enablePermission` 同步块。
- `BizPlatformMenuServiceImpl.syncAccessMode`：移除 `enablePermission` 写回；**保留** 由旧 `/base` 入参 `enablePermission` 译源到 `accessMode`（兼容旧前端 D3）。
- `BizMenuUtilsTest`：改为 `accessMode` 驱动。
- 编译通过，`authorization.*` 与 `core.*` 单测通过。

### 0.4 发布 A 实施记录（enablePermission 字段移除 + roleSource 取舍）

进一步消除冗余字段，统一表示：

- **菜单访问模式**：删除 `PlatformMenu.enablePermission` 与 `MenuTreeNodeVO.enablePermission` Java 字段，`accessMode` 成为唯一表示。旧 `/base` 入参不再读 `enablePermission`（`PlatformMenuAPI` 直接透传 `accessMode`，`BizPlatformMenuServiceImpl.syncAccessMode` 默认 `OPEN`）。物理列 `platform_menu.enable_permission` 仍保留，待 P6.6 删除。
- **角色来源取舍**：`platformRole`(boolean) 与 `roleSource`(enum) 冲突冗余。决策 **保留 `platformRole`、删除 `roleSource`**——理由：`platformRole` 与 `RoleType.getPlatformRole()` 接口契约一致且全链路使用（`BizRoleUtils`/审计/绑定），而 `roleSource` 仅在 `TenantRolePermissionPrivate` 单向双写、`TenantRoleUserPrivate` 从不写入，适配残缺。
  - 删除 `RoleSourceEnum`、两个 `TenantRole*Private` 实体的 `roleSource` 字段、`TenantRolePermissionPrivateServiceImpl` 的双写。
  - `role_source` 列为本次 007 未发布新增列，直接从 `007`、`rollback_007`、`migrations/ingot_core.sql`（含种子数据）中整体移除，零生产影响，无需 P6.6 再删。
- 编译通过，`authorization.*` 与 `core.*` 单测通过。

> **切换闸门状态**：`APPLICATION` 模式已验证，T6.2 与 T6.5 均已落地（双轨/模型开关移除、access_mode 单一来源）。破坏性 DDL（§3 / P6.6）仍待前置条件满足后随发布 B 独立发布。

> 注意：`org_type`、`enable_permission`、双写/双读兼容字段与 `platform_role` 布尔字段对应的**物理列**仍存在，**不可在 P6.6 DDL 发布前删除列**，否则破坏回滚能力。

## 1. 目标

在新模型稳定运行后删除旧字段、旧接口和兼容分支，确保应用中心化模型成为唯一事实来源。

## 2. 前置条件

必须同时满足：

- 新模型稳定运行至少一个完整发布周期。
- 所有租户均已切换。
- 影子差异记录持续为零。
- 没有旧版本服务实例运行。
- 已完成数据库备份和回滚演练。
- 已确认没有外部系统读取待删除字段。

## 3. 清理内容

### 3.1 应用

删除：

```text
platform_app.menu_id
platform_app.permission_id
```

### 3.2 菜单

删除：

```text
platform_menu.org_type
platform_menu.enable_permission
```

将以下字段改为非空：

```text
platform_menu.app_id
platform_menu.access_mode
platform_menu.permission_id
```

### 3.3 权限

删除：

```text
platform_permission.org_type
```

将以下字段改为非空：

```text
platform_permission.app_id
platform_permission.node_type
platform_permission.source_type
```

### 3.4 角色关联

**已调整决策**：保留 `platform_role` 布尔字段作为唯一来源；`role_source` 已整体废弃删除（见 §0.4），本节无需处理。

### 3.5 代码

删除：

- 旧应用根菜单和根权限推导逻辑。
- 普通父权限自动包含后代的逻辑。
- `org_type` 菜单和权限过滤。
- `enable_permission` 分支。
- 双写和双读兼容代码。
- 已废弃的管理接口。
- 旧模型功能开关。

## 4. 数据约束

最终增加或确认：

```text
UNIQUE platform_app(code)
UNIQUE platform_permission(code)
UNIQUE tenant_app_config(tenant_id, app_id)
UNIQUE role permission relation
INDEX platform_menu(app_id, pid, status, sort)
INDEX platform_permission(app_id, pid, status)
```

如数据库支持并启用外键策略，可对应用、菜单和权限增加受控外键；否则必须由领域服务和审计任务持续保证完整性。

## 5. 文档与运维

更新：

- 数据库初始化脚本。
- API 文档。
- 权限编码规范。
- 应用接入指南。
- 租户开通应用操作指南。
- 角色授权审计说明。
- 故障回滚手册。

## 6. 验收标准

- 代码中不再读取已删除字段。
- 数据库不存在 `app_id`、权限来源或角色来源为空的数据。
- 所有权限都能追溯到应用。
- 所有菜单权限都能追溯到菜单。
- 所有租户应用授权都有唯一关系。
- 全量测试、权限安全测试和迁移回归测试通过。
- 全新数据库可以仅通过最新初始化脚本正确启动。
- 从上一正式版本升级可以通过迁移脚本完成。

## 7. 回滚

本阶段属于破坏性清理，不能只通过配置切回旧模型。

回滚依赖：

- 数据库备份恢复。
- 上一版本应用镜像。
- 已验证的反向迁移脚本。

因此阶段 6 必须独立发布，不能与阶段 5 同批上线。

## 8. 独立发布拆解（P6 子阶段）

> 原则：先用「代码不再读 legacy 字段」的可回滚改造铺路（P6.1–P6.5，可与字段共存、可逐步上线），最后一步才执行不可逆 DDL（P6.6，独立发布）。每个子阶段结束都应编译通过、全量回归通过。

### 当前 legacy 依赖面（代码盘点结果）

| legacy 资产 | 仍在读取的位置 |
| --- | --- |
| `platform_app.menu_id` / `permission_id` | `AuthorizationDataAuditService`（根引用校验）、`AuthorizationSnapshotService`（按 menu_id 收集子树）、`BizPlatformAppServiceImpl`（创建要求 menuId）、`BizPlatformMenuServiceImpl` / `BizPlatformPermissionServiceImpl`（删除前置校验） |
| `platform_menu.enable_permission` | ~~授权/可见性读取与派生写回 + 实体/VO 字段~~（P6.2/§0.4 已移除全部 Java 引用）；仅余物理列待 P6.6 删除 |
| `platform_menu.org_type` / `platform_permission.org_type` | `OrgTypeEnum` 在 `BizFilter`、`BizRoleServiceImpl`、`PermissionType`/`MenuType` 接口、`TenantEngine` 等处广泛使用（需改为由 `app.appType` 派生） |
| `platformRole` 布尔（roles + tenant_role_*_private） | **保留为唯一来源**：`RoleType.getPlatformRole()`（接口契约）、`PlatformRole`/`TenantRolePrivate`/`RoleTreeNodeVO`、`BizRoleUtils`、`BizRoleServiceImpl`、`BizUserServiceImpl`、审计；冗余的 `roleSource`/`RoleSourceEnum` 已删除（§0.4） |
| 模型开关 / 双轨 | ~~`AuthorizationProperties.model`、`LegacyAuthorizationResolver`、`AuthorizationObservationService`、`ShadowComparisonService`~~（P6.5 已删除）；旧 `/base` 写转发按 D3 暂留 |

### P6.0 前置闸门

- 满足 §2 全部前置条件；完成数据库备份与回滚演练。
- 冻结 `authorization.model=application` 稳定运行确认零非预期差异。

### P6.1 解除应用根引用（menu_id / permission_id 读路径）

- `AuthorizationSnapshotService`：应用子树改为按 `permission.app_id` + 权限树（`pid`）收集，移除 `menu_id` 路径。
- `AuthorizationDataAuditService`：根引用校验仅基于 `permission_id`（应用根权限），去掉 `menu_id` 缺失项。
- `BizPlatformAppServiceImpl`：去除创建强制 `menuId`；统一走 `ApplicationResourceService.createApp`。
- `BizPlatformMenuServiceImpl` / `BizPlatformPermissionServiceImpl`：删除前置校验由 `app.menu_id==id` 改为 `app_id` 维度。
- 验收：以上代码不再调用 `getMenuId()`；审计/快照对新数据结果不变。

### P6.2 access_mode 单一来源（移除 enable_permission 分支）（已完成）

- `BizMenuUtils`、`ApplicationMenuTreeBuilder`、`ApplicationResourceServiceImpl`：仅读 `access_mode`，停止读写 `enable_permission`。
- `BizPlatformMenuServiceImpl`：移除 `enable_permission` 写回；保留旧 `/base` 入参译源（D3）。
- 更新 `BizMenuUtilsTest` 用 `accessMode` 驱动。
- 验收：菜单可见性与权限开关行为与基线一致；编译 + 单测通过。

### P6.3 类型枚举统一 + org_type 派生化【决策 D1】

- 删除 `AppTypeEnum`，应用类型统一使用 `OrgTypeEnum`（两者存储值同为 `0`/`1`，DB 兼容，无需改列值）。
  - 迁移点：`PlatformApp.appType`、`AppCreateDTO`、`AppDetailVO`、`ApplicationResourceServiceImpl`、`EffectiveAuthorizationService`、`BizPlatformAppServiceImpl`、`BizAppServiceImpl`。
  - `ApplicationResourceServiceImpl.resolveOrgType` 退化为直接返回 `app.appType`（即应用 orgType）。
- 应用内创建的菜单/权限其 `orgType` 由所属应用的 `OrgTypeEnum` 派生，不再独立存储。
- 调整 `BizFilter`、`BizRoleServiceImpl`、`BizPermissionUtils` 等隔离逻辑改用应用维度派生。
- 验收：平台/租户权限隔离不回归；越权安全用例通过。

### P6.4 platformRole 保留 + roleSource 删除【决策 D2，已完成】

- **保留** `getPlatformRole()` 与 `platform_role` 布尔字段作为唯一来源（仍在 `BizRoleUtils`/`BizRoleServiceImpl`/`BizUserServiceImpl`/审计等使用）。
- **删除** 冗余的 `roleSource`/`RoleSourceEnum`：两个 `TenantRole*Private` 实体字段、`TenantRolePermissionPrivateServiceImpl` 双写、`007`/`rollback_007`/`migrations/ingot_core.sql` 中的 `role_source` 列与种子数据（未发布新增列，零生产影响）。
- P6.6 **不**删除 `platform_role` 布尔列。

### P6.5 删除双轨开关【决策 D3/D4】（已完成）

- 移除 `authorization.model` 开关、`LegacyAuthorizationResolver`、`ShadowComparisonService`、`AuthorizationObservationService`（登录/菜单解析直接走 `ApplicationAuthorizationResolver`）。
- `DevAuthorizationAuditAPI` 移除 shadow 依赖端点（compare-user/observe-login）与 snapshot 端点，保留只读 audit；snapshot/diff 服务与 VO 一并退役。
- **保留** 旧 `/base` 应用/菜单/权限接口（暂不删除，过渡期可用）。

### P6.6 破坏性 DDL + 约束（独立发布，最后一步）

- 新增 `databases/migrations/008_application_authorization_cleanup.sql` 与 `rollback_008.sql`：
  - DROP：`platform_app.menu_id/permission_id`、`platform_menu.org_type/enable_permission`、`platform_permission.org_type`。（按 D2，**不**删除 `platform_role` 布尔列）
  - NOT NULL：`platform_menu.app_id/access_mode/permission_id`、`platform_permission.app_id/node_type/source_type`。
  - 唯一/索引：`platform_app(code)`、`platform_permission(code)`、`tenant_app_config(tenant_id,app_id)`、角色关联唯一、`platform_menu(app_id,pid,status,sort)`、`platform_permission(app_id,pid,status)`。
- 同步更新数据库初始化脚本与 §5 各文档。
- 验收：全新库仅靠初始化脚本启动；上一版本可经 008 升级；回滚经 rollback_008 + 备份。

### 顺序与发布边界

- 发布 A：P6.1–P6.5（纯代码，字段仍在，可灰度可回滚）。
- 发布 B：P6.6（DDL，独立发布，依赖发布 A 稳定）。

## 9. 决策结论

- **D1（已决策）**：删除 `AppTypeEnum`，应用类型统一用 `OrgTypeEnum`；应用内菜单/权限 `orgType` 由应用派生，移除 `org_type` 列。
- **D2（已决策，含修订）**：保留 `getPlatformRole()` 与 `platform_role` 布尔列作为角色来源唯一表示；P6.6 不删除该列。**修订**：冗余的 `roleSource`/`RoleSourceEnum` 已整体删除（实体字段 + 双写 + 未发布的 `role_source` 列），见 §0.4。
- **D3（已决策）**：旧 `/base` 接口暂时保留。
- **D4（已决策）**：一并清除 `authorization.model` 开关与 shadow/legacy 双轨。
