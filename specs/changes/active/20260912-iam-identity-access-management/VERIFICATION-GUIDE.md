# IAM 联调操作手册

> 2026-09-21。给人在独立环境按页执行。场景编号、身份键、验收映射仍以同 change 的 `TEST-DATA.md` 为准（前端仓副本为 `sources/BACKEND_TEST_DATA.md`）。本文件不勾选 A/F/L/U 父任务；数据准备成功、列表能打开都不算产品验收通过。
>
> 两端共用同一套 TD 编号与 runId。本手册在前端仓的副本为 `sources/BACKEND_VERIFICATION_GUIDE.md`。

## 1. 功能测试导读

本轮要证明的不是「超管能点开菜单」，而是：**不同身份在对应域里，允许的事能做成、不允许的事被拦住，范围/字段/期限按当前会话生效。**

| 功能块 | 测什么 | 主身份 | 主入口 | TD |
|---|---|---|---|---|
| 冷启动 | 空库只出治理目录和受控平台账号，口令不在 SQL 里 | 启动日志里的 `platform` | Auth 直连，不传 `org` | TD01 |
| 登录与身份 | 平台永不选组织；租户必须带组织；同一账号多身份互不影响 | dual、platform-governor、owner-a | 四站浏览器 / Auth | TD02、TD17 |
| 组织与开通 | 创建组织是最小实体；开通应用 ≠ 业务授权 | platform-governor、owner-a | 平台「租户管理」；租户「应用管理」 | TD03 |
| 菜单与人群 | 同时满足域、开通、人群、页面操作才出现入口 | owner-a、ordinary-a、no-access | 租户「应用管理」「工作台」 | TD04 |
| 角色与差异 | 共享可直接分配；定制只存差异；REMOVE 不是全局禁止 | platform-governor、owner-a | 平台「共享角色」；租户「角色与授权」 | TD05 |
| 版本升级 | 新版本不自动扩权；冲突未解不能提交 | 同上，用独立对象 | 租户角色详情「升级」 | TD06 |
| 成员范围 | 只读看得到研发及下级、手机脱敏、不能写/导出 | reader-a、editor-a | 租户「成员与部门」 | TD07 |
| 部门任职 | 所属部门授权随任职变，指定部门授权不变 | owner-a、editor-a、multi-dept-a | 「成员与部门」调部门 | TD08 |
| 组与委派 | 组预览引用；委派上限内可分配，越限整批失败 | owner-a、grantor-a | 「用户组」「角色与授权」授权管理员 Tab | TD09 |
| 通讯录 | DIRECTORY 可见树与后台管理部门树不是同一套 | ordinary-a、reader-a | 「通讯录」 | TD10 |
| 字段策略 | MASKED/HIDDEN 不进表单、不回传、不能按原值搜 | reader-a、editor-a、owner-a | 「成员权限」字段 Tab | TD11 |
| 期限与失效 | 到期/撤销/停用后立即不可用 | owner-a、editor-a | 授权记录；关键写 | TD12 |
| 诊断与审计 | 真实写之后才有原因链；无模拟执行、无审计导出 | owner-a、reader-a | 授权页诊断；「审计」 | TD13 |
| 成员导出 | 创建→轮询 SUCCEEDED→下载；失败/过期不当成功 | owner-a、reader-a | 「成员与部门」导出 | TD14 |
| 所有者转交 | 治理权转走，独立业务授权留下 | owner-transfer-src/dst | 专用组织「组织设置」 | TD15 |
| 账号与辅助 | 本人资料/改密、账号启停锁定、字典/发号/社交/OSS | platform-governor、dual | 头像菜单；平台人员/账号；原配置页 | TD16 |
| 四站登录 | host-only Cookie、CSRF、ticket 一次性、双域并行 | dual 等 | 四个 `*.localhost` origin | TD17 |
| 全入口 | 选择器翻页、预览后改草稿、批量失败不报部分成功 | 多身份 | 已实现管理页 | TD18 |

当前工具能力（必须先读，避免用空数据硬测）：

