# IAM 功能目录与授权补齐 SDD 方案

2026-11-06 · 方案评审

本方案覆盖两个管理端的五个 IAM 应用，定义资源、操作、菜单和显示名称，重点补齐上传、用户组、组织工作台及权限诊断。系统尚未上线，新目录可直接启用规范后的编码。开发验证使用新建隔离库和重新创建的测试角色。现有业务页面 URL、routeName 和 viewPath 继续作为前端契约。现状及证据见 [功能目录评审稿](RESOURCE-MENU-REVIEW.md)。

## 需求与范围

- 每项正式功能都有明确的授权入口。后台管理操作必须归属一个应用和资源，并由服务端按精确操作、可信域、目标范围及适用字段执行校验。菜单、Tab 和按钮按相同契约显示。
- 列表、详情、候选、预览、导入导出及异步文件都登记其实际授权条件。同一业务权限可覆盖多个入口。本人自助、认证协议、内部调用及纯前端工具按各自执行边界登记。
- 可独立分配的高风险操作单列，覆盖密码和密钥重置、账号状态、锁定解锁、强制清除、刷新访问策略、会话撤销及并发策略变更。
- 完整清单以当前已实现功能为范围。统一安全事件的采集和内部上报纳入登记，平台安全事件查询属于另一个功能需求。访问防护已有的历史封禁查询在本次整理。
- 保留组织拥有者和平台超级管理员的可信动态授权规则、平台角色固定版本、组织角色即时生效、委派上限、应用开通和可用人群约束。

## 应用和操作命名

| 域 | 应用名 | 当前编码 | 目标编码 | 建议描述 |
| --- | --- | --- | --- | --- |
| PLATFORM | 平台治理 | `iam-platform` | `iam-platform` | 管理租户、全局账号、平台成员、应用目录和平台授权。 |
| TENANT | 组织治理 | `iam-tenant` | `iam-tenant` | 管理本组织的成员、部门、用户组、应用可用人群和授权。 |
| PLATFORM | 开发者平台 | `platform:develop` | `platform-developer` | 管理 OAuth 2.0 客户端、社会化登录配置和业务 ID，生成二维码。 |
| PLATFORM | 安全中心 | `platform:security` | `platform-security` | 管理凭证、账号防护、访问策略及在线会话。 |
| PLATFORM | App 用户管理 | `platform:member` | `platform-member` | 管理 App 用户及其角色、权限和关联关系。 |

- 内置操作码统一为 `<app-code>:<resource-code>:<operation>`，各段使用小写 kebab-case。应用编码与资源归属一致。新建业务应用及操作沿用同一规范，由目录校验一致性。作用域由 `ResourceKey` 与目录外键确定，代码前缀用于命名。通过目录或显式注册拿到实际的域、应用及资源，改掉诊断和范围分发中从操作码截取资源名的逻辑。既有 SDK 的资源身份继续使用三元组。
- 一个操作覆盖列表和详情时用 `read`。已有可分别授予的列表和详情用 `read-list`、`read-detail`。关系列表或替换用 `read-roles`、`set-roles` 等。用途不同的动作采用具体动词，例如 `revoke`、`publish-version`、`transfer-owner`、`reset-secret`。操作授权使用精确码。
- 界面中「全局账号」负责登录资料与凭证状态，「平台成员」「组织成员」描述管理域身份，「App 用户」属于 Member 业务用户体系。平台端管理对象使用「租户」，组织端使用「组织」。平台「角色分配」和「授权委派」分别命名，审计文案按平台、组织区分。
- 中文操作按下表的资源名组合。通用操作用「查看、创建、编辑、删除、预览、启停 + 对象」。列表及详情分别用「查看对象列表」「查看对象详情」。特殊动词见目录下的中文文案表。应用和菜单中文展示名与角色操作码分开维护。

## 四项授权设计

### 图片上传

通用 OSS 上传改为用途受限的图片上传。IAM 使用 `POST /api/iam/v1/assets/uploads`，Member 使用 `POST /api/member/v1/assets/uploads`。请求为 multipart，字段为 `purpose`、`file`、场景所需的 `targetId`。服务端枚举固定场景及操作，拒绝额外的 bucket、对象路径或操作码。前端组件由业务表单注入上传用途，选择文件时在本地预览，提交业务表单时才上传。

