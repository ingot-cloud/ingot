# 人工认证清单

> 状态：第一批与第二批人工 HTTP 均已由你确认通过（本机 IDE + Bruno）。H2 单测通过不代替本清单。未勾选项表示尚未由你确认。
>
> 使用方式：每项按环境 → 数据 → 身份 → 步骤 → 预期 → 证据执行；通过后把 `- [ ]` 改成 `- [x]` 并附证据位置。

## 2026-09-15 修订说明

本轮仅更新Spec。下方原记录与人工步骤保留为历史输入，T01/T03已重开，不能按旧表的“已勾选”说明判定完成。正式验收使用ACCEPTANCE A01–A28及F01–F09；旧迁移门禁退出范围。现有7个ACTION人工种子仅适合局部测试，不足以证明完整冷启动或治理功能；T05/T16须补充完整新系统初始化及相应步骤。

补充必须验证：平台登录无允许租户仍成功且不选组织；平台/租户/Member分支隔离；锁定态复用既有框架且失败计数/自动解锁/密码事件不退化；跨租户升级拒绝、所有者治理转交、超过200条多实例导出。详细输入与预期见ACCEPTANCE及REMEDIATION，后续按实际实现补齐环境证据，不能现在标记通过。

## 历史自动化记录（不代表本轮完成）

| 项 | 证据 | 说明 |
|---|---|---|
| T01 契约 | `TASKS.md` T01 已勾选；`Iam*ContractTest`、`tools/iam/test_contract.py` | 163 入口映射、类型/错误、管理面 OpenAPI 结构闭合。运行时执行器仍属 T10。 |
| T03 前后端夹具 | `TASKS.md` T03 已勾选；`contracts/examples/` 与 `FRONTEND.md` | 只读/差异/升级/委派/字段/诊断示例与 OpenAPI 一致。 |
| T05 H2 身份/初始化/写入门闩 | IAM provider 相关测试（见 IMPLEMENTATION 本轮记录） | 不证明 MySQL、镜像、Gateway 或真实登录。 |
| MP06 生产 JDBC 清零 | `./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test`；`src/main/java` 无 NamedParameterJdbcTemplate | 不证明 MySQL、多实例缓存失效或 A 系列。 |

## 0. 隔离环境准备

- [x] **0.1 独立目标库**
  - 环境：本机或隔离 Docker MySQL 8.4，**禁止**指向现网/共享业务库。
  - 数据：空库；会话时区 UTC。
  - 步骤：创建库名并导出为 `IAM_DATABASE`；依次执行 `databases/iam/001_identity.sql` … `005_auxiliary.sql`、`databases/iam/007_member_export.sql`、框架 DDL（`account_lock_state`、`password_history`/`password_expiration`）、`databases/iam/006_bootstrap.sql`，再执行 `databases/iam/seed-manual-verification.sql`。
  - 预期：55 张表存在；`006` 写入 2 个应用、40 个资源、145 个 ACTION、24 个菜单、每域一个 SYSTEM 治理角色及 145 条授权、2 个默认策略版本；种子账号 `platform` / `owner` 明文口令均为 `password`；leaf 标签 `iam` 的 `max_id >= 1000000`。
  - 说明：`006` 可重复执行且不覆盖人工修改；平台受控账号另有 `ingot.iam.bootstrap.enabled` 启动器路径，与本清单的固定口令夹具互斥，两者只用其一。
  - 证据：2026-09-16 本机隔离库 + 种子；你确认 0.1 通过（Bruno / IDE）。

- [x] **0.2 运行配置**
  - 环境：IAM 进程使用显式 `IAM_DATABASE`，连接与会话 UTC；Nacos 服务名 `in-service-iam`；Gateway 外部前缀 `/api/iam`。
  - 数据：不使用旧 `ingot_core` 默认名。
  - 步骤：按 `deploy/services` 与 env 模板填写后启动（或本机 bootRun）。
  - 预期：进程能连上目标库；日志无旧 `ingot-pms` 模块名。
  - 证据：2026-09-16 本机 IDE 启动 Auth `5100`、Gateway `7980`、IAM；你确认 0.2 通过。不替代 A18.1 镜像构建。