- **已能**：`prepare` / `build` 经真实 `/iam/v1` 创建 `iam-test-*` 账号、租户 A/B、部门、成员、组、一条共享角色和分配；口令写入 `*.secrets.json`。
- **尚未由 D02 自动铺齐**：通讯录/字段策略完整规则、动态期限、250+ 导出成员、升级冲突对象、专用转交组织的完整授权图。这些场景或手工补数，或等到 D03 后再测，**不要把「页面能打开」写成通过**。
- **禁止**：把 `seed-manual-verification.sql` 的 `platform`/`owner` + `password` 当成 D02 身份；禁止 SQL dump 替代 `build`；禁止向未登记的开发库 `reset`。

## 2. 本机站点

`require-https: false` 的 DEV 四站（`*.localhost` 不必改 hosts）：

| 站点 | Origin | 用途 |
|---|---|---|
| 租户管理台 | http://tenant.localhost:5798 | 组织 IAM 页面 |
| 租户登录 | http://tenant-login.localhost:1798 | 只处理租户成员 |
| 平台管理台 | http://platform.localhost:5799 | 平台 IAM 页面 |
| 平台登录 | http://platform-login.localhost:1799 | 只处理平台成员，无选组织 |

后端常用端口：Auth `5100`，Gateway `7980`，BFF `5400`。浏览器只走管理台/登录站，`/api` 由 Vite 代理到 Gateway。不要用 `http://127.0.0.1:端口` 当注册 origin。

## 3. 环境准备（逐步）

工作目录默认：后端仓根 `ingot/`。库名、账号按你的 `config.local.json`，下面以 `iam_test` / `iam-test-local` / runId `demo` 为例。

### 3.1 独立空库

1. 准备一台本机或隔离 Docker 的 MySQL 8.0.16+（或 8.4），会话时区 UTC。
2. 新建空库，例如 `iam_test`。**不要**指向现网或日常 `ingot_core`。
3. 独立 Redis 命名空间，例如 `iam-test`。多实例用例以后共用这一对库/Redis。

### 3.2 权威 DDL（不要跳步、不要混种子）

在目标库按顺序执行（路径相对后端仓根）：

1. `databases/iam/001_identity.sql`
2. `databases/iam/002_catalog_role.sql`
3. `databases/iam/003_assignment_delegation.sql`
4. `databases/iam/004_policy_audit_migration.sql`
5. `databases/iam/005_auxiliary.sql`
6. `databases/iam/007_member_export.sql`
7. `ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/resources/sql/account_lock_state.sql`
8. `ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/add_password_history.sql`
9. `databases/iam/006_bootstrap.sql`

到这里库里只有治理目录，没有测试账号。`006` 可重复执行且不覆盖人工改过的行。

**二选一，不要两个都做：**

- **联调 D02（推荐）**：打开 IAM 的 `ingot.iam.bootstrap.enabled=true`，启动一次 IAM，从 WARN 日志取出受控平台账号口令（默认登录名 `platform`），登录后立刻改密。然后用第 3.6 节工具 `build`。
- **历史小型 Bruno**：额外执行 `databases/iam/seed-manual-verification.sql`，得到固定口令 `password` 的 `platform` / `owner`。这套账号**不能**当作 `iam-test-*` 场景数据。

### 3.3 启动后端

按现有 `deploy/services` / Nacos 启动 Auth、Gateway、IAM、BFF（及登录所需 Security）。核对：

- IAM 数据源指向 `iam_test`，不是旧库名。
- Nacos 出现 `in-service-iam`。
- 经 Gateway 能打到 `/iam/v1/...`（旧 `/v1/platform/org/tenant` 已删除，打到旧路径算失败）。

### 3.4 启动前端四 App

在 `ingot-admin` 分别启动（端口与 BFF 注册表一致）：

- `apps/admin` → `tenant.localhost:5798`
- `apps/auth` → `tenant-login.localhost:1798`
- `apps/admin-platform` → `platform.localhost:5799`
- `apps/auth-platform` → `platform-login.localhost:1799`

登录页样式由 `@ingot/auth-plugin` 入口拉取。若输入框是原生白底、没有左侧 banner，硬刷新或重启对应 auth App。

### 3.5 登记测试工具配置

```bash
cd /path/to/ingot
cp tools/iam/test-data/config.example.json tools/iam/test-data/config.local.json
```

`config.local.json` 必须：