| 服务 | 用途编码 | 必须满足的授权 |
| --- | --- | --- |
| IAM | `platform-member-avatar-create`、`platform-member-avatar-update` | 分别要求 `iam-platform:member:create`、`iam-platform:member:update` 和适用的成员范围、avatar 字段写权限。 |
| IAM | `tenant-member-avatar-create`、`tenant-member-avatar-rejoin`、`tenant-member-avatar-update` | 新增和重新加入用 `iam-tenant:member:create`，编辑用 `iam-tenant:member:update`。按已有部门范围、成员状态和 avatar 字段授权执行。 |
| IAM | `platform-tenant-logo-create`、`platform-tenant-logo-update` | 分别要求 `iam-platform:tenant:create`、`iam-platform:tenant:update`，编辑绑定当前权限内的目标租户。 |
| IAM | `tenant-logo-update` | `iam-tenant:settings:update`，目标从当前可信组织身份取得。 |
| IAM | `application-icon-create`、`application-icon-update` | 分别要求 `iam-platform:application:create`、`iam-platform:application:update`。整单创建仍按实际内容校验资源、操作和菜单的创建操作。Iconify 名走普通表单字段，菜单图标继续使用已支持的图标名。 |
| Member | `app-user-avatar-create`、`app-user-avatar-update` | 分别要求 `platform-member:user:create`、`platform-member:user:update`，使用 Member 的可信平台域与 ALL 校验。 |
| Member | `app-self-avatar-update` | 要求有效 App 用户会话，目标 ID 从当前 App 用户身份取得。App 资料读写继续限制本人。管理台个人资料当前仅编辑登录手机号与邮箱。 |

上传阶段校验在线会话、用途操作、已有对象范围及可提前判断的字段资格。编辑既有对象和重新加入成员须提供目标 ID。新建、当前组织和 App 用户本人场景由服务端约束目标，拒绝显式传入目标 ID。新建对象所需的完整范围、字段条件与附加授权随业务提交计算，例如组织成员的目标部门和应用整单配置。提交时重验全部条件，上传记录本身不产生业务写入资格。

- 使用服务端配置的私有 bucket 和服务端生成的不可覆盖对象键。头像和 Logo 接受可解码的 JPEG、PNG、WebP 静态图片，方案默认上限 5 MiB、宽高各 2048 像素。按内容识别类型，拒绝伪造后缀、SVG、超限或解码失败。入口和应用层的 multipart 限制同步配置。
- 上传返回短时的一次性 `uploadId`。记录绑定域、组织、操作者、用途及已有目标 ID。业务表单在对应字段提交 `avatarUploadId` 或 `iconUploadId`，后端将凭证按实际写入字段处理，成员头像继续走原有 avatar 字段投影与写入规则。更新表单区分保留、替换和显式清除，清除遵循当前字段契约。应用图标允许直接提交有效的 Iconify 名。替换图片需提交该用途的上传凭证，任意路径或签名 URL 无法绑定新图。
- 上传先登记待写入记录，存储成功后才标记为可绑定。在业务数据所属库的事务内锁定并消费凭证，与业务对象引用同事务提交。持久化只保存 `bucket/objectName`，响应用时效链接。相同凭证并发绑定最多成功一次。上传阶段记录默认 30 分钟到期，清理任务回收超时未绑定的对象，重试删除失败项。清理先锁定并复核记录状态，与绑定操作互斥。
- 前端改用新接口后移除 IAM 与 Member 的旧通用上传入口。成员、部门表格仍使用各自 workbook 接口，下载任务仍复核来源操作。

### 平台用户组

保留 `/platform/iam/personnel` 和 `platform.iam.personnel`，显示名为「平台成员与用户组」。菜单按 `iam-platform:member:read`、`iam-platform:group:read` 的 ANY 放行。两个 Tab 分别按各自的 read 可见，默认首个可读 Tab，撤权后切换至其他可读 Tab 或退出。组件仅发起当前可读 Tab 需要的请求。

- 平台组列表、详情、候选、关联查询、编辑、删除、预览按本次操作的 ALL 或 OBJECT_SET 过滤。列表与总数使用同一范围条件。新建组要求 create 的 ALL。已有目标操作拒绝越界 ID，提交时再次校验目标和关联。
- 只持有组读取资格可以查看授权范围内的组资料。组的成员 ID、成员详情、搜索和可见人数还需成员读取资格、成员数据范围和字段权限。按交集分页和计数。成员详情中的组关联反向检查 group:read 和组范围。缺少关联读取资格时页面省略该块。
- 组编辑保留原 URL。更新或预览时省略 `selection` 只编辑组资料，显式传入时按完整替换处理。添加、移除的成员均须位于当前可选范围，目标组需对应操作的对象资格。完整替换涉及不可见的既有关联时拒绝提交，前端有条件启用关联编辑。新建成员时顺带加入既有组也要校验 `group:update` 和组范围。继续校验角色分配、委派及引用影响，变更成功后按既有规则使授权失效。
- 两域复用组范围逻辑。组织组列表、详情、组的成员关系及工作台组计数按当前组织和操作的范围求值。组织组的部门展开及其负责人、成员字段保持现有独立约束。