## A18 镜像与运行链路（T04 未勾选的原因）

- [ ] **A18.1 镜像构建与启动**
  - 环境：可执行 docker build 的机器；产物目录 `output/ingot-iam-provider`。
  - 步骤：按 CI/assemble 构建 JAR 与 Dockerfile；`docker build`；容器启动并健康检查。
  - 预期：入口 `com.ingot.cloud.iam.InIamApplication`；镜像/服务名无 `pms`。
  - 证据：镜像名、`docker ps`、健康检查响应。

- [x] **A18.2 Gateway / Nacos / RPC**
  - 环境：Gateway + Auth + IAM + Nacos（及 Security 如需登录）。
  - 身份：先用种子 `platform` 走 Auth 密码登录，**不传 tenant**（平台身份）。
  - 步骤：确认 Nacos 出现 `in-service-iam`；经 Gateway 访问 `POST /api/iam/v1/platform/tenants/preview`。
  - 预期：请求到达 IAM 新控制器，而不是 `/v1/platform/org/tenant`；401/403/200 均来自新信封，无旧 PMS 服务名。
  - 证据：2026-09-16 经 `http://localhost:7980/iam/v1/...` 到达新控制器；平台登录 Basic `ingot:ingot`；你确认 A18.2 通过。A18.1 镜像仍未勾选。

## A21 正式冷启动（T05 / T16）

冷启动分两段：目录与治理角色由 `databases/iam/006_bootstrap.sql` 落库，受控平台账号由 provider 启动器经安全框架注册用例创建。0.1 已覆盖 SQL 段的行数与幂等，本段只验证 Java 段与真实登录。

- [ ] **A21.1 受控平台账号首启**
  - 环境：空库执行完 001–005、`007_member_export.sql`、框架 DDL 与 `006_bootstrap.sql`，**不要**执行 `seed-manual-verification.sql`；IAM 进程设 `ingot.iam.bootstrap.enabled=true`。
  - 数据：`iam_account`、`iam_platform_member`、`iam_role_assignment` 均为空。
  - 步骤：启动 IAM 进程一次，从启动日志取 WARN 行给出的初始口令；用该账号走 Auth 密码登录且不传 `tenant`。
  - 预期：`iam_account` 出现配置的登录名（默认 `platform`），`iam_platform_member` 一行，治理 assignment 指向平台域 SYSTEM 角色版本；口令未硬编码在源码或 SQL 中；首次登录被要求改密，口令超出有效期后失效。
  - 证据：启动日志 WARN 行（口令打码）、三张表查询结果、登录响应中的改密标志。

- [ ] **A21.2 重复启动与开关默认关闭**
  - 步骤：保持 `enabled=true` 再启动一次；随后去掉该配置（默认关闭）再启动一次。
  - 预期：第二次启动不新增账号/成员/授权，也不重置既有口令；关闭后启动日志无冷启动动作，且不实例化冷启动相关 Bean。
  - 证据：两次启动前后的行数对照、关闭后日志。

## A01 / A01a / A01b 身份隔离（T05 运行时）

登录约定：Auth 用户名密码；`tenant` 空 = 平台身份；`tenant=<组织ID>` = 该租户成员。同一账号不得靠请求体传 memberId 切换。

- [x] **A01 跨域 / 跨租户拒绝**
  - 数据：用平台身份创建组织 A、B（见 A03）；A 内再准备一个非所有者成员（若成员创建 HTTP 未就绪，可用 SQL 插入 `iam_tenant_member`，**不要**给平台会话写租户 ID）。
  - 身份：① 平台会话；② 组织 A 所有者会话。
  - 步骤：平台会话请求 `PATCH /api/iam/v1/tenant/members/{A的成员}/status`；A 会话请求 B 的成员 status/remove。
  - 预期：403 `ActionDenied` 或 404 `ObjectNotFound`；B 成员状态不变。平台治理角色名不得绕过。
  - 证据：2026-09-16 Bruno HTTP；平台 token 调租户成员、A 调 B 均拒绝；你确认通过。