- `environmentId` 非空；`registered`、`independent` 均为 `true`。
- `database.name` 就是刚才的空库；`user`/`password` 按你的 MySQL（密码可用环境变量，不要提交该文件）。
- `gatewayBaseUrl` / `authBaseUrl` / `bffBaseUrl` / 四站 origin 与真实进程一致。
- `bootstrapUsername` 与冷启动登录名一致（默认 `platform`）。
- `credentialEnv.platform-governor` 指向**已改密后**的平台治理口令环境变量，例如 `IAM_TEST_PASSWORD_PLATFORM_GOVERNOR`。
- `owner-a` / `owner-b` **不要**先 export 成 `password`。未设置时，`build` 会生成一次性口令并写入 secrets 文件。若环境变量已设置，工具用环境变量，**不再**重置 secrets 里的值。

该文件和 `tools/iam/test-data/runs/`、`*.secrets.json` 均不入库。

### 3.6 导入测试数据

```bash
export IAM_TEST_PASSWORD_PLATFORM_GOVERNOR='<改密后的平台口令>'
# 不要设置 OWNER_A/B，除非你明确要固定口令

python3 tools/iam/test-data/iam_test_data.py prepare --config tools/iam/test-data/config.local.json --run-id demo
python3 tools/iam/test-data/iam_test_data.py build   --config tools/iam/test-data/config.local.json --run-id demo
python3 tools/iam/test-data/iam_test_data.py verify  --config tools/iam/test-data/config.local.json --run-id demo
```

成功时 stdout JSON 含 `runId`、对象映射、`secretsFile` 文件名。报告里只有身份用途和环境变量名，**没有密码**。

口令文件：

`tools/iam/test-data/runs/<environmentId>/demo.secrets.json`

键为身份键（`owner-a`、`dual`…），值为本次生成的口令。登录名规则：`iam-test-<身份键>`，例如 `iam-test-owner-a`。租户 ID 在同目录 `demo.json` 的 objects 里，登录租户域时 Auth 表单字段是 `org`（不是 `tenant`）。

同一 `run-id` 再 `build` 会复用已有对象，不覆盖你在界面上改过的数据。配置或 DDL 指纹变了会拒绝静默改回，需要：

```bash
python3 tools/iam/test-data/iam_test_data.py reset --config tools/iam/test-data/config.local.json --run-id demo --confirm-reset
```

`reset` **只清清单，不 DROP 库**。要彻底重来：空库重跑 3.2，再 `reset` + `prepare` + `build`。

组织所有者登录失败（`S0400 用户名或密码错误`）时：确认 `IAM_TEST_PASSWORD_OWNER_A` 未设置；删掉 `demo.secrets.json`（保留 `demo.json`）后再 `build`，让工具重新 resetPassword。Auth 把「用户不存在」和「密码错」都显示成同一句，不要只猜密码。

### 3.7 每条记录要写什么

场景版本 / runId / UTC t0、代码与契约版本、账号类别、对象自然键与 ID、步骤、预期与实际、脱敏截图路径、是否重建。期限、撤权、并发、导出失败、BFF 重放必须写「动态步骤已执行」，不能只贴静态行。

## 4. 身份与登录步骤

| 身份键 | 登录名 | 去哪登 | 说明 |
|---|---|---|---|
| platform-governor | 配置的 bootstrap 名，默认 `platform` | 平台登录站 | 仅平台治理；口令来自冷启动改密，不是 secrets |
| platform-reader | `iam-test-platform-reader` | 平台登录站 | 平台只读 |
| owner-a / owner-b | `iam-test-owner-a` / `b` | 租户登录站，选组织 A/B | 组织所有者 |
| dual | `iam-test-dual` | 两站都登；租户侧选 A 或 B | 双域并行 |
| reader-a | `iam-test-reader-a` | 租户 A | 研发只读 |
| editor-a | `iam-test-editor-a` | 租户 A | 可编辑研发 |
| grantor-a | `iam-test-grantor-a` | 租户 A | 受限分配 |
| ordinary-a | `iam-test-ordinary-a` | 租户 A | 普通通讯录 |
| multi-dept-a / no-dept-a | 对应 `iam-test-*` | 租户 A | 任职边界对象，也可登录核对能力 |
| no-access / suspended / disabled | 对应 `iam-test-*` | 按场景 | 破坏性对象，不要拿来做日常浏览 |
| owner-transfer-* | 对应 `iam-test-*` | 仅 TD15 专用组织 | 不要转交租户 A 的 owner-a |

