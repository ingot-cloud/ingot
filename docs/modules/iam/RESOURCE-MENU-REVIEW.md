# IAM 功能资源、操作与菜单盘点

2026-11-06 · 评审稿

本次以 `ingot@014af921` 的正式 IAM 初始化数据为基线，以 `ingot-admin@d17ff4d` 的平台端和组织端为主入口，并核对 Auth、IAM、Member、Security 的后台接口。微信小程序单列为用户侧接入。统计对应新建库，现有库可能保留自定义应用、菜单、资源和角色配置，实际差异待只读比对。[初始化清单](../../../databases/iam/manifest.json) · [正式种子](../../../databases/iam/006_bootstrap.sql) · [部署入口](../../../deploy/services/SAME-ORIGIN-LOGIN.md)

## 目录概况

| 管理端 | 应用编码 | 应用名 | 资源 | 操作 | 菜单节点 | 其中页面 | 菜单与操作关联 |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: |
| 平台 | `iam-platform` | 平台治理 | 17 | 77 | 13 | 9 | 22 |
| 组织 | `iam-tenant` | 组织治理 | 12 | 44 | 14 | 9 | 24 |
| 平台 | `platform:develop` | 开发者平台 | 4 | 15 | 5 | 4 | 8 |
| 平台 | `platform:security` | 安全中心 | 6 | 16 | 5 | 4 | 12 |
| 平台 | `platform:member` | App 用户管理 | 3 | 20 | 4 | 3 | 6 |
| 合计 | 5 个应用 | | 42 | 172 | 41 | 29 | 72 |

172 项操作中 171 项启用。`iam-tenant:role:upgrade` 保留停用，用于兼容历史引用。平台角色继续发布固定版本，组织角色保存后即时生效。两个治理角色的冷启动版本共写入 172 条授权关系，实际求值还会校验操作与应用状态。菜单是导航规则，角色授予精确操作，后端再检查身份、数据范围及字段策略。[种子生成源](../../../tools/iam/generate_bootstrap.py) · [组织即时授权](../../../specs/current/iam/tenant-live-authorization/SPEC.md)

## 优先处理的问题

| 问题 | 当前事实 | 建议 |
| --- | --- | --- |
| 通用上传缺业务授权 | IAM 与 Member 都有 `POST /v1/oss/upload`，控制器把客户端提交的 bucket 和文件名直接传给存储服务，未校验用途对应的资源操作或目标归属。管理台头像与通用组件复用 IAM 入口。 | 按上传场景绑定业务对象与写操作。本人头像绑定可信当前身份，代改成员或组织头像复用对应编辑操作和字段资格。服务端限定 bucket 与对象路径并生成对象名。先清点所有调用方，再收紧通用入口。 |
| 平台用户组有资源但入口漏配 | `iam-platform:group:read` 可独立控制组接口，「平台人员」菜单只关联 `iam-platform:member:read`。页面有成员、组两个 Tab。 | 改为「平台成员与用户组」，入口按两项 read 的 ANY 放行。按权限展示 Tab，默认打开第一个可读 Tab，各自按范围发请求。 |
| 组织工作台借用了应用权限 | 菜单绑定 `iam-tenant:application:read`。统计 API 按当前租户会话和各指标的可见范围返回数据，没有工作台独立操作。 | 在 `iam-tenant` 增加 `workbench` / `iam-tenant:workbench:read`，菜单与 API 一致。指标仍各自按资源校验，应用统计继续按开通、人群和可见页面计算。 |
| 权限诊断有操作但缺独立入口 | 两端都有 `authorization:diagnose`，入口挂在角色与授权工作区的行操作，菜单只认角色、分配、委派三项 read。 | 加「权限诊断」Tab，入口 ANY 增加 diagnose。成员、应用、操作及目标候选仍用自身读取与范围规则。 |
| 接口契约漏记或状态陈旧 | IAM 契约少 15 个已有方法映射，另有 23 个已匹配方法映射的条目仍标 `implemented: false`。 | 同步后端路由、真实操作条件和前端副本，保留退役接口的单独声明。补充跨服务管理面的覆盖检查。 |
| 初始化文档计数陈旧 | `databases/iam/README.md` 的正式种子摘要仍写 4 个应用、155 项操作、38 个菜单和 70 条关联。 | 按初始化生成结果更新文档与计数断言。 |
| 小程序仍使用旧接口前缀 | 本机小程序源代码的注册、个人资料请求仍指向 `/pms/v1/app/…`，当前 Member 控制器位于 `/v1/app/…`，仓库网关路由使用 `/member/**`。 | 用目标环境验证 OAuth 入口、代理重写和小程序兼容要求后统一路径。自助资料只允许本人的 App 用户身份，注册继续由注册协议校验。 |

