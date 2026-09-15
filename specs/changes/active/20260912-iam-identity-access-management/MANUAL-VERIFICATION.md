# 人工认证清单

> 状态：待你逐项执行。本文件只列 Agent 无法代做或必须在真实环境核验的项。H2 单测通过不代替本清单。未勾选项表示尚未由你确认。
>
> 使用方式：每项按环境 → 数据 → 身份 → 步骤 → 预期 → 证据执行；通过后把 `- [ ]` 改成 `- [x]` 并附证据位置。

## 已由研发标记、不必在本清单重复的自动化

| 项 | 证据 | 说明 |
|---|---|---|
| T01 契约 | `TASKS.md` T01 已勾选；`Iam*ContractTest`、`tools/iam/test_contract.py` | 163 入口映射、类型/错误、管理面 OpenAPI 结构闭合。运行时执行器仍属 T10。 |
| T03 前后端夹具 | `TASKS.md` T03 已勾选；`contracts/examples/` 与 `FRONTEND.md` | 只读/差异/升级/委派/字段/诊断示例与 OpenAPI 一致。 |
| T05 H2 身份/初始化/写入门闩 | IAM provider 相关测试（见 IMPLEMENTATION 本轮记录） | 不证明 MySQL、镜像、Gateway 或真实登录。 |
| MP06 生产 JDBC 清零 | `./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test`；`src/main/java` 无 NamedParameterJdbcTemplate | 不证明 MySQL、多实例缓存失效或 A 系列。 |

## 0. 隔离环境准备

- [ ] **0.1 独立目标库**
  - 环境：本机或隔离 Docker MySQL 8.4，**禁止**指向现网/共享业务库。
  - 数据：空库；会话时区 UTC。
  - 步骤：创建库名并导出为 `IAM_DATABASE`；依次执行 `databases/iam/001_identity.sql` … `005_auxiliary.sql`，再执行 `databases/iam/seed-manual-verification.sql`。
  - 预期：55 张表存在；种子账号 `platform` / `owner` 明文口令均为 `password`；leaf 标签 `iam` 的 `max_id >= 1000000`。
  - 证据：执行日志或 `SHOW TABLES` / 账号行截图（不含密码哈希扩散到聊天）。

- [ ] **0.2 运行配置**
  - 环境：IAM 进程使用显式 `IAM_DATABASE`，连接与会话 UTC；Nacos 服务名 `in-service-iam`；Gateway 外部前缀 `/api/iam`。
  - 数据：不使用旧 `ingot_core` 默认名。
  - 步骤：按 `deploy/services` 与 env 模板填写后启动（或本机 bootRun）。
  - 预期：进程能连上目标库；日志无旧 `ingot-pms` 模块名。
  - 证据：启动日志中的 JDBC URL / 时区 / 应用名。

## A18 镜像与运行链路（T04 未勾选的原因）

- [ ] **A18.1 镜像构建与启动**
  - 环境：可执行 docker build 的机器；产物目录 `output/ingot-iam-provider`。
  - 步骤：按 CI/assemble 构建 JAR 与 Dockerfile；`docker build`；容器启动并健康检查。
  - 预期：入口 `com.ingot.cloud.iam.InIamApplication`；镜像/服务名无 `pms`。
  - 证据：镜像名、`docker ps`、健康检查响应。

- [ ] **A18.2 Gateway / Nacos / RPC**
  - 环境：Gateway + Auth + IAM + Nacos（及 Security 如需登录）。
  - 身份：先用种子 `platform` 走 Auth 密码登录，**不传 tenant**（平台身份）。
  - 步骤：确认 Nacos 出现 `in-service-iam`；经 Gateway 访问 `POST /api/iam/v1/platform/tenants/preview`。
  - 预期：请求到达 IAM 新控制器，而不是 `/v1/platform/org/tenant`；401/403/200 均来自新信封，无旧 PMS 服务名。
  - 证据：Gateway 路由日志、Nacos 实例列表、HTTP 状态与 `code`。

## A01 / A01a / A01b 身份隔离（T05 运行时）

登录约定：Auth 用户名密码；`tenant` 空 = 平台身份；`tenant=<组织ID>` = 该租户成员。同一账号不得靠请求体传 memberId 切换。

- [ ] **A01 跨域 / 跨租户拒绝**
  - 数据：用平台身份创建组织 A、B（见 A03）；A 内再准备一个非所有者成员（若成员创建 HTTP 未就绪，可用 SQL 插入 `iam_tenant_member`，**不要**给平台会话写租户 ID）。
  - 身份：① 平台会话；② 组织 A 所有者会话。
  - 步骤：平台会话请求 `PATCH /api/iam/v1/tenant/members/{A的成员}/status`；A 会话请求 B 的成员 status/remove。
  - 预期：403 `ActionDenied` 或 404 `ObjectNotFound`；B 成员状态不变。平台治理角色名不得绕过。
  - 证据：两次失败响应 body；目标成员 `status/version` 查询结果。