### 4.1 平台登录（TD17 主路径）

1. 浏览器打开 http://platform.localhost:5799 （或任意需登录的平台管理路径）。
2. 应跳到 http://platform-login.localhost:1799/oauth2/challenge?... ，**没有**「选择组织」。
3. 输入平台账号与口令，提交。
4. 回到平台管理台后看顶栏：当前是平台身份，`tenantId` 为空。
5. 反例：在平台登录站即使用 dual 的租户口令，也不应降级进组织。无平台成员应失败，而不是弹出租户列表。

### 4.2 租户登录

1. 打开 http://tenant.localhost:5798 。
2. 跳到租户登录站。输入 `iam-test-owner-a` 与 secrets 中的口令。
3. 零组织：失败，不降级去平台。单组织：BFF 自动完成。多组织（dual）：出现选择列表，选 A 或 B 后再进入。
4. 进入后顶栏显示当前组织名称；菜单只有租户四个一级目录。
5. 用平台治理账号走租户登录站应失败（没有租户成员）。

### 4.3 双域同时在线

1. 同一浏览器先完成平台登录（governor 或 dual 的平台身份）。
2. 新标签打开租户管理台，用 dual 完成租户登录。
3. 两边菜单、请求互不影响。在租户侧退出，平台标签仍保持登录。
4. 切组织：走租户侧切换，成功后旧列表/草稿应清空；取消则原会话保留。

## 5. 分场景操作步骤

下列路径是治理菜单种子中的页面名。实际 URL 以 bootstrap 菜单为准。每个场景先写「介绍」，再写点击步骤。

### TD01 冷启动

**介绍：** 证明全新系统能从空库起来：目录来自 `006`，第一个平台账号来自启动器，口令不在仓库 SQL 里。重复启动不得改口令。这是进程级测试，前端只使用其产物环境。

**步骤：**

1. 空库只跑到 `006`，不要执行人工种子，不要先 `build`。
2. `ingot.iam.bootstrap.enabled=true` 启动 IAM，复制 WARN 行中的初始口令（记录时打码）。
3. Auth 密码登录，`user_type=0`，**不传 `org`**。应要求改密。
4. 改密后再调一个平台只读接口（或打开平台管理台只读页）。
5. 保持开关再启动一次：账号/平台成员/治理授权行数不变，口令仍是你改过的。
6. 关掉开关再启动：日志无冷启动动作。

**不要：** 用 `seed-manual-verification.sql` 的 `password` 声称冷启动通过。

### TD02 身份隔离

**介绍：** 同一账号可以有平台成员和多个租户成员，但一次会话只有一个当前身份。暂停平台成员不影响租户；移出组织不影响全局账号；禁用账号则所有域都进不去。跨租户写必须 403/404。

**步骤：**

1. dual 登平台：应能进「平台人员」「租户管理」，不能进租户「成员与部门」。
2. 退出后 dual 登租户选 A：应能进组织管理，不能创建平台租户。
3. 再选 B：A 的成员数据不得出现。
4. 用 owner-a 打开租户 A「成员与部门」，尝试改 B 的成员（换组织或直改 ID）：应拒绝，B 数据不变。
5. 用平台会话 `PATCH` 租户成员状态（或界面若露出按钮）：应拒绝。
6. 破坏性：对 **suspended 专用对象** 暂停平台成员，再用该账号登平台失败、登租户 A 仍可（若该账号同时有 A 成员）。对 **disabled** 禁用全局账号后，所有登录失败。
7. 常规 dual / owner-a 不要拿来做暂停/禁用。

### TD03 组织与开通

**介绍：** 创建组织只产生所有者、根部门、一条治理分配、baseline 开通，不复制平台角色/菜单。开通业务应用只让应用「可用」，不自动给业务操作权。

**平台步骤（governor）：**

1. 「组织与租户」→「租户管理」→ 创建。
2. 向导：组织资料 → 指定所有者（已有账号，不要猜别人是否在别的组织）→ 基础应用/套餐 → 预览。
3. 预览中不得出现「复制角色模板」。提交前租户行数不变（可先对已有名称走预览）。
4. 再创建一个一次性组织（不要反复重建租户 A）。提交后两个组织 ID 不同。