### 组织工作台

新增 `TENANT / iam-tenant / workbench`，唯一操作 `iam-tenant:workbench:read`，显示「查看组织工作台」，范围为当前组织内 ALL。菜单「工作台 / 组织概览」和 `GET /api/iam/v1/tenant/workbench/statistics` 均要求此操作。工作台仍置顶，页面 URL `/org/iam/workbench`、注册键 `org.iam.workbench` 保留。

- 工作台读取只返回当前组织的最小资料和可见统计。成员、部门、用户组数分别使用其 read 操作及数据范围，无资格的指标省略，有资格但范围为空返回零。
- 应用数沿用开通、人群及可见非隐藏页面的去重口径。快捷入口从当前可见菜单构建。应用读取资格与工作台读取资格独立。
- 组织拥有者按已有动态规则取得有效基础应用内的工作台操作。普通角色通过角色编辑明确授予。请求必须由当前有效租户身份进入，授权不可用时按 IAM 现有规则关闭访问。

### 权限诊断

两域资源改为 `authorization-diagnosis`，操作为 `iam-platform:authorization-diagnosis:run` 和 `iam-tenant:authorization-diagnosis:run`，范围为当前域 ALL。两端「角色与授权」保留现有 URL 和注册键，新增「权限诊断」Tab。菜单按角色读取、分配读取、委派读取、诊断运行四项做 ANY 匹配。已有行操作跳转到同一诊断面板并传入所选对象。

- 有诊断操作即可打开 Tab。默认诊断可信当前成员，展示当前身份的最小标识。查看他人须同时具备相应域的成员读取和目标成员范围。按全局账号传入的既有诊断请求先解析到当前域成员，再执行相同检查。伪造 ID 的请求与候选搜索走相同规则。
- 应用与操作候选只暴露诊断所需的名称和标识，限制当前域、启用目录及组织开通。操作须属于所选应用。业务目标候选和提交目标都按资源注册的读取操作、目标范围及既有扩展规则判断，使用真实资源键，跨应用同名资源互相隔离。对没有对象候选的资源明确显示「无需指定目标」或「不支持选择对象」，缺少目标的结果只解释操作准入。
- 诊断执行和候选路由保持现状。无诊断资格的角色无法请求。其他授权来源、字段结果按操作者已有披露资格返回，诊断不会修改授权。权限检查不借用角色列表、分配列表或审计读取权限。

## 目标目录与菜单

五个应用保留 41 个菜单节点，其中 29 个为页面。目录继承子页操作的并集并使用 ANY。目标目录包含 44 个资源、181 个启用操作和 80 条菜单操作关联。上传和其他派生入口的授权归属见前述设计。

资源表给出全部精确码的组成。将前缀与后缀连接即可得到完整操作码，例如 `iam-tenant:workbench:read`。标识的变动规则列在目录表之后。

### 平台治理

`iam-platform` 含 17 个资源和 77 项操作。

| 资源 | 操作码前缀 | 操作后缀 |
| --- | --- | --- |
| 全局账号 | `iam-platform:account:` | `create`、`delete`、`disable`、`enable`、`lock`、`lookup`、`read`、`reset-password`、`unlock`、`update` |
| 应用操作 | `iam-platform:action:` | `change-status`、`create`、`delete`、`read`、`update` |
| 应用目录 | `iam-platform:application:` | `change-status`、`create`、`delete`、`purge`、`read`、`update` |
| 租户应用开通 | `iam-platform:application-entitlement:` | `preview`、`read`、`update` |
| 平台授权审计 | `iam-platform:authorization-audit:` | `read` |
| 平台权限诊断 | `iam-platform:authorization-diagnosis:` | `run` |
| 平台授权委派 | `iam-platform:delegation:` | `create`、`preview`、`read`、`revoke`、`update` |
| 字典 | `iam-platform:dictionary:` | `create`、`delete`、`read`、`update` |
| 平台用户组 | `iam-platform:group:` | `create`、`delete`、`preview`、`read`、`update` |
| 平台成员 | `iam-platform:member:` | `change-status`、`create`、`read`、`remove`、`update` |
| 应用菜单 | `iam-platform:menu:` | `create`、`delete`、`read`、`update` |
| 租户套餐 | `iam-platform:plan:` | `create`、`read`、`update` |
| 应用资源 | `iam-platform:resource:` | `create`、`delete`、`read`、`update` |
| 平台角色 | `iam-platform:role:` | `change-status`、`create`、`delete`、`preview`、`publish-version`、`read` |
| 平台角色分配 | `iam-platform:role-assignment:` | `apply-version`、`create`、`read`、`revoke`、`update` |
| 共享角色 | `iam-platform:shared-role:` | `change-status`、`create`、`delete`、`preview`、`read`、`update` |
| 租户 | `iam-platform:tenant:` | `create`、`preview`、`read`、`update` |