- [x] **A01a 同一账号多身份切换**
  - 数据：`platform` 账号同时拥有平台成员，并作为某组织所有者（A03 创建后即具备）。
  - 身份：先平台登录，再带该 `tenant` 重新登录。
  - 步骤：比较两次 token 中的成员上下文；分别调用平台创建预览与租户成员 status。
  - 预期：`accountId` 相同，`memberId` 不同；平台会话 `tenantId` 为空；权限只随当前域变化；不能靠改 header/query 把平台会话变成租户会话。
  - 证据：2026-09-16 Bruno：同一账号平台/租户两次登录，`accountId` 同、`memberId` 不同；你确认通过。

- [x] **A01b 暂停平台 / 移出租户 / 禁用账号**
  - 数据：账号 X 同时有平台成员和租户 A 成员（可用 `owner`：先给其插入平台成员，或另建账号）。
  - 身份：有 `iam-platform:member:status` / `iam-tenant:member:remove` 的治理身份。
  - 步骤：① `PATCH /v1/platform/members/{平台成员}/status` 暂停；② 用 X 的租户会话确认仍能作为租户成员认证；③ `POST /v1/tenant/members/{租户成员}/remove`；④ `POST /v1/platform/accounts/{id}/disable`；⑤ 再登录任一身份。
  - 预期：① 只影响平台成员；② 租户身份仍有效；③ 租户成员 `REMOVED`，账号行仍在，平台成员状态保持；④⑤ 所有身份无法认证。
  - 证据：2026-09-16 Bruno `bruno/01-A01b-identity-lifecycle/` 全部通过。

## A28 / A28a 登录域（T05 / T13）

- [x] **A28 平台无租户、租户带 org、Member 分支保留**
  - 身份：种子 `platform` 平台登录；组织 A 所有者租户登录。
  - 步骤：平台密码登录不传 `org`；`GET /iam/v1/me/bootstrap`；再以 `domain=TENANT` 且 `org=<组织A>` 登录后 bootstrap。
  - 预期：平台 `tenantId` 为空、不返回租户列表；租户会话 `org` 与 header `Tenant` 一致；无旧 PMS 路径。
  - 证据：2026-09-16 Bruno / 本机 Auth `5100` + Gateway `7980`；你确认通过。平台 JWT 不含 `domain`，租户请求须带 `Tenant`。

- [x] **A28a domain 组合**
  - 步骤：`domain=PLATFORM` 带 tenant 拒绝；`domain=TENANT` 不带 tenant 无管理上下文；`domain=TENANT` 带 tenant 建立成员上下文。
  - 证据：2026-09-16 你确认通过。

## A03 连续创建组织（T05）

- [x] **A03.1 预览无写入**
  - 身份：`platform` 平台会话。
  - 步骤：`POST /api/iam/v1/platform/tenants/preview`，body 见 `contracts/examples/tenant-create.json`，把 `ownerAccountId` 换成 `900002`。
  - 预期：200，`valid=true`；响应无 `governanceRevisionId`；`iam_tenant` 行数不变。
  - 证据：2026-09-16 Bruno `POST /iam/v1/platform/tenants/preview`；你确认通过。

- [x] **A03.2 连续提交两个组织**
  - 步骤：对两个不同 `name` 各 `POST /api/iam/v1/platform/tenants` 一次，所有者均为 `900002`。
  - 预期：两个组织 ID 不同；`iam_role_definition` / `iam_application` / `iam_action` / `iam_menu` 行数不因创建而增加；每个组织仅有所有者成员、根部门、一条治理分配、baseline 开通与默认策略引用；无空 `iam_role_delta`。
  - 证据：2026-09-16 Bruno 连续创建组织 A/B；你确认通过。

## A02 开通不等于授权（T06）

- [x] **A02 开通业务应用后再显式授权**
  - 数据：非 baseline 租户域应用；普通成员 Y（不用所有者 SYSTEM 角色）。
  - 步骤：平台开通 → Y bootstrap/诊断无业务操作 → 分配共享角色 → Y 重新登录后操作码出现且诊断允许 → 审计有记录。
  - 预期：开通本身不赋予业务操作权；授权后生效且有审计。
  - 证据：2026-09-16 Bruno `bruno/02-A02-entitlement-vs-assignment/` 全部通过。

