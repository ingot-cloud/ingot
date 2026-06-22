# 应用中心化授权目标架构

> 工件类型：目标设计。

## 1. 目标

将现有以根菜单和根权限隐式表达应用范围的模型，调整为明确的应用资源域：

```text
应用
├── 菜单树
└── 权限树
```

应用负责资源归属和租户授权，菜单负责导航，权限负责访问能力。

## 2. 领域模型

### 2.1 应用

`platform_app` 是平台定义的资源容器。

核心字段：

| 字段 | 说明 |
|---|---|
| `id` | 应用 ID |
| `code` | 稳定且唯一的应用编码，同时作为权限命名空间 |
| `name` | 应用名称 |
| `app_type` | `PLATFORM` 或 `TENANT` |
| `icon` | 应用图标 |
| `intro` | 应用说明 |
| `sort` | 应用全局排序 |
| `status` | 应用状态 |

约束：

- `code` 创建后不可通过普通更新接口修改。
- `PLATFORM` 应用只用于平台后台。
- `TENANT` 应用可以授权给租户。
- 一个应用可以拥有多个根菜单。
- 应用本身不作为菜单节点返回。

### 2.2 菜单

`platform_menu` 保存前端路由和导航配置。

新增或调整字段：

| 字段 | 说明 |
|---|---|
| `app_id` | 非空，所属应用 |
| `access_mode` | `OPEN` 或 `PERMISSION` |
| `permission_id` | 菜单自动权限 ID，持续保留 |

移除字段：

- `org_type`
- `enable_permission`

约束：

- 根菜单的 `app_id` 由应用上下文填充。
- 子菜单继承父菜单的 `app_id`。
- 子菜单不能跨应用移动。
- 菜单所属平台端或租户端由应用的 `app_type` 决定。
- `OPEN` 只跳过角色权限检查，不跳过应用状态和租户应用授权检查。

### 2.3 权限

`platform_permission` 保存应用内权限树。

核心字段：

| 字段 | 说明 |
|---|---|
| `app_id` | 非空，所属应用 |
| `pid` | 父权限 ID |
| `code` | 权限编码 |
| `node_type` | `GROUP`、`NAVIGATION`、`ACTION` |
| `source_type` | `SYSTEM`、`MENU`、`MANUAL` |
| `source_id` | 来源菜单等资源 ID |
| `managed` | 是否由系统托管 |
| `status` | 权限状态 |

权限树示例：

```text
contacts:*
├── contacts:user:*
│   ├── contacts:user:access
│   ├── contacts:user:create
│   ├── contacts:user:update
│   └── contacts:user:delete
└── contacts:dept:*
    ├── contacts:dept:access
    └── contacts:dept:update
```

约束：

- 应用创建时自动创建 `app_code:*` 根权限。
- `GROUP` 权限必须以 `:*` 结尾。
- `NAVIGATION` 和 `ACTION` 必须是精确权限，不能以 `:*` 结尾。
- 子权限编码必须位于父权限命名空间中。
- `MENU` 来源权限由菜单模块托管，在权限管理中只读。
- 前端按钮和后端 API 使用同一个 `ACTION` 权限。

## 3. 菜单权限生命周期

每个菜单始终关联一条托管的 `NAVIGATION` 权限。

| 菜单操作 | 权限行为 |
|---|---|
| 创建菜单 | 同事务创建菜单权限 |
| 修改名称 | 同步权限名称 |
| 修改父级 | 同步权限父级 |
| 修改路由 | 不自动修改权限编码 |
| 切换 `access_mode` | 不删除权限和角色绑定 |
| 删除菜单 | 无子菜单、无子权限时删除托管权限 |

权限编码可以在创建菜单时根据路由生成建议值，但创建后必须稳定。编码变更需要独立迁移接口、引用检查和审计记录。

## 4. 权限匹配语义

精确权限只匹配自身：

```text
contacts:user:access
```

显式通配权限匹配自身命名空间内的所有后代：

```text
contacts:*       → contacts 下全部权限
contacts:user:*  → contacts:user 下全部权限
```

禁止继续使用“普通父权限自动包含全部子权限”的隐式规则。

通配授权是动态授权。应用新增权限后，已拥有对应 `:*` 的角色自动获得新权限。因此所有通配授权操作必须写入审计日志，并在管理界面明确提示其动态范围。

## 5. 角色与租户授权

### 5.1 租户应用授权

`tenant_app_config` 演进为明确的租户应用授权：

| 字段 | 说明 |
|---|---|
| `tenant_id` | 租户 ID |
| `app_id` | `TENANT` 应用 ID |
| `enabled` | 是否启用 |
| `source` | 套餐、人工或系统初始化 |
| `valid_from` | 生效时间 |
| `valid_until` | 失效时间 |

未关联应用默认不可用。

### 5.2 平台预设角色

`platform_role` 中面向租户的角色继续作为共享预设角色，不复制到每个租户。

有效权限：

```text
platform_role_permission
∪ tenant_role_permission_private
```

租户不能修改或取消平台默认权限，只能追加或撤销自己追加的权限。

### 5.3 租户自定义角色