| 上级目录 | 菜单显示名 | URL | routeName / 页面 viewPath | 操作条件 |
| --- | --- | --- | --- | --- |
| 一级目录 | 租户管理 | `/platform/iam/tenant` | `platform.iam.tenant` | 子页操作并集（ANY） |
| `platform.iam.tenant` | 租户列表 | `/platform/iam/tenants` | `platform.iam.tenants` | `iam-platform:tenant:read` |
| 一级目录 | 应用与配置 | `/platform/iam/config` | `platform.iam.config` | 子页操作并集（ANY） |
| `platform.iam.config` | 应用目录 | `/platform/iam/applications` | `platform.iam.applications` | `iam-platform:application:read` |
| `platform.iam.config` | 租户套餐 | `/platform/iam/plans` | `platform.iam.plans` | `iam-platform:plan:read` |
| `platform.iam.config` | 共享角色 | `/platform/iam/shared/roles` | `platform.iam.shared.roles` | `iam-platform:shared-role:read` |
| 一级目录 | 平台管理 | `/platform/iam/manage` | `platform.iam.manage` | 子页操作并集（ANY） |
| `platform.iam.manage` | 全局账号 | `/platform/iam/accounts` | `platform.iam.accounts` | `iam-platform:account:read` |
| `platform.iam.manage` | 平台成员与用户组 | `/platform/iam/personnel` | `platform.iam.personnel` | `iam-platform:member:read`, `iam-platform:group:read`（ANY） |
| `platform.iam.manage` | 角色与授权 | `/platform/iam/authorization` | `platform.iam.authorization` | `iam-platform:role:read`, `iam-platform:role-assignment:read`, `iam-platform:delegation:read`, `iam-platform:authorization-diagnosis:run`（ANY） |
| `platform.iam.manage` | 平台授权审计 | `/platform/authorization-audit` | `platform.iam.authorization.audit` | `iam-platform:authorization-audit:read` |
| 一级目录 | 系统设置 | `/platform/settings` | `platform.settings` | 子页操作并集（ANY） |
| `platform.settings` | 字典管理 | `/platform/settings/dictionaries` | `platform.settings.dictionaries` | `iam-platform:dictionary:read` |

### 组织治理

`iam-tenant` 含 13 个资源和 44 项操作。

| 资源 | 操作码前缀 | 操作后缀 |
| --- | --- | --- |
| 已开通应用 | `iam-tenant:application:` | `read` |
| 应用可用人群 | `iam-tenant:application-audience:` | `read`、`update` |
| 组织授权审计 | `iam-tenant:authorization-audit:` | `read` |
| 组织权限诊断 | `iam-tenant:authorization-diagnosis:` | `run` |
| 组织授权委派 | `iam-tenant:delegation:` | `create`、`preview`、`read`、`revoke`、`update` |
| 部门 | `iam-tenant:department:` | `create`、`delete`、`export`、`read`、`set-leaders`、`update` |
| 资源字段策略 | `iam-tenant:field-policy:` | `read`、`update` |
| 组织用户组 | `iam-tenant:group:` | `create`、`delete`、`preview`、`read`、`update` |
| 组织成员 | `iam-tenant:member:` | `change-status`、`create`、`export`、`read`、`remove`、`set-departments`、`update` |
| 组织角色 | `iam-tenant:role:` | `change-status`、`create`、`delete`、`preview`、`read`、`update` |
| 组织角色分配 | `iam-tenant:role-assignment:` | `create`、`read`、`revoke`、`update` |
| 组织设置 | `iam-tenant:settings:` | `read`、`transfer-owner`、`update` |
| 组织工作台 | `iam-tenant:workbench:` | `read` |