上传证据见 IAM 与 Member 的 `OSSCommonAPI`、`AbstractS3OssService.uploadFile` 及管理台 `packages/admin-core/src/api/common/oss.ts`。工作台与诊断的权限关联见种子生成源。小程序路径是本机代码现状，目标环境是否另有代理重写需核实。[IAM 控制器](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/web/v1) · [网关路由](../../../nacos/PROD_GROUP/in-service-gateway.yml)

## 命名与授权口径

| 对象 | 统一写法 |
| --- | --- |
| 应用 | 平台和组织两端分别显示其可用应用。保留 5 个正式应用名，描述改用业务能力。 |
| 登录账号、成员、App 用户 | 「全局账号」负责登录凭证及全局状态，「平台成员」和「组织成员」负责管理域身份与关系，「App 用户」属于 Member 业务用户体系。登录手机号与域内联系手机号各自命名。 |
| 租户与组织 | 平台端用「租户」表示管理对象，组织端用「组织」描述当前工作空间。「租户」与「组织设置」分别用于平台、组织端。 |
| 开通、人群、分配、委派 | 「应用开通」表示租户具备使用资格，「可用人群」限制应用入口，「角色分配」向成员或用户组赋权，「授权委派」允许受限管理员在上限内分配角色。 |
| 资源、操作、菜单 | 资源用业务名词，操作用动词加对象，菜单用用户能识别的模块名。「操作」用于 IAM 目录授权，「App 权限」用于 Member 的业务权限记录。 |
| 角色与版本 | 「平台角色」「组织角色」「共享角色」分别体现归属。平台操作使用「发布平台角色版本」「应用角色版本」。组织操作使用「保存组织角色权限」。系统平台角色 `role_admin` 显示「超级管理员」。组织域的兼容系统角色 `tenant-governance` 可显示为「组织治理系统角色」，实际身份称「组织拥有者」。 |
| 常用操作 | 列表与详情共用 read 时称「查看对象」，已有独立 query / detail 时称「查看对象列表 / 查看对象详情」。统一用「创建」「编辑」「删除」。成员移除用「移出」，账号状态用「启用 / 停用」，锁定、解锁与重置密码分别命名。审计区分「平台授权审计」「组织授权审计」「封禁历史」。 |
| 编码与显示 | 权限保存精确操作码。资源身份以 `域 / 应用编码 / 资源编码` 为准。应用归属以数据库关系为准，开发者与安全中心存在历史前缀。可见文案独立维护，操作编码、资源编码、应用编码、保留 ID、URL、路由名和 viewPath 按兼容契约管理。 |

新增可分配的管理能力以「菜单或触发入口 → 资源操作 → 后端执行」为验收单元。选择器、预览、异步任务和批量操作记录实际的 ANY / ALL、目标范围或归属条件。本人自助、认证协议和服务间调用分别登记其授权边界，按可信身份或服务身份执行。

## 各应用的资源与菜单

资源表列出了正式种子的全部操作。把每行的编码前缀与后缀拼接可得精确操作码。二维码行直接列完整编码。范围一列表示资源声明的可选范围，接口仍按自身契约执行。应用段落给出建议描述。菜单表的名称箭头右侧为建议显示名，现有路径与页面键来自正式种子。目录按子页操作并集做 ANY 匹配，一级目录的 viewPath 为 `layout.main`，二级目录「成员与部门」为 `layout.simple`。页面使用各自的页面键。

### 平台治理

`PLATFORM / iam-platform`。管理租户、全局账号、平台成员、应用目录和平台授权。 应用图标 `ep:monitor`，域内顺序 1。按平台角色或系统管理员资格进入。