## A04 共享角色与差异（T07）

- [x] **A04 直接分配共享角色、定制差异、恢复基础**
  - 步骤：分配 SHARED 给 Y；TENANT_CUSTOM 仅存 REMOVE/REPLACE_SCOPE/ADD；空 deltas 发布恢复基础；既有共享授权引用不变。
  - 预期：存储最小差异；合成正确；移除/发布不影响其他角色授权。
  - 证据：2026-09-16 Bruno `bruno/03-A04-shared-role-deltas/` 全部通过。

## A26 所有者转交（T05）

- [x] **A26 转交至普通成员、独立授权保留、陈旧版本拒绝**
  - 步骤：给原所有者一条独立 SHARED 授权；转交给 Z；旧所有者失去治理；独立授权仍在；旧 `expectedVersion` 再转交 409 `RevisionConflict`。
  - 预期：治理来源随所有权转交；独立授权保留；竞态/陈旧版本拒绝。
  - 证据：2026-09-16 Bruno `bruno/04-A26-owner-transfer/` 全部通过。双客户端同时移出新所有者未另做，乐观锁 409 已覆盖。
  - 说明：本项会改组织 A 所有者；后续治理请求须用新所有者 Z。

## A08 / A09 任职与暂停

以下 **不能** 当作 A08 数据范围验收通过。求值器已按任职与 `includeDescendants` 计算部门/组范围（H2 与隔离 MySQL CTE 核对过），但 ACCEPTANCE 要求真实 HTTP 下调整任职后授权随之变化。

- [x] **A09.1 多部门暂停 / 移出覆盖全部关系**
  - 数据：租户内成员挂两个部门；操作者具备 `iam-tenant:member:status` 与 `remove`（新组织所有者默认具备种子治理角色）。
  - 步骤：`PATCH .../status` 暂停；再 `POST .../remove`（对另一成员或恢复后再移出）。尝试对所有者 status/remove。
  - 预期：非所有者成功且审计一行；所有者拒绝 `ObjectInUse`；移出后账号仍在，不能再 status 恢复。
  - 证据：2026-09-16 Bruno 暂停/移出非所有者成功，所有者拒绝；你确认通过。

- [x] **A09.2 任职替换**
  - 步骤：`PUT /api/iam/v1/tenant/members/{id}/departments`，`expectedVersion` 为读取版本；目标含一个主部门。再提交过期 version。
  - 预期：部门关系整体替换；响应不含手机号/邮箱原值；过期 version 返回 409 `RevisionConflict`。跨租户部门 ID 返回 404。
  - 证据：2026-09-16 Bruno `PUT .../departments` 成功后过期 version 返回 409；你确认通过。对已 REMOVED 成员再调任职会 `ActionDenied`，不作为本项失败。

- [ ] **A08 所属部门 vs 指定部门**
  - 说明：给「仅所属部门」与「指定部门」两类授权后调整任职，HTTP 预期前者失效、后者不变。H2 `AuthorizationEvaluatorTest` 已覆盖该语义，本项仍须真实会话证据才能勾选。

## T02 / T15 源库快照

- [ ] **真实源库元数据预检**
  - 环境：只读源库快照（不是仓库 `ingot_core.sql`）。
  - 步骤：按 `tools/iam/migration/preflight.py` 要求导出元数据后运行；不把报告当可导入。
  - 预期：`readyForImport` / `readyForCutover` 为 false；未知表与所有者歧义可见；日志无凭证明文。
  - 证据：预检报告路径。

## 前端联调（本仓库不改前端）

- [ ] **T17 / F 系列**
  - 环境：独立前端 change `20260913-base-iam-admin`。
  - 说明：后端未宣称可切流；无前端证据不得勾 F01–F07。

## 已知限制（遇到时不要当成环境配错）