| 上级目录 | 菜单显示名 | URL | routeName / 页面 viewPath | 操作条件 |
| --- | --- | --- | --- | --- |
| 一级目录 | 工作台 | `/org/iam/workspace` | `org.iam.workspace` | 子页操作并集（ANY） |
| `org.iam.workspace` | 组织概览 | `/org/iam/workbench` | `org.iam.workbench` | `iam-tenant:workbench:read` |
| 一级目录 | 组织管理 | `/org/iam/organization` | `org.iam.organization` | 子页操作并集（ANY） |
| `org.iam.organization` | 成员与部门 | `/org/iam/members/root` | `org.iam.members.root` | 子页操作并集（ANY） |
| `org.iam.members.root` | 成员 | `/org/iam/members` | `org.iam.members` | `iam-tenant:member:read` |
| `org.iam.members.root` | 部门 | `/org/iam/departments` | `org.iam.departments` | `iam-tenant:department:read` |
| `org.iam.organization` | 用户组 | `/org/iam/groups` | `org.iam.groups` | `iam-tenant:group:read` |
| `org.iam.organization` | 组织设置 | `/org/iam/settings` | `org.iam.settings` | `iam-tenant:settings:read` |
| 一级目录 | 权限与应用 | `/org/iam/authorization/root` | `org.iam.authorization.root` | 子页操作并集（ANY） |
| `org.iam.authorization.root` | 角色与授权 | `/org/iam/authorization` | `org.iam.authorization` | `iam-tenant:role:read`, `iam-tenant:role-assignment:read`, `iam-tenant:delegation:read`, `iam-tenant:authorization-diagnosis:run`（ANY） |
| `org.iam.authorization.root` | 已开通应用 | `/org/iam/applications` | `org.iam.applications` | `iam-tenant:application:read` |
| 一级目录 | 安全与合规 | `/org/iam/compliance` | `org.iam.compliance` | 子页操作并集（ANY） |
| `org.iam.compliance` | 资源字段策略 | `/org/field-policies` | `org.iam.field.policies` | `iam-tenant:field-policy:read` |
| `org.iam.compliance` | 组织授权审计 | `/org/authorization-audit` | `org.iam.authorization.audit` | `iam-tenant:authorization-audit:read` |

### 开发者平台

`platform-developer` 含 4 个资源和 15 项操作。

| 资源 | 操作码前缀 | 操作后缀 |
| --- | --- | --- |
| 业务 ID 发号 | `platform-developer:id-allocation:` | `create`、`delete`、`read`、`update` |
| OAuth 2.0 客户端 | `platform-developer:oauth-client:` | `create`、`delete`、`read-detail`、`read-list`、`reset-secret`、`update` |
| 二维码 | `platform-developer:qr-code:` | `generate` |
| 社会化登录配置 | `platform-developer:social-config:` | `create`、`delete`、`read`、`update` |

| 上级目录 | 菜单显示名 | URL | routeName / 页面 viewPath | 操作条件 |
| --- | --- | --- | --- | --- |
| 一级目录 | 开发者平台 | `/platform/develop` | `platform.develop` | 子页操作并集（ANY） |
| `platform.develop` | 生成二维码 | `/platform/develop/qrcode` | `platform.develop.qrcode` | `platform-developer:qr-code:generate` |
| `platform.develop` | OAuth 2.0 客户端 | `/platform/develop/client` | `platform.develop.client` | `platform-developer:oauth-client:read-list` |
| `platform.develop` | 社会化登录配置 | `/platform/develop/social` | `platform.develop.social` | `platform-developer:social-config:read` |
| `platform.develop` | 业务 ID 发号 | `/platform/develop/id` | `platform.develop.id` | `platform-developer:id-allocation:read` |

### 安全中心

`platform-security` 含 7 个资源和 20 项操作。

| 资源 | 操作码前缀 | 操作后缀 |
| --- | --- | --- |
| 访问防护策略 | `platform-security:access-policy:` | `create`、`delete`、`read`、`refresh`、`update` |
| 账号锁定策略 | `platform-security:account-lockout-policy:` | `read`、`update` |
| 历史封禁记录 | `platform-security:block-event:` | `read` |
| 凭证策略 | `platform-security:credential-policy:` | `create`、`delete`、`read`、`update` |
| 登录失败防护 | `platform-security:login-failure-policy:` | `read`、`update` |
| 在线会话 | `platform-security:session:` | `read`、`revoke` |
| 会话并发策略 | `platform-security:session-concurrency-policy:` | `create`、`delete`、`read`、`update` |