`tenant_role_private` 继续保存租户自定义角色，其权限全部来自租户私有配置，并且不能超过租户已授权应用范围。

角色可绑定：

- 应用根权限，例如 `contacts:*`。
- 模块通配权限，例如 `contacts:user:*`。
- 精确权限，例如 `contacts:user:create`。

## 6. 有效权限计算

```text
用户角色权限并集
→ 合并平台预设权限与租户追加权限
→ 应用显式通配和精确权限匹配
→ 过滤禁用权限
→ 过滤禁用应用
→ 过滤租户未授权或已过期应用
```

租户管理员不保存全部叶子权限，动态拥有当前租户全部有效应用的应用根权限。

## 7. 菜单生成与排序

前端继续接收原有菜单树结构，不增加应用层节点。

根菜单顺序：

```text
app.sort ASC
→ menu.sort ASC
→ menu.id ASC
```

同一父菜单下的子菜单顺序：

```text
menu.sort ASC
→ menu.id ASC
```

构建算法：

1. 获取当前主体可访问的应用并按应用排序。
2. 按应用过滤有效菜单。
3. 根据 `access_mode` 和有效权限过滤菜单。
4. 补齐可见叶子菜单所需的祖先目录。
5. 在应用内部构建并递归排序菜单树。
6. 按应用顺序拼接各应用的根菜单。

不能只依赖数据库 `ORDER BY app.sort, menu.sort` 构建完整树，因为子节点需要按各自父节点递归排序。

## 8. 缓存与失效

至少需要以下版本或失效维度：

- 应用资源版本：应用、菜单或权限发生变化。
- 租户应用授权版本：租户开通、停用或续期应用。
- 角色授权版本：平台角色权限或租户私有权限变化。
- 用户角色版本：用户角色关联变化。

菜单和有效权限缓存键必须包含租户和用户上下文。所有写操作应在事务提交后触发缓存失效。

## 9. 企业级适用性评估

该模型适用于企业级 SaaS，原因如下：

- 应用授权与角色授权分层，支持套餐、租户开通和内部 RBAC。
- 应用是稳定边界，菜单和权限可以独立演进。
- 通配权限支持管理员类角色，新权限无需批量补数据。
- 精确权限支持最小权限原则。
- 预设角色共享，避免租户级角色副本膨胀。
- 菜单结构与应用管理解耦，不强迫前端改变导航形态。

主要风险及约束：

- 应用不能退化为任意菜单文件夹。
- 通配权限会动态扩大，必须审计。
- 应用编码和权限编码属于稳定外部契约。
- 跨应用依赖必须通过业务服务协作，不能通过权限树交叉挂载表达。

## 10. 当前基线（改造前）

以下描述当前线上事实，供迁移对照。详细 current 基线将在验收后写入 `specs/current/pms/application-authorization/`。

### 10.1 数据模型

| 表 | 当前关键字段 | 当前语义 |
|---|---|---|
| `platform_app` | `menu_id`、`permission_id` | 应用与单个根菜单、根权限绑定 |
| `platform_menu` | `org_type`、`enable_permission`、`permission_id` | 通过 `org_type` 区分平台/租户菜单；`enable_permission=false` 时跳过权限检查 |
| `platform_permission` | `org_type`、`pid`、`code` | 通过 `org_type` 区分平台/租户权限；父权限绑定后隐式包含后代 |
| `tenant_app_config` | `tenant_id`、`app_id`、`enabled` | 租户应用私有开关；无配置时继承应用全局 `status` |
| `platform_role` / `tenant_role_private` | `platform_role` 布尔标记 | 区分平台预设角色与租户自定义角色 |

### 10.2 当前授权链路

```text
登录
→ IdentityUtil.getScopes()
→ BizUserService.getUserRoles()
→ BizRoleService.getRolesPermissions()（父权限隐式展开子权限）
→ BizAppService.getDisabledApps()（按 app.permission_id 前缀过滤）
→ 菜单：BizMenuUtils.filterMenus()（enable_permission + 权限 ID 匹配）
```

### 10.3 当前主要代码位置

| 职责 | 模块路径 |
|---|---|
| 菜单过滤与权限编码生成 | `ingot-pms-provider/.../core/BizMenuUtils.java` |
| 权限树与应用过滤 | `ingot-pms-provider/.../core/BizPermissionUtils.java` |
| 租户应用启停 | `ingot-pms-provider/.../service/biz/impl/BizAppServiceImpl.java` |
| 菜单 CRUD 与权限联动 | `ingot-pms-provider/.../service/biz/impl/BizPlatformMenuServiceImpl.java` |
| 登录权限快照 | `ingot-pms-provider/.../identity/IdentityUtil.java` |
| 应用管理 API | `ingot-pms-provider/.../web/v1/platform/base/PlatformAppAPI.java` |

### 10.4 已知问题

- 应用范围通过根权限 ID 和前缀匹配表达，跨模块权限边界不清晰。
- 父权限绑定语义不明确（精确 vs 通配），新增子权限时可能意外扩权或漏权。
- 菜单排序仅按 `menu.sort`，无法按应用分组排序。
- `org_type` 与 `app_type` 语义重叠，平台/租户隔离规则分散在多处。