- [ ] **A01a 同一账号多身份切换**
  - 数据：`platform` 账号同时拥有平台成员，并作为某组织所有者（A03 创建后即具备）。
  - 身份：先平台登录，再带该 `tenant` 重新登录。
  - 步骤：比较两次 token 中的成员上下文；分别调用平台创建预览与租户成员 status。
  - 预期：`accountId` 相同，`memberId` 不同；平台会话 `tenantId` 为空；权限只随当前域变化；不能靠改 header/query 把平台会话变成租户会话。
  - 证据：两次登录响应中的身份字段（可打码）、两次 API 的成功/失败对照。

- [ ] **A01b 暂停平台 / 移出租户 / 禁用账号**
  - 数据：账号 X 同时有平台成员和租户 A 成员（可用 `owner`：先给其插入平台成员，或另建账号）。
  - 身份：有 `iam-platform:member:status` / `iam-tenant:member:remove` 的治理身份。
  - 步骤：① `PATCH /v1/platform/members/{平台成员}/status` 暂停；② 用 X 的租户会话确认仍能作为租户成员认证；③ `POST /v1/tenant/members/{租户成员}/remove`；④ 将 `iam_account.enabled=FALSE`（账号安全 HTTP 若未切新模型，允许 SQL）；⑤ 再登录任一身份。
  - 预期：① 只影响平台成员；② 租户身份仍有效；③ 租户成员 `REMOVED`，账号行仍在，平台成员状态保持；④⑤ 所有身份无法认证。
  - 证据：三张成员表与 `iam_account` 状态截图；登录失败响应。

## A03 连续创建组织（T05）

- [ ] **A03.1 预览无写入**
  - 身份：`platform` 平台会话。
  - 步骤：`POST /api/iam/v1/platform/tenants/preview`，body 见 `contracts/examples/tenant-create.json`，把 `ownerAccountId` 换成 `900002`。
  - 预期：200，`valid=true`；响应无 `governanceRevisionId`；`iam_tenant` 行数不变。
  - 证据：响应 JSON 与预览前后 `SELECT COUNT(*) FROM iam_tenant`。

- [ ] **A03.2 连续提交两个组织**
  - 步骤：对两个不同 `name` 各 `POST /api/iam/v1/platform/tenants` 一次，所有者均为 `900002`。
  - 预期：两个组织 ID 不同；`iam_role_definition` / `iam_application` / `iam_action` / `iam_menu` 行数不因创建而增加；每个组织仅有所有者成员、根部门、一条治理分配、baseline 开通与默认策略引用；无空 `iam_role_delta`。
  - 证据：两次 `CreatedResource`；创建前后目录表计数；每个 `tenant_id` 的成员/开通/分配查询。

## A08 / A09 任职与暂停（范围引擎未完成，本段只验生命周期）

以下 **不能** 当作 A08 数据范围验收通过。当前 Guard 只检查 ACTION 是否出现在直接分配中，**不**计算所属部门/指定部门范围。

- [ ] **A09.1 多部门暂停 / 移出覆盖全部关系**
  - 数据：租户内成员挂两个部门；操作者具备 `iam-tenant:member:status` 与 `remove`（新组织所有者默认具备种子治理角色）。
  - 步骤：`PATCH .../status` 暂停；再 `POST .../remove`（对另一成员或恢复后再移出）。尝试对所有者 status/remove。
  - 预期：非所有者成功且审计一行；所有者拒绝 `ObjectInUse`；移出后账号仍在，不能再 status 恢复。
  - 证据：成员状态、`iam_account` 仍在、审计 `target_id`、所有者失败 body。

- [ ] **A09.2 任职替换**
  - 步骤：`PUT /api/iam/v1/tenant/members/{id}/departments`，`expectedVersion` 为读取版本；目标含一个主部门。再提交过期 version。
  - 预期：部门关系整体替换；响应不含手机号/邮箱原值；过期 version 返回 409 `RevisionConflict`。跨租户部门 ID 返回 404。
  - 证据：任职表、成功响应、409 body。

- [ ] **A08 所属部门 vs 指定部门（待 T09，当前应记失败或跳过）**
  - 说明：完整引擎未接入前，给“仅所属部门”与“指定部门”两类授权后调整任职，**不应**勾选通过。若你执行了，把实际结果记在证据里（预期：现在只要有 ACTION 就会放行，这是已知缺口）。

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

1. 旧 `/v1/platform/org/tenant` 等 PMS 控制器仍在；验收新行为请走 `/v1/platform/tenants*` 与 `/v1/tenant/members*`。
2. 新模型登录优先 `iam_account`；同名 `sys_user` 不会再作为该账号的权威身份。
3. 社交登录在社交表 `user_id` 能对应 `iam_account.id` 时走新账号，否则回退旧用户表。
4. 管理面 132 个操作已有控制器；字段策略与导出下载已按当前身份重验，人工 A14/A10 仍须在真实环境执行。
5. OpenAPI 管理面操作 `x-runtime-implemented=true`；未启动业务镜像前不能当作联调通过。
6. 授权求值含组、差异、委派上限与部门范围；热缓存关闭过期放行。多实例失效见 A15。