| 上级目录 | 菜单显示名 | URL | routeName / 页面 viewPath | 操作条件 |
| --- | --- | --- | --- | --- |
| 一级目录 | 安全中心 | `/security` | `security.root` | 子页操作并集（ANY） |
| `security.root` | 凭证策略 | `/security/credential-policies` | `security.credential.policies` | `platform-security:credential-policy:read` |
| `security.root` | 账号保护 | `/security/account-protection` | `security.account.protection` | `platform-security:account-lockout-policy:read` |
| `security.root` | 访问防护 | `/security/access-protection` | `security.access.protection` | `platform-security:access-policy:read`, `platform-security:login-failure-policy:read`, `platform-security:block-event:read`（ANY） |
| `security.root` | 会话管理 | `/security/sessions` | `security.sessions` | `platform-security:session:read`, `platform-security:session-concurrency-policy:read`（ANY） |

### App 用户管理

`platform-member` 含 3 个资源和 25 项操作。

| 资源 | 操作码前缀 | 操作后缀 |
| --- | --- | --- |
| App 权限 | `platform-member:permission:` | `change-status`、`create`、`delete`、`read`、`update` |
| App 角色 | `platform-member:role:` | `assign-users`、`change-status`、`create`、`delete`、`read`、`read-permissions`、`set-permissions`、`update` |
| App 用户 | `platform-member:user:` | `create`、`delete`、`disable`、`enable`、`lock`、`read-detail`、`read-list`、`read-roles`、`reset-password`、`set-roles`、`unlock`、`update` |

| 上级目录 | 菜单显示名 | URL | routeName / 页面 viewPath | 操作条件 |
| --- | --- | --- | --- | --- |
| 一级目录 | App 用户管理 | `/member` | `member.root` | 子页操作并集（ANY） |
| `member.root` | App 用户 | `/member/users` | `member.user` | `platform-member:user:read-list` |
| `member.root` | App 角色 | `/member/roles` | `member.role` | `platform-member:role:read` |
| `member.root` | App 权限 | `/member/permissions` | `member.permission` | `platform-member:permission:read` |

### 资源编码变更

| 当前资源编码 | 目标资源编码 | 适用范围 |
| --- | --- | --- |
| `assignment` | `role-assignment` | 平台、组织 |
| `audit` | `authorization-audit` | 平台、组织 |
| `authorization` | `authorization-diagnosis` | 平台、组织 |
| `audience` | `application-audience` | 组织 |
| `entitlement` | `application-entitlement` | 平台 |
| `qrcode` | `qr-code` | 开发者平台 |
| `client` | `oauth-client` | 开发者平台 |
| `lockout-policy` | `account-lockout-policy` | 安全中心 |
| `security-policy` | `access-policy` | 安全中心 |
| `session-policy` | `session-concurrency-policy` | 安全中心 |

社交配置与发号操作改用开发者平台前缀。原安全操作统一切换到安全中心前缀。其他资源的编码继续使用目标目录表中的值。

### 操作编码和文案变更

| 旧后缀或行为 | 目标后缀 | 中文文案规则 |
| --- | --- | --- |
| 独立 `query`、`detail` | `read-list`、`read-detail` | 查看列表、查看详情。App 角色和权限的 query 改用 read。 |
| `status` | `change-status` | 启停平台角色、组织角色、应用、应用操作等对应对象。成员按界面状态显示「暂停成员」「恢复成员」。 |
| 角色分配及委派的 `delete` | `revoke` | 撤销角色分配、撤销授权委派。会话继续用「撤销在线会话」。 |
| 角色分配 `upgrade`、平台角色 `publish` | `apply-version`、`publish-version` | 应用角色版本、发布平台角色版本。 |
| `diagnose`、二维码单段操作 | `run`、`generate` | 运行权限诊断、生成二维码。 |
| `reset`、`reset-pwd` | `reset-secret`、`reset-password` | 重置 OAuth 2.0 客户端密钥、重置 App 用户密码。 |
| `roles:query`、`roles:set` | `read-roles`、`set-roles` | 查看 App 用户角色、配置 App 用户角色。 |
| `permissions:query`、`permissions:set`、`user:assign` | `read-permissions`、`set-permissions`、`assign-users` | 查看 App 角色权限、配置 App 角色权限、为 App 角色分配用户。 |
| `departments`、`leaders`、`owner-transfer` | `set-departments`、`set-leaders`、`transfer-owner` | 调整成员任职、设置部门负责人、转交组织拥有者。 |
| 安全策略编辑内的刷新、历史封禁 | `access-policy:refresh`、`block-event:read` | 刷新访问防护策略、查看历史封禁记录。 |
| 会话并发策略的通用 update | `create`、`update`、`delete` | 创建、编辑、删除会话并发策略。 |
| App 用户资料编辑中的启停、锁定中的解锁 | `enable`、`disable`、`unlock` | 启用 App 用户、停用 App 用户、解锁 App 用户。 |
| App 角色和权限资料编辑中的启停 | `change-status` | 启停 App 角色、启停 App 权限。 |
| 组织角色更新 | `update` | 保存组织角色权限。 |
| 工作台借用应用读取 | `workbench:read` | 查看组织工作台。 |
| 已停用的组织角色 `upgrade` | 移出新种子 | 组织角色权限保存后即时生效。 |