**租户步骤（owner-a）：**

1. 「权限与应用」→「应用管理」，开通一个非 baseline 业务应用（若目录里有）。
2. 用 ordinary-a 或未授权成员打开工作台：不应出现该应用业务入口。
3. 回到「角色与授权」给该成员显式分配后再刷新：入口出现，且审计有授权记录。

### TD04 菜单与人群

**介绍：** 工作台 URL 来自服务端菜单。未开通、过期、停用、不在人群、页面操作不满足，父目录也应隐藏。

**步骤：**

1. owner-a 打开「应用管理」，给测试应用配置人群（组或部门）。
2. ordinary-a 打开「工作台」，记录可见入口。
3. 把 ordinary-a 移出人群，强制刷新（或重新登录）：该入口消失。
4. no-access 登录：不应看到业务应用入口。
5. 平台 governor 看「应用目录」菜单 ANY/ALL/OPEN 说明，不把应用 code 填进浏览器当 URL。

数据未含过期开通时，本项只做到「人群移出后刷新」；过期/停用标阻塞，不写通过。

### TD05 角色差异

**介绍：** 共享角色在租户侧只读，操作为「分配使用 / 基于此定制」。定制三种差异（新增、移除、替换范围）只存在该角色上；「恢复平台设置」清空差异。移除文案不能写成「此人被全局禁止」。

**步骤：**

1. governor：「应用与配置」→「共享角色」，发布或确认 v1 只读。
2. owner-a：「角色与授权」→ 角色 Tab → 分配该共享 v1 给某成员。
3. 「基于此角色定制」：做 ADD / REMOVE / REPLACE_SCOPE，预览里应能区分「来自平台 / 本组织新增 / 移除 / 范围已调整」。
4. 发布。再用另一个角色把被 REMOVE 的操作授回：该成员应仍能做该操作。
5. 「恢复平台设置」：差异清空，已分配的共享授权引用仍在。

### TD06 版本升级

**介绍：** 平台发 v2 不会改写已引用 v1 的授权。升级要处理三方冲突，未解冲突不能提交；默认不勾选既有授权。请用独立派生角色，不要破坏 TD05 主路径。

**步骤：**

1. 平台发布共享 v2（删除某操作或改范围）。
2. 租户打开基于 v1 的定制角色 → 升级预览。应列出冲突。
3. 不处理冲突点提交：应失败。
4. 逐项处置冲突（替换范围必填 scopes），只勾选要升级的授权。
5. 提交成功后，未勾选的授权仍停在 v1。
6. 用第二个浏览器/会话并发撤销其中一条，或提交过期 version：409，草稿保留，再预览而不是空白重来。

无独立升级对象时跳过，并在记录里写「D03/手工对象未备」。

### TD07 操作与范围

**介绍：** 列表过滤在服务端。reader-a 只看研发及下级，手机是脱敏，没有创建/编辑/移出/导出。editor-a 能改研发，不能改销售；「能看全组织」不能拼出「能改销售」。

**步骤（reader-a）：**

1. 「组织管理」→「成员与部门」。左侧树、右侧表应只有研发范围对象。
2. 打开一条详情：手机为脱敏，不是可复制原值；无编辑/移出/导出按钮（或按钮禁用且有原因）。
3. 搜索框回车查询；清空即重查。不要出现按隐藏原值搜到人。
4. 尝试导出或直打导出 API：403。

**步骤（editor-a）：**

1. 编辑一名研发成员，保存成功。
2. 打开销售成员：可看（若有查看范围）但不能编辑。
3. 对照 owner-a 确认销售在所有者下可编辑，避免把「没数据」当成授权正确。

### TD08 部门任职

**介绍：** 「所属部门」类授权跟成员当前部门走；「指定管理部门」不跟任职变。无部门成员打不中部门范围。非空部门不能删。

**步骤：**

1. owner-a 打开 multi-dept-a 详情 → 调整部门（整体替换，保留一个主部门）。
2. 用 editor-a 再看成员列表：所属范围里该人应随新部门出现或消失；指定部门那条授权的对象集合不变。
3. 关掉某授权的「包含下级」，下级部门成员应退出范围。
4. 删除非空部门：应返回业务原因，部门仍在。
5. 过期 `expectedVersion` 再保存任职：409。
6. 不要对所有者做暂停/移出（应 `ObjectInUse`）。