| 资源 | 操作编码前缀 | 操作后缀 | 范围 | 能力 |
| --- | --- | --- | --- | --- |
| 全局账号 `account` | `iam-platform:account:` | `create`、`delete`、`disable`、`enable`、`lock`、`lookup`、`read`、`reset-password`、`unlock`、`update` | 全量、指定对象 | 列表与详情、按用途查找、登录资料、启停、锁定、解锁、重置密码、删除 |
| 操作 → 应用操作 `action` | `iam-platform:action:` | `create`、`delete`、`read`、`status`、`update` | 全量、指定对象 | 应用内操作列表和授权目录、操作编辑与启停 |
| 应用 → 应用目录 `application` | `iam-platform:application:` | `create`、`delete`、`purge`、`read`、`status`、`update` | 全量、指定对象 | 应用详情与整单创建、启停、删除和强制清除。整单创建还校验资源、操作、菜单创建权限 |
| 角色授权 → 平台角色分配 `assignment` | `iam-platform:assignment:` | `create`、`delete`、`read`、`update`、`upgrade` | 全量、指定对象 | 向成员或组分配固定角色版本、候选和范围参数、修改、撤销、升级、批量应用版本任务 |
| 审计记录 → 平台授权审计 `audit` | `iam-platform:audit:` | `read` | 全量 | 审计列表、详情、筛选、事件变更展示 |
| 授权诊断 → 平台权限诊断 `authorization` | `iam-platform:authorization:` | `diagnose` | 全量 | 诊断候选与权限判定、授权来源及目标范围 |
| 授权委派 → 平台授权委派 `delegation` | `iam-platform:delegation:` | `create`、`delete`、`preview`、`read`、`update` | 全量、指定对象 | 委派配置、接收人、角色白名单、操作及范围上限、期限、预览、撤销 |
| 字典 `dictionary` | `iam-platform:dictionary:` | `create`、`delete`、`read`、`update` | 全量 | 类型和字典项、层级、启停与排序。启停和排序复用 update |
| 应用开通 → 租户应用开通 `entitlement` | `iam-platform:entitlement:` | `preview`、`read`、`update` | 全量、指定对象 | 租户已开通应用、开通预览与配置 |
| 用户组 → 平台用户组 `group` | `iam-platform:group:` | `create`、`delete`、`preview`、`read`、`update` | 全量、指定对象 | 组、成员关系、候选和引用影响预览 |
| 成员 → 平台成员 `member` | `iam-platform:member:` | `create`、`read`、`remove`、`status`、`update` | 全量、本人、指定对象 | 成员列表、资料与域内联系信息、启停与移出。角色维护和联系资料还受对应操作、范围及字段约束 |
| 菜单 → 应用菜单 `menu` | `iam-platform:menu:` | `create`、`delete`、`read`、`update` | 全量、指定对象 | 应用菜单树、路由与显示配置、菜单关联操作与匹配规则 |
| 套餐 → 租户套餐 `plan` | `iam-platform:plan:` | `create`、`read`、`update` | 全量、指定对象 | 套餐、应用清单、配置调整。应用到租户走开通流程 |
| 资源 → 应用资源 `resource` | `iam-platform:resource:` | `create`、`delete`、`read`、`update` | 全量、指定对象 | 资源目录、对象范围、字段能力及字段绑定引用 |
| 角色 → 平台角色 `role` | `iam-platform:role:` | `create`、`delete`、`preview`、`publish`、`read`、`status` | 全量、指定对象 | 角色、权限、版本详情及元数据、预览、发布与启停。分配和应用版本另校验相关操作 |
| 共享角色 `shared-role` | `iam-platform:shared-role:` | `create`、`delete`、`preview`、`read`、`status`、`update` | 全量、指定对象 | 组织可用的共享角色、完整权限与字段配置、预览与启停 |
| 组织 → 租户 `tenant` | `iam-platform:tenant:` | `create`、`preview`、`read`、`update` | 全量、指定对象 | 租户列表、详情、创建预览、创建与编辑，初始化拥有者和基础开通 |