## 11. 影响范围

### 11.1 数据库

- 迁移脚本目录：`databases/migrations/`
- 初始化脚本：`databases/ingot_core.sql`（阶段 6 同步更新）

### 11.2 后端服务

| 服务 | 变更类型 |
|---|---|
| `ingot-pms` | 核心改造：领域模型、鉴权引擎、菜单生成、管理 API |
| `ingot-auth` | 消费侧：登录权限快照、令牌 scope 格式保持兼容 |

### 11.3 前端

| 页面 | 变更类型 |
|---|---|
| 平台应用/菜单/权限管理 | 应用中心化入口，权限树展示调整 |
| 租户角色授权 | 平台默认权限锁定、应用范围过滤、通配提示 |
| 运行时菜单 | 协议兼容；可选使用 `appId`/`appCode` 调试 |

### 11.4 配置项

```text
authorization.model = legacy | shadow | application
```

- 阶段 0 接入，默认 `legacy`。
- 阶段 3 起启用 `shadow` 做差异观测。
- 阶段 5 按开关逐步切到 `application`。

## 12. 数据流与失败处理

### 12.1 菜单创建

```text
POST /platform/apps/{appId}/menus
→ 校验 appId 来自 URL 上下文
→ 校验父菜单 app_id 一致
→ 事务：保存菜单 → 创建 NAVIGATION 托管权限 → 回写 permission_id
→ 提交后发布缓存失效事件
```

失败处理：

- 父菜单跨应用：拒绝，返回业务错误，不部分写入。
- 权限编码冲突：拒绝，同事务回滚。
- 缓存失效失败：记录告警，依赖 TTL 兜底；不阻塞主事务。

### 12.2 有效权限计算

```text
输入 userId + tenantId
→ 加载用户角色（role_source + role_id）
→ 合并 platform_role_permission 与 tenant_role_permission_private
→ 分离 EXACT 与 SUBTREE（:*）授权
→ 过滤禁用权限 / 禁用应用 / 未授权或过期租户应用
→ 输出 EffectiveAuthorization
```

失败处理：

- 角色引用已删除权限：审计告警，计算时忽略该绑定。
- 租户应用授权数据缺失（切换期）：按阶段策略回退到 legacy 或 shadow 对比。

### 12.3 菜单树生成

```text
输入 userId + tenantId + EffectiveAuthorization
→ 获取可访问应用列表（已排序）
→ 批量加载应用下有效菜单
→ 按 access_mode 和权限过滤
→ 补齐祖先目录
→ 分应用构建树并递归排序
→ 按应用顺序拼接根节点
```

失败处理：

- 祖先菜单禁用时丢弃对应子树。
- 单应用菜单加载失败：记录错误，跳过该应用，不返回不完整子树。

## 13. 迁移与回滚摘要

| 阶段 | 线上行为 | 回滚方式 |
|---|---|---|
| 0 | 不变 | 关闭观测代码 |
| 1 | 不变（读旧字段） | 回滚代码，保留新字段 |
| 2 | 写入双写，读旧字段 | 功能开关恢复旧管理入口 |
| 3 | legacy 返回，shadow 对比 | `authorization.model=legacy` |
| 4 | 按租户灰度新授权 | 按租户关闭新模型 |
| 5 | 分开关切换读路径 | 各开关独立切回 legacy |
| 6 | 唯一模型 | 数据库备份 + 版本回退 |

关键迁移决策：

- 历史父级授权 → 显式 `SUBTREE` 或 `EXACT`，禁止批量无脑转通配。
- 租户应用默认策略切换前 → 为活跃租户生成等价 `tenant_app_config` 记录。
- 平台一级模块 → 回填为 `PLATFORM` 应用；租户应用根菜单 → 回填 `TENANT` 应用。

## 14. 测试策略

### 14.1 单元测试

- 权限匹配器：精确、通配、跨应用隔离、中间通配拒绝。
- 菜单排序：同 sort 值 ID 稳定、跨应用根节点顺序。
- 权限编码校验：`GROUP` 必须以 `:*` 结尾，`ACTION`/`NAVIGATION` 不得结尾 `:*`。

### 14.2 集成测试

- 菜单 CRUD 与托管权限同事务一致性。
- 角色授权读写与 `role_source` 正确性。
- 租户应用开通/禁用/过期后的权限即时性。
- 登录菜单树与权限快照端到端。

### 14.3 迁移测试

- 回填脚本可重复执行（幂等）。
- 测试库、预发布库全量回填后审计零阻断。
- 影子模式样本用户（平台 + 租户各至少一组）连续三次结果一致。

### 14.4 性能测试

- 单次 `hasPermission` 缓存命中 < 1ms，无 DB 查询。
- 菜单树生成：批量加载，禁止 N+1 查询。
- 角色/应用变更后缓存失效延迟可观测。

### 14.5 安全测试

- 未授权应用权限不可通过 API 直接提交。
- 平台默认权限不可被租户 API 删除。
- 通配授权审计完整性抽查。