其余后缀保持原操作语义，展示名根据目标资源调整。「平台超级管理员」系统角色沿用 `role_admin`，界面显示「超级管理员」。「组织治理」系统角色沿用 `tenant-governance`，显示「组织治理系统角色」，和实际的「组织拥有者」身份区分。

## 入口覆盖与操作边界

- 旧 PMS 的页面和小程序接口前缀先按调用方核对。正式管理台使用 IAM 动态菜单及已登记的静态入口。小程序注册和个人资料接入 Member 当前网关路径，OAuth 入口按真实部署验证。已退役页面、组织角色版本升级、测试样例及旧接口按清单单列。
- 两端隐藏表格路由、我的下载、登录、改密、个人资料、帮助和内部服务入口全部列入覆盖矩阵。每条业务路由登记方法、路径、执行模式、资源键、操作或身份条件。含多个授权条件的入口记录实际 ANY、ALL、按模式或按字段的规则。
- 诊断、预览按只读操作登记，即使 HTTP 方法是 POST。所有写入和上传提交重新求值，读取沿用各消费服务已有的实时或热窗口契约。二维码生成标记为纯前端受保护功能，以页面操作作为入口。只允许本人、内部服务及认证协议的入口分别登记专用身份条件。
- 成员新增、重新加入和创建模式导入共用成员创建授权。导入更新按实际资料或任职变化检查写操作，导出独立。成员、部门表格的任务和文件继续校验发起人、状态、来源操作及对象范围。本人下载按任务归属和来源授权签发。
- App 用户的启用、停用、锁定、解锁各自独立。App 角色、App 权限的更新路由保留，提交状态字段要求 `change-status`，提交资料字段要求 `update`，混合写入校验两项。App 用户资料更新请求使用资料白名单，状态只能走对应状态操作。
- 访问防护中的路径分组、限流、名单、挑战和违规升级继续共用策略资源的 CRUD。刷新操作要求策略读取与刷新两项资格。封禁历史读取单列资源，安全事件统一查询另行定义。会话并发策略分别校验读取、创建、编辑和删除。会话客户端候选要求开发者平台的 OAuth 2.0 客户端列表权限。
- OAuth 2.0 客户端管理继续使用现有接口路径，改用可信平台域、明确资源键、精确操作及 ALL 授权。管理操作校验与普通 OAuth 客户端认证分别执行。

## 编码、路由和角色的切换

| 对象 | 切换规则 |
| --- | --- |
| 目录编码与 ID | 正式种子直接生成目标编码。取消旧码运行时别名、双写和兼容操作记录。测试夹具按自然键查新目录，再使用本次初始化的 ID 建立引用。停用的 `iam-tenant:role:upgrade` 从新库种子移除。 |
| 前后端引用 | 在同一联调版本同步 Java 操作常量与注解、资源注册、范围和字段绑定、Security 与 Member SDK、Auth 客户端校验、前端权限常量、帮助文案、种子和契约测试。代码扫描应发现运行路径残留的旧码，历史归档按原内容保留。IAM 应用编码与 BFF 宿主 appId、OAuth clientId 分别管理。 |
| 菜单及路由 | 保留已有正式页面的 path、routeName 和 viewPath。目录标题与图标可调整。平台人员改名、两域新增诊断 Tab，工作台门禁变更，访问防护新增历史封禁的独立门禁。路由直接访问与菜单导航共享同一权限事实。 |
| API | 现有业务方法与路径继续使用。明确的替换是两条通用上传入口。组更新和预览的 `selection` 支持省略。图片关联写入按新凭证契约提交。App 角色与权限启停继续使用现有更新路径，后端按提交字段判定操作。 |
| 普通角色 | 根据旧码到目标码的清单，通过角色 API 在隔离库重建等价角色与分配，保留域、范围、字段、期限、来源及委派上限的测试语义。现有库无须做升级 SQL。新拆出的权限在测试角色中显式选择，不按名称或操作前缀自动推导。 |
| 系统角色 | 冷启动种子重建治理目录及版本。平台超级管理员和组织拥有者仍由当前可信身份动态取得域内有效操作。组织普通角色只读或写入的职责按最新目录授予。 |
| 联调与回退 | IAM 及相关服务、两管理台用同批目录契约。重启或切换环境后重新登录和 bootstrap，重新拉取菜单及能力。原测试库保留供比较，回退切回旧代码和旧库配对。任何已有本地库重建须明确选择目标和备份。 |