| 上级目录 | 菜单名 | 路径 | routeName / 页面 viewPath | 当前菜单操作 |
| --- | --- | --- | --- | --- |
| 一级目录 | 组织与租户 → 租户管理 | `/platform/iam/tenant` | `platform.iam.tenant` | 子页操作并集，ANY |
| `platform.iam.tenant` | 租户管理 → 租户列表 | `/platform/iam/tenants` | `platform.iam.tenants` | `iam-platform:tenant:read` |
| 一级目录 | 应用与配置 | `/platform/iam/config` | `platform.iam.config` | 子页操作并集，ANY |
| `platform.iam.config` | 应用目录 | `/platform/iam/applications` | `platform.iam.applications` | `iam-platform:application:read` |
| `platform.iam.config` | 套餐 → 租户套餐 | `/platform/iam/plans` | `platform.iam.plans` | `iam-platform:plan:read` |
| `platform.iam.config` | 共享角色 | `/platform/iam/shared/roles` | `platform.iam.shared.roles` | `iam-platform:shared-role:read` |
| 一级目录 | 平台管理 | `/platform/iam/manage` | `platform.iam.manage` | 子页操作并集，ANY |
| `platform.iam.manage` | 全局账号 | `/platform/iam/accounts` | `platform.iam.accounts` | `iam-platform:account:read` |
| `platform.iam.manage` | 平台人员 → 平台成员与用户组 | `/platform/iam/personnel` | `platform.iam.personnel` | `iam-platform:member:read` |
| `platform.iam.manage` | 角色与授权 | `/platform/iam/authorization` | `platform.iam.authorization` | `iam-platform:role:read`, `iam-platform:assignment:read`, `iam-platform:delegation:read` |
| `platform.iam.manage` | 平台授权审计 | `/platform/authorization-audit` | `platform.iam.authorization.audit` | `iam-platform:audit:read` |
| 一级目录 | 系统设置 | `/platform/settings` | `platform.settings` | 子页操作并集，ANY |
| `platform.settings` | 字典管理 | `/platform/settings/dictionaries` | `platform.settings.dictionaries` | `iam-platform:dictionary:read` |

授权工作区的显示名建议为「角色」「角色分配」「授权委派」「权限诊断」。角色分配删除对应的用户操作文案为「撤销角色分配」。「发布版本角色」改为「发布平台角色版本」，账号重置改为「重置全局账号密码」。

### 组织治理

`TENANT / iam-tenant`。管理本组织的成员、部门、用户组、应用可用人群和授权。 应用图标 `ep:office-building`，域内顺序 1。组织创建默认开通。

| 资源 | 操作编码前缀 | 操作后缀 | 范围 | 能力 |
| --- | --- | --- | --- | --- |
| 应用 → 已开通应用 `application` | `iam-tenant:application:` | `read` | 全量、指定对象 | 当前租户已开通应用、操作候选及权限树 |
| 角色授权 → 组织角色分配 `assignment` | `iam-tenant:assignment:` | `create`、`delete`、`read`、`update` | 全量、指定对象 | 分配、候选、成员或组的授权来源、编辑与撤销。受限管理员按当前委派来源执行 |
| 应用可用人群 `audience` | `iam-tenant:audience:` | `read`、`update` | 全量 | 查看和维护应用可用的成员、用户组、部门及范围 |
| 审计记录 → 组织授权审计 `audit` | `iam-tenant:audit:` | `read` | 全量 | 组织审计列表、详情和筛选 |
| 授权诊断 → 组织权限诊断 `authorization` | `iam-tenant:authorization:` | `diagnose` | 全量 | 诊断本人或可见目标的权限、候选和原因 |
| 授权委派 → 组织授权委派 `delegation` | `iam-tenant:delegation:` | `create`、`delete`、`preview`、`read`、`update` | 全量、指定对象 | 委派上限、接收人、候选、预览、维护及撤销。本人有效来源另有自助查询 |
| 部门 `department` | `iam-tenant:department:` | `create`、`delete`、`export`、`leaders`、`read`、`update` | 全量、管理部门、指定对象 | 树、详情、层级、移动、负责人、导航、成员关系及表格导入导出。移动复用 update |
| 字段策略 → 资源字段策略 `field-policy` | `iam-tenant:field-policy:` | `read`、`update` | 全量 | 资源字段清单、策略读取和更新。当前启用成员字段策略 |
| 用户组 → 组织用户组 `group` | `iam-tenant:group:` | `create`、`delete`、`preview`、`read`、`update` | 全量、指定对象 | 组资料、显式成员、部门来源及有效成员、修改影响预览 |
| 成员 → 组织成员 `member` | `iam-tenant:member:` | `create`、`departments`、`export`、`read`、`remove`、`status`、`update` | 全量、本人、任职部门、管理部门、指定对象 | 列表、详情、候选、按手机号加入、重新加入、资料、任职、状态、移出、JSON 与表格导出和表格导入 |
| 角色 → 组织角色 `role` | `iam-tenant:role:` | `create`、`delete`、`preview`、`read`、`status`、`update`、`upgrade` | 全量、指定对象 | 即时生效的组织角色、权限配置、预览、启停、删除、已分配成员和用户组。升级操作停用 |
| 组织设置 `settings` | `iam-tenant:settings:` | `owner-transfer`、`read`、`update` | 全量 | 组织基本资料与拥有者转交，转交校验当前拥有者及登录密码 |