1. 旧 `/v1/platform/org/tenant` 等 PMS HTTP 已删除；验收只走 `/v1/platform/*`、`/v1/tenant/*`、`/v1/me/*`、`/v1/directory/*`。内部 RPC 仍在 `/inner/*`，不进管理面 OpenAPI。
2. 登录权威是 `iam_account`；同名 `sys_user` 行不是该账号的身份来源。`SysUser`/`SysTenant` 只作为 Feign 外形。
3. 社交绑定 `user_id` 指向 `iam_account`；未绑定返回空，不再回读 `sys_user`。
4. OpenAPI 为 96 路径 / 161 操作，`x-runtime-implemented=true` 只表示 `/v1` 控制器存在。字段策略与导出下载已按当前身份重验，人工 A14/A10/A27 仍须真实环境。
5. 未启动 Auth/Gateway/IAM 业务进程前，不能把单测或契约快照当作 A18/A23/A24/A28 通过。
6. 授权求值含组、差异、委派上限与部门范围；热缓存关闭过期放行。多实例失效见 A15，多实例导出见 A27。

## 2026-09-16 人工 HTTP 第一批（已确认）

本机 IDE 启动 + Bruno。Auth `http://localhost:5100/oauth2/token`，Gateway `http://localhost:7980/iam/v1/...`，OAuth Basic `ingot:ingot`（`aW5nb3Q6aW5nb3Q=`）。

已勾选：0.1、0.2、A18.2、A28、A28a、A03.1、A03.2、A01、A01a、A09.1、A09.2。

## 2026-09-16 人工 HTTP 第二批（已确认）

Bruno 集合 [`bruno/`](./bruno/README.md) 按 `00-auth` → `01-A01b` → `02-A02` → `03-A04` → `04-A26` 全部通过。已勾选：A01b、A02、A04、A26。

未勾选：A21 空库冷启动、A18.1 镜像、A08、A05/A16/A25 升级、A10/A27 多实例导出、A13/A14、A15/A20、A17/A19/A22/A23/A24 及 F 系列。不勾选 T16。组织 A 所有者已转为 Z。

## 2026-09-16 T16 自动化证据（不勾选 A 系列）

本轮 Agent 在隔离环境跑通下列回归，**不能**替代 0.1 起的真实进程、登录与 HTTP：

| 证据 | 结果 | 对应 A 项缺口 |
|---|---|---|
| `python3 databases/iam/test_identity_schema.py` | 20 项 MySQL 8.4 约束通过（一次性容器，不连现网库） | 结构/跨域 FK；不证明 HTTP |
| `python3 databases/iam/test_bootstrap_seed.py` | 9 项：145 ACTION、幂等、无凭证行 | A21 SQL 段；缺 A21.1 进程首启 |
| IAM provider `test` | 192 项 H2 通过 | 规则组合；不证明 MySQL 并发/多实例 |
| `LockAccountUseCaseServiceTest` / `UnlockAccountUseCaseServiceTest` | 框架锁定用例通过；IAM 无 `IamAccountLockState` | A22 代码归属；缺失败计数运行时 |
| `python3 tools/iam/test_contract.py` | 7 项，96/161 快照可复现 | A19 映射对账；缺逐入口 HTTP |
| 静态：`ServiceNameConstants.IAM_SERVICE=in-service-iam`，镜像 `ingot/iam`，无 `ingot-pms` 部署名 | 通过 | A18 命名；缺镜像启动与 Gateway |

## 2026-09-19 后续全功能测试数据入口

已勾选历史人工用例保留原证据范围。本轮未执行新的人工验证。后续按 [TEST-DATA](./TEST-DATA.md) 的测试数据 D01–D05准备独立环境，按 TD01–TD18 场景卡及第5节覆盖表执行，不直接在现有调试库重建。逐步点击与功能导读见 [VERIFICATION-GUIDE](./VERIFICATION-GUIDE.md)。测试数据 D01 与本文件历史「DESIGN D01 已确认」不是同一事项。

每次报告附runId/UTC基准、代码与契约版本、账号类别、对象映射、步骤及预期/实际；期限、组变化、撤权、并发、导出失败和BFF重放等需执行动态步骤。数据准备成功只完成准备门禁，不自动勾选本清单或F/L联调。