## 实施任务与验收

| 顺序 | 任务 | 完成条件 |
| --- | --- | --- |
| 1 | 固化目录及授权覆盖矩阵 | 每个正式操作唯一归属资源。已实现的管理接口、页面、Tab、操作按钮和后台任务都能对应授权规则。补齐评审稿所列 15 个映射和 23 个过期实现标记。验证退役入口与业务入口区分。 |
| 2 | 同步编码、种子、资源注册和契约 | Java、前端、路由契约、正式种子及能力注册逐项对齐。跨服务精确码、完整资源键和操作执行模式一致。更新静态校验、测试角色和建库文档统计。 |
| 3 | 补用户组、工作台与权限诊断 | 两端角色工作区和平台人员页按 ANY 与 Tab 单独校验。平台用户组、组织工作台、诊断均通过后述范围与授权验收。 |
| 4 | 收紧上传并切换所有调用 | 两服务图片用途与业务资源一致。前端成功保存后可显示新图。直接访问、任意路径、跨目标、字段禁止、撤权和异常重试通过验收。 |
| 5 | 验证拆分的写操作与命名 | 读写分离、状态、锁定、刷新、会话策略、审计及按钮文案一致。检查普通角色无越权，管理员完整路径可用。 |
| 6 | 双端与小程序联调 | 隔离库验证初始化和关联完整性。平台端、组织端逐页验证导航、权限、深链与撤权。小程序按部署路径验证注册、登录及本人资料。验收后更新各仓库 current，并归档 SDD change。 |

### 验收用例

| 能力 | 必须通过的场景 |
| --- | --- |
| 上传 | 有操作和字段资格可上传并保存。仅能读、范围外、跨域、跨成员、错误用途、伪造 bucket 或路径、错误格式、超限及伪造凭证均拒绝。上传后撤权、对象版本变化及同一凭证并发提交不得绕过检查。业务回滚后可安全重试或等待过期清理。 |
| 平台用户组 | 成员只读、组只读、两者都可读及无权四类角色分别验证菜单和请求。指定组不可见时列表、计数、详情、预览和编辑一致拒绝或过滤。用户组成员列表只返回成员范围交集。只编辑组资料可保留关联，提交不可见成员的完整替换被拒绝。 |
| 组织工作台 | 只有应用读取时菜单和 API 均无工作台资格。仅有工作台读取可进入，缺少的指标不返回。分别授予成员、部门、组的指定范围后验证计数。隐藏页面和受人群限制的应用按现有可见性口径计数。平台或跨组织请求拒绝。 |
| 权限诊断 | 单独授予诊断可进入工作区并诊断本人，不请求其他 Tab 的接口。选择他人需成员读取与数据范围，指定业务对象需目标读取和范围。伪造跨域、跨组织、跨应用同名资源及错配操作均拒绝。角色查看或审计查看不能代替诊断操作。 |
| 目录与切换 | 新库菜单、操作、资源、角色与 SQL 引用一致。相同页面 URL 能打开正确组件。旧运行码不再命中。重新创建的只读、受限和管理角色保留测试意图，新增操作只按明确授权出现。 |
| 拆分操作 | 配置编辑者不能刷新策略、查封禁历史或删除未授权的并发策略。App 用户资料编辑者不能启停或解锁。App 角色及权限的混合写入缺任一操作都拒绝。 |

实施前需确认本方案的目录编码、工作台显式授权、诊断主体范围、上传凭证方案及拆分操作。确认后按模板建立 active change，把需求、设计和任务拆为正式工件，经批准开工。[SDD 工作流](../../../specs/README.md) · [前次盘点与证据](RESOURCE-MENU-REVIEW.md)