| 上级目录 | 菜单名 | 路径 | routeName / 页面 viewPath | 当前菜单操作 |
| --- | --- | --- | --- | --- |
| 一级目录 | 工作台 | `/org/iam/workspace` | `org.iam.workspace` | 子页操作并集，ANY |
| `org.iam.workspace` | 工作台 → 组织概览 | `/org/iam/workbench` | `org.iam.workbench` | `iam-tenant:application:read` |
| 一级目录 | 组织管理 | `/org/iam/organization` | `org.iam.organization` | 子页操作并集，ANY |
| `org.iam.organization` | 成员与部门 | `/org/iam/members/root` | `org.iam.members.root` | 子页操作并集，ANY |
| `org.iam.members.root` | 成员 | `/org/iam/members` | `org.iam.members` | `iam-tenant:member:read` |
| `org.iam.members.root` | 部门 | `/org/iam/departments` | `org.iam.departments` | `iam-tenant:department:read` |
| `org.iam.organization` | 用户组 | `/org/iam/groups` | `org.iam.groups` | `iam-tenant:group:read` |
| `org.iam.organization` | 组织设置 | `/org/iam/settings` | `org.iam.settings` | `iam-tenant:settings:read` |
| 一级目录 | 权限与应用 | `/org/iam/authorization/root` | `org.iam.authorization.root` | 子页操作并集，ANY |
| `org.iam.authorization.root` | 角色与授权 | `/org/iam/authorization` | `org.iam.authorization` | `iam-tenant:role:read`, `iam-tenant:assignment:read`, `iam-tenant:delegation:read` |
| `org.iam.authorization.root` | 应用管理 → 已开通应用 | `/org/iam/applications` | `org.iam.applications` | `iam-tenant:application:read` |
| 一级目录 | 安全与合规 | `/org/iam/compliance` | `org.iam.compliance` | 子页操作并集，ANY |
| `org.iam.compliance` | 字段策略 → 资源字段策略 | `/org/field-policies` | `org.iam.field.policies` | `iam-tenant:field-policy:read` |
| `org.iam.compliance` | 组织授权审计 | `/org/authorization-audit` | `org.iam.authorization.audit` | `iam-tenant:audit:read` |

建议补充「查看组织工作台」操作 `iam-tenant:workbench:read`。组织角色的权限更新显示为「保存组织角色权限」。成员和部门的导入导出入口列在隐藏路由中。

### 开发者平台

`PLATFORM / platform:develop`。管理 OAuth 2.0 客户端、社会化登录配置和业务 ID，生成二维码。 应用图标 `ep:cpu`，域内顺序 2。按平台角色或系统管理员资格进入。

| 资源 | 操作编码前缀 | 操作后缀 | 范围 | 能力 |
| --- | --- | --- | --- | --- |
| 客户端 → OAuth 2.0 客户端 `client` | `platform:develop:client:` | `create`、`delete`、`detail`、`query`、`reset`、`update` | 全量 | 列表、详情、创建、编辑、删除、重置密钥，Auth 在线权限表达式 |
| 发号 → 业务 ID 发号 `id-allocation` | `iam-platform:id-allocation:` | `create`、`delete`、`read`、`update` | 全量 | 发号标签及号段配置管理，操作前缀保留 IAM 历史值 |
| 二维码 → 二维码生成 `qrcode` | 完整编码 | `platform:develop:qrcode` | 全量 | 浏览器配置、生成与下载二维码，页面访问校验该操作 |
| 社会化登录配置 `social-config` | `iam-platform:social-config:` | `create`、`delete`、`read`、`update` | 全量 | 社会化登录渠道和接入配置，操作前缀保留 IAM 历史值 |

| 上级目录 | 菜单名 | 路径 | routeName / 页面 viewPath | 当前菜单操作 |
| --- | --- | --- | --- | --- |
| 一级目录 | 开发者平台 | `/platform/develop` | `platform.develop` | 子页操作并集，ANY |
| `platform.develop` | 生成二维码 | `/platform/develop/qrcode` | `platform.develop.qrcode` | `platform:develop:qrcode` |
| `platform.develop` | 客户端管理 → OAuth 2.0 客户端 | `/platform/develop/client` | `platform.develop.client` | `platform:develop:client:query` |
| `platform.develop` | 社交管理 → 社会化登录配置 | `/platform/develop/social` | `platform.develop.social` | `iam-platform:social-config:read` |
| `platform.develop` | 业务ID管理 → 业务 ID 发号 | `/platform/develop/id` | `platform.develop.id` | `iam-platform:id-allocation:read` |