### TD09 组与委派

**介绍：** 组改动前要 preview 引用。委派只给「允许给别人授权」的上限，不自动给业务权。越版本/对象/范围/期限或拼接两委派，整批失败。

**步骤：**

1. owner-a「用户组」打开已有组：看有效人数、成员来源、引用位置。改成员或部门来源前应出现预览。
2. 「角色与授权」→ 授权管理员 Tab：查看 grantor-a 的白名单、接收人、范围与期限上限。
3. 用 grantor-a 登录，分配允许版本给 ordinary-a，期限在上限内：预览后提交成功。
4. 故意越上限（更宽范围、更长期限、不在白名单的版本）：提交失败，已有授权不变。
5. 扩大组后再用被授权人访问：不能靠扩组间接得到额外业务权（按预览影响解释）。
6. 收窄或撤销委派后，派生授权失效。

### TD10 通讯录

**介绍：** 工作台「通讯录」走 `purpose=DIRECTORY`，与后台「成员与部门」的管理部门树分开。默认规则 + 允许 − 禁止；本人基础资料仍可见。隐藏分支不能点进去，祖先骨架不算隐藏部门的人数。

**步骤：**

1. ordinary-a 打开「工作台」→「通讯录」：只有树、搜索、只读详情，没有创建成员。
2. 点开可见成员详情。尝试直打管理部门 purpose：应被拒绝或与通讯录结果不同。
3. reader-a 再走一遍，范围应更窄。
4. owner-a 对照后台成员树，确认通讯录没有把隐藏子树统计进去。

策略规则未由 D02 写入时，只记录「无创建按钮 + purpose 隔离」；完整允许/禁止预览标未齐。

### TD11 字段策略

**介绍：** 字段矩阵在「成员权限」字段 Tab。脱敏值不能填回保存；HIDDEN 不能排序检索；FULL 也不等于自动有更新 ACTION。

**步骤：**

1. owner-a 打开「安全与合规」→「成员权限」→ 字段权限，配置查看者/目标/手机 MASKED 或 HIDDEN，保存前预览。
2. reader-a 打开同一目标详情：看不到原值。
3. editor-a 打开编辑：表单没有脱敏框；若强行提交 `***` 应失败。
4. 导出列（若有权）与当前字段权限一致。

### TD12 期限与失效

**介绍：** 有效期按服务端时间。过期、撤销、停用应用/角色后，热缓存最多约 30 秒内切断。静态 `expiresAt` 行不能当通过。

**步骤：**

1. 执行前新建一条相对现在很短的授权，记下服务端时间。
2. 生效前用被授权人调关键写：应拒绝。
3. 生效窗口内：允许。
4. 等到过期或拨测时钟后：拒绝。
5. 撤销后再调：立即拒绝。
6. 多实例/Redis 故障见 TEST-DATA 第 6 节 XC01/XC02，缺条件就记阻塞。

D02 不写动态期限，本项默认未执行。

### TD13 诊断与审计

**介绍：** 诊断解释「为什么能/不能」，来源链受操作者范围限制。审计来自真实写，禁止插库假行。无审计导出按钮。

**步骤：**

1. 先做一次真实授权或成员写（TD03/TD05/TD09）。
2. owner-a 在「角色与授权」打开诊断，对允许对象与拒绝对象测同一操作。
3. reader-a 打开诊断：不能看到越界对象详情。
4. 「安全与合规」→「审计」：能打开刚那次写的详情（before/after），没有导出。

### TD14 成员导出

**介绍：** 导出是异步任务。必须 `PENDING/RUNNING` 继续等，`SUCCEEDED` 才下载。`FAILED`/`EXPIRED`/404/下载 503 都不能当成功空表。reader-a 不能创建任务。完整行数要求 ≥250，当前 D02 未生成 export-batch。

**步骤（数据齐备后）：**

1. owner-a 成员页点导出，记下任务 id。
2. 状态轮询到 SUCCEEDED，下载后核对行数与字段权限。
3. reader-a 点导出：失败。
4. 任务 RUNNING 时撤权，再下载应拒绝。