### 安全中心

`PLATFORM / platform:security`。管理凭证、账号防护、访问策略及在线会话。 应用图标 `ep:lock`，域内顺序 3。按平台角色或系统管理员资格进入。

| 资源 | 操作编码前缀 | 操作后缀 | 范围 | 能力 |
| --- | --- | --- | --- | --- |
| 凭证策略 `credential-policy` | `iam-platform:credential-policy:` | `create`、`delete`、`read`、`update` | 全量 | 密码强度、历史、过期与初始密码策略 |
| 账号锁定策略 `lockout-policy` | `iam-platform:lockout-policy:` | `read`、`update` | 全量 | 按用户类型查看和编辑锁定规则 |
| 登录失败防护 `login-failure-policy` | `iam-platform:login-failure-policy:` | `read`、`update` | 全量 | 登录失败防护维度与配置 |
| 安全策略 → 访问防护策略 `security-policy` | `iam-platform:security-policy:` | `create`、`delete`、`read`、`update` | 全量 | API 路径分组、限流规则、黑白名单、挑战策略、违规升级、历史封禁及强制广播刷新 |
| 在线会话 `session` | `iam-platform:session:` | `read`、`revoke` | 全量 | 会话列表、详情、按会话或按用户撤销 |
| 会话并发策略 `session-policy` | `iam-platform:session-policy:` | `read`、`update` | 全量 | 策略列表、详情、创建、编辑和删除。后三项共用 update |

| 上级目录 | 菜单名 | 路径 | routeName / 页面 viewPath | 当前菜单操作 |
| --- | --- | --- | --- | --- |
| 一级目录 | 安全中心 | `/security` | `security.root` | 子页操作并集，ANY |
| `security.root` | 凭证策略 | `/security/credential-policies` | `security.credential.policies` | `iam-platform:credential-policy:read` |
| `security.root` | 账号保护 | `/security/account-protection` | `security.account.protection` | `iam-platform:lockout-policy:read` |
| `security.root` | 访问防护 | `/security/access-protection` | `security.access.protection` | `iam-platform:security-policy:read`, `iam-platform:login-failure-policy:read` |
| `security.root` | 会话管理 | `/security/sessions` | `security.sessions` | `iam-platform:session:read`, `iam-platform:session-policy:read` |

会话 revoke 显示为「撤销在线会话」。访问防护的「封禁历史」与 IAM 授权审计分别呈现。

### App 用户管理

`PLATFORM / platform:member`。管理 App 用户及其业务角色、权限和关联关系。 应用图标 `ep:iphone`，域内顺序 4。按平台角色或系统管理员资格进入。

| 资源 | 操作编码前缀 | 操作后缀 | 范围 | 能力 |
| --- | --- | --- | --- | --- |
| App 权限 `permission` | `platform:member:permission:` | `create`、`delete`、`query`、`update` | 全量 | App 权限树、创建、编辑、启停、删除 |
| App 角色 `role` | `platform:member:role:` | `create`、`delete`、`permissions:query`、`permissions:set`、`query`、`update`、`user:assign` | 全量 | 角色树和候选、维护和启停、分配用户、读取及设置角色权限 |
| App 用户 `user` | `platform:member:user:` | `create`、`delete`、`detail`、`lock`、`query`、`reset-pwd`、`roles:query`、`roles:set`、`update` | 全量 | 用户列表、详情、资料维护、启停、删除、重置密码、锁定或解锁、读取及设置用户角色 |

| 上级目录 | 菜单名 | 路径 | routeName / 页面 viewPath | 当前菜单操作 |
| --- | --- | --- | --- | --- |
| 一级目录 | App 用户管理 | `/member` | `member.root` | 子页操作并集，ANY |
| `member.root` | 用户管理 → App 用户 | `/member/users` | `member.user` | `platform:member:user:query` |
| `member.root` | 角色管理 → App 角色 | `/member/roles` | `member.role` | `platform:member:role:query` |
| `member.root` | 权限管理 → App 权限 | `/member/permissions` | `member.permission` | `platform:member:permission:query` |

各资源的 create 统一显示为「创建 App 用户」「创建 App 角色」「创建 App 权限」。角色绑定操作 `role:user:assign` 显示为「为 App 角色配置用户」。

## 复用操作与特殊入口

| 能力 | 入口 | 现有授权边界 | 整理方式 |
| --- | --- | --- | --- |
| 成员、部门表格 | 两条 `/org/iam/{members,departments}/workbook` 隐藏静态路由 | 成员创建、编辑、任职调整、导出或部门创建、编辑、导出。按模式校验。任务文件还校验发起人及状态。 | 保持在成员、部门资源下，按模板、导入模式、导出、任务文件标出具体门禁。跨域、越权和直接访问隐藏路由纳入验收。 |
| 我的下载 | 组织端顶部工具入口 `/downloads`，后端 `/v1/tenant/downloads` | 可信租户身份及本人任务，签发短时链接时复核来源导出权限。 | 作为本人任务入口登记。授权验证覆盖成员和部门导出来源、撤权、跨成员、跨组织及到期。 |
| 组织成员选择 | `/v1/tenant/member-picker/browse`、部门导航和成员候选 | 根据场景复用成员查看、部门负责人维护或角色分配资格，再校验可见范围。 | 维护场景到操作的映射，覆盖角色分配、委派、人群、部门负责人等实际调用方。 |
| 个人账号 | 当前身份资料、改密、强制改密状态、会话权限视图 | BFF 与 IAM `/v1/me/*` 绑定当前可信身份；自助资料与改密只操作当前账号。 | 统一称「个人资料」「修改密码」，保留强制改密流程及本人边界。 |
| 登录与会话引导 | 平台登录、组织登录、选择组织、事务恢复、退出、OAuth/OIDC、JWK、加密公钥 | 可信入口、协议、会话、CSRF 或客户端约束随接口而异。 | 单列基础契约。后台管理 OAuth 客户端及会话仍分别归开发者平台和安全中心。 |
| 帮助中心 | 双端 `/help`、分类、文章、搜索 | 前端静态内容，按宿主装配各自入口和文章。 | 统一称「帮助中心」，继续记录为静态入口。 |
| App 用户自助 | 小程序注册与登录、Member `/v1/app/user` | 注册走社会化注册流程，资料按当前认证用户 ID 读取或更新。 | 分开记录平台管理端与 App 用户端。目标环境核验路由与 App 用户身份约束。 |
| 内部运行能力 | IAM 授权快照与执行、内网字典/发号/社交/身份查询，安全事件上报、凭证/策略读写、会话失效等 | 内部调用身份。IAM 授权 RPC 还恢复转发的在线成员身份。 | 在服务契约中登记。实际管理界面开放时再定义面向管理角色的操作。 |
| 开发与旧入口 | `apps/dev-portal`、`examples/iam-ops`、`ingot-test`；管理台 13 个未绑定正式种子的旧页面或样板 | 当前正式宿主目录未提供这些页面的种子入口。旧页面仍有部分 PMS API 引用。 | 样例与开发文档单列。旧页面先用实际库查引用，再决定退役。 |

安全中心目前展示的是历史封禁记录。统一安全事件 `fw_security_event` 已有采集、存储和内部入口，现行基线还没有平台读侧 API。若将事件查询纳入产品范围，应增设安全事件资源及独立的查看、导出操作，旧封禁历史按其数据源标注。会话客户端筛选继续单独校验 `platform:develop:client:query`。[安全事件中心](../../../specs/current/security/security-event-center/SPEC.md) · [平台能力](../../../specs/current/iam/platform-capabilities/SPEC.md) · [下载中心](../../../specs/current/framework/download-center/SPEC.md)

本机前端额外注册的 13 个未绑定正式种子的页面键，按宿主分组如下。两个表格工作簿页已有单独的静态入口，见上表。

- 平台端共有 8 个，分别为 `platform.admin.user`、`platform.config.app.detail`、`platform.config.app.home`、`platform.config.menu`、`platform.config.permission`、`platform.config.role`、`platform.dashboard`、`platform.org.tenant`。
- 组织端共有 5 个，分别为 `org.contacts.auth`、`org.contacts.dept`、`org.contacts.role`、`org.contacts.structure`、`org.contacts.user`。

### 接口契约缺项

下表合计 15 个方法映射。IAM 与 Member 的两个通用上传接口按前述单独审查。