成员不足 250 时只可做「拒绝创建 / 失败态展示」，不能勾完整导出验收。

### TD15 所有者转交

**介绍：** 只在专用组织上转交，保留 owner-a 作为租户 A 所有者。治理授权随所有权走，旧所有者身上独立业务授权留下。陈旧版本 409。

**步骤：**

1. 用 owner-transfer-src 登录**专用组织**（不是 A）。
2. 「组织设置」→ 转交，远程选择 owner-transfer-dst（必须是该组织有效成员）。
3. 确认文案，提交。src 失去治理菜单，dst 获得。
4. 检查 src 仍保有事先加的独立 SHARED 授权。
5. 用旧 version 再转交：409，草稿还在。
6. 不能手填一个通讯录里看不见的成员 ID。

### TD16 账号与辅助

**介绍：** 全局账号启停/锁定/重置走平台治理；本人资料和改密走头像菜单。字典/发号/社交/OSS 已迁 IAM 路径，菜单可能仍在原配置入口。

**步骤：**

1. dual 或任意登录用户：头像 → 资料，改显示名后保存（带 version）；改密走独立对话框。
2. governor：「平台管理」→「平台人员」成员/组 Tab；全局账号若另有页面则测锁定/解锁/重置/启停。不要停用受控 `platform` 账号。
3. 打开字典、发号、社交配置页，做一次只读或可回滚的写。OSS 上传走 `/api/iam/v1/oss/upload`。
4. platform-reader：上述写按钮应无或提交 403。
5. 新页面不得再把 `/api/pms` 当成功路径。

### TD17 四站登录（浏览器）

**介绍：** 接口 `build` 用的 Auth password grant **不等于** 本场景通过。要看 Cookie 是否 host-only、ticket 是否一次性、CSRF/Origin 失败是否拒绝、深链是否只恢复管理台相对 path。

**步骤：**

1. 清掉四个 origin 的 Cookie 后重跑 4.1–4.3。
2. 平台无租户账号直接完成；租户零/单/多候选见 4.2。
3. 登录成功后的 ticket 再刷一次完成页：应失败。
4. 非法绝对 URL、错入口、错 Origin：应拒绝，错误页不能开放跳走。
5. 直接打开登录站且无事务：只回配对管理台 `/auth/start`。
6. 管理台 401 只进本站 `/auth/start`。
7. Member 原登录协议抽空回归，确认没被改写。

缺 HTTPS/四产物/BFF 注册时记 XC06 阻塞，不拿 Vite 单站冒充四站验收。

### TD18 规模与全入口

**介绍：** 每个已实现管理操作都要有「谁、在哪、成功/失败长什么样」。选择器超过一页仍能选中；预览后改草稿必须重新预览；批量失败不能 toast 部分成功。

**步骤：**

1. 按 TEST-DATA 第 5 节表，用对应身份点开每个页面的主操作一次。
2. 远程分页选择器翻到第二页再选。
3. 预览成功后改一项草稿，不重新预览就提交：应被拦住或自动作废旧预览。
4. 切身份后让一个慢请求返回：界面不得回填旧组织数据。

## 6. 页面速查

平台管理台（governor）：

- 组织与租户 → 租户管理
- 应用与配置 → 应用目录 / 套餐 / 共享角色
- 平台管理 → 平台人员（成员、组）、角色与授权
- 安全与运维 → 事件与审计

租户管理台（owner-a 等）：

- 组织管理 → 成员与部门 / 用户组 / 组织设置
- 权限与应用 → 角色与授权 / 应用管理
- 安全与合规 → 成员权限 / 审计
- 工作台 → 工作台 / 通讯录

诊断在「角色与授权」工具入口和成员详情快捷入口，不是一级菜单。

## 7. 明确不能报通过的情况

1. 只启动了 IAM、H2 单测绿、OpenAPI `x-runtime-implemented=true`。
2. 只用 governor 把每个列表点开一遍。
3. `verify` 命令成功（它目前主要核对清单与 DDL，不是业务行为验收）。
4. Auth 直连 token 成功，但四站 Cookie/CSRF 没测。
5. 导出第一页 CSV、或成员不足 250。
6. 缺 XC 条件时用 Mock、空列表或「暂无数据」代替 503/409。
7. 把本手册勾完当成 T16/T17/P26 完成。