| 数量 | 方法与路径 | 现有权限或身份条件 |
| ---: | --- | --- |
| 1 | `POST /v1/tenant/members/{id}/rejoin` | `member:create`，再校验原成员状态、目标部门、字段及组织版本。 |
| 6 | `/v1/tenant/members/workbook` 下 `GET /template`、`GET /history`、`POST /import`、`POST /export`、`GET /{id}/status`、`GET /{id}/file` | 模板用 `member:create`。导入与历史按创建、资料编辑或任职调整权限判定。导出用 `member:export`。任务状态和文件校验归属与对应来源权限。 |
| 3 | `GET /v1/tenant/roles/{id}/members`、`/groups`、`/members/{memberId}/assignments` | 可读角色及分配查询资格，按角色可见性、授权来源和关联范围分页。 |
| 1 | `GET /v1/tenant/applications/{id}/action-catalog` | `application:read`，目标应用须对当前租户开通。 |
| 3 | `GET /v1/tenant/downloads`、`GET /v1/tenant/downloads/{id}`、`POST /v1/tenant/downloads/{id}/link` | 当前租户、本人任务，签发前复核来源。 |
| 1 | `GET /v1/tenant/workbench/statistics` | 当前租户身份及指标各自的可见范围。 |

另有 23 项已匹配方法映射的契约标记为 `implemented: false`，涉及组织角色、平台共享角色、组织角色权限目录、分配候选和委派。组织角色的旧版本、升级入口是另有明确退役行为的兼容路径，应继续区分。[公开路由契约](../../../specs/changes/active/20260912-iam-identity-access-management/contracts/routes.json) · [核对程序](../../../tools/iam/test_contract.py)

### 可单独分配的操作

现有共享授权的能力也应在清单中明确。以下拆分将改变授权行为，需先确认谁可以独立获得这些操作。

| 现有共用 | 可选拆分 | 迁移决策 |
| --- | --- | --- |
| `security-policy:update` 同时覆盖访问防护配置更新和全量失效广播 | 单独定义「广播策略刷新」操作。历史封禁读取可独立定义。 | 明确现有策略编辑者能否继续触发广播及查看历史。 |
| `session-policy:update` 覆盖新增、更新和删除 | 分为「创建会话并发策略」「编辑会话并发策略」「删除会话并发策略」。 | 逐角色比对新增与删除能力。 |
| App 用户、角色、权限的 `update` 同时覆盖资料与启停，App 用户的 `lock` 同时覆盖锁定与解锁 | 按实际业务岗位拆分状态管理和资料编辑。 | 平台全局账号已分离这些动作，App 管理保留独立命名空间。 |
| 成员重新加入与创建共用 `member:create`，表格导入与逐条写入共用相应操作 | 只有需限制批量修改或单独控制重新加入时才拆为专用操作。 | 保留对象范围、字段规则、导入模式以及下载来源复核。 |

## 实施顺序与验收

1. 确认命名、工作台是否独立授权、下载及自助入口的规则、可单独分配的高风险操作范围。完成 SDD 需求、设计、兼容性、迁移、测试与验收对齐后建立 active change。
2. 先修正契约漏项和显示命名，再处理用户组与权限诊断入口。对照正式种子、在用角色、已有菜单以及前端实际入口，验证单项读取、混合授权、完全无权、直接 URL 和 API 调用。
3. 将工作台与上传用途接入明确的授权规则。工作台现有入口资格来自应用读取，升级前按实际角色与人群计算影响，明确需保留的资格。上传先盘点场景与对象范围，验证无权限、跨域、跨成员及任意路径请求被拒绝。
4. 同步维护正式目录生成源、初始化脚本与已有库增量迁移。按自然键核对并保留已有 ID 与自定义项，角色固定版本保持不可变，确需变更通过发布和应用版本完成。禁止用完整初始化脚本升级已有库。已有库迁移预演应报告受影响行与角色，有冲突时停止并提供回退依据。
5. 验收用普通角色逐项验证「授权后可见且可执行，撤权后按实时或热窗口契约失效」。多权限页面只加载已授权面板，批量及后台任务校验范围、字段与任务归属。检查新库、已有库、迁移重跑、回退和前后端契约一致性。

初始化生成校验及 7 项种子单测通过。8 项 IAM 契约测试中有 1 项因上表 15 个方法映射缺项失败。4 项数据库源测试中有 1 项仍断言 66 张表，当前清单生成 64 张表。29 个正式 PAGE 的 viewPath 均可解析到本机前端组件。已知并行工作涉及授权审计展示、App 用户管理目标环境发布和租户即时授权实际库发布，实施时应沿用各自验收边界。目标环境菜单、角色引用与自定义数据可通过只读目录快照补证，所需字段限于应用、资源、操作、菜单关联及角色授权引用。[SDD 工作流](../../../specs/README.md) · [活动变更](../../../specs/changes/active/README.md) · [已有库升级约定](../../../databases/iam/migrations/README.md)
