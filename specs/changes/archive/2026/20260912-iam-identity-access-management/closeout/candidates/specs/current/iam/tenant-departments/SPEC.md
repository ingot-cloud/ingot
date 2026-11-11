# 租户部门目录

## 管理部门树

`GET /v1/tenant/departments` 仅接受 `purpose=MANAGED_DEPARTMENT`，权限为 `iam-tenant:department:read`。一次返回当前身份可见的全部部门树，不分页，不接受 `page` / `pageSize`。响应为 `DepartmentTreeNode[]`：子部门挂在父节点的 `children` 下，根部门的直属一级部门不再与根平级。

`GET /v1/tenant/departments/{id}` 为部门详情。通讯录 `GET /v1/directory/departments` 以及策略/选择器构造保持自身语义，不改成分页树；组织选人使用[独立浏览投影](../organization-member-picker/SPEC.md)。

## 直属成员数

`DepartmentRecord.memberCount` 为可选整数，只在租户管理部门树填充。非根部门口径为本部门直属任职且成员状态为 `ACTIVE`，不含下级部门人数。无直属 ACTIVE 成员时填 `0`，不省略。根部门（`parentId` 为空）的人数是当前租户未移出成员总数，含暂停，与不传 `departmentId` 的成员列表总数一致，不按根部门任职行计数。详情、通讯录、策略骨架和 `navigationOnly` 祖先骨架省略该字段。

计数按当前可见部门 ID 一次 `GROUP BY`，禁止按部门 N+1。无可见部门时跳过计数 SQL。计数失败随列表失败，不 fail-open 为 `0`。未出现在计数结果中的部门视为 `0`。

删除空部门的既有校验仍统计全部任职（含暂停），与列表口径分离，避免暂停成员所在部门被误删。计数不改变对象能力、范围求值或授权。

## 部门负责人关系与权限

负责人保存于独立关系 `iam_department_leader`，复合主键为 `tenant_id/department_id/member_id`，以同租户外键约束部门和成员。`is_primary` 标记主负责人，生成列及唯一索引约束每个部门至多一位主负责人；业务保存保证非空关系集合恰有一位主负责人。

仅普通部门维护负责人，允许本租户有效成员跨部门兼管，不改变成员任职部门。暂停、移出或账号停用不删除关系，不自动更换主负责人；恢复后按当前状态计算可用性。新增及新指定主负责人必须有效且账号可用，未修改的失效关系可以保留。仍有任何负责人关系时禁止删除部门或将其改为根部门。

负责人身份仅记录组织事实，不自动分配角色、操作、数据范围或字段特权，不写入 Token，也不改变通讯录策略。操作和范围仍由现有角色与授权系统配置；普通角色不因新增操作目录而自动补权，拥有者沿现有动态授权规则求值。

| 行为 | 权限及范围 |
|---|---|
| 查看部门 | `iam-tenant:department:read` 与部门对象范围 |
| 查看负责人资料 | 同时满足 `iam-tenant:member:read`、成员对象范围与成员管理字段策略 |
| 维护负责人 | `iam-tenant:department:set-leaders` 与目标部门范围，并重验相关成员读取范围及资格 |
| 修改部门基本信息 | `iam-tenant:department:update` 与部门对象范围 |

## 负责人读取与详情聚合

管理树及详情的可选 `leaderSummary` 包含 `primaryLeader`、`visibleCount`、`restricted`。数量仅计当前成员读取范围可见关系；不可见主负责人不返回身份，存在隐藏关系仅返回受限标志，不泄露隐藏 ID、姓名或完整人数。根部门、导航骨架与通讯录不附加负责人摘要。管理树批量装配摘要，不按部门逐个查询。

详情以及统一编辑/既有更新响应在原读取资格内装配 `parentDepartment:{id,name}` 和 `leaderPage`。父对象不可见时不补名称；负责人首屏默认第 1 页、20 条，复用摘要及关联查询。缺少成员读取资格时不查询人员、不填首屏；旧更新调用没有对应读取资格时省略聚合字段，不为回显额外补权。列表、通讯录与导航不装配详情聚合字段。

`GET /v1/tenant/departments/{id}/leaders` 分页读取已有关系，`GET /leader-candidates` 读取可新增的有效未绑定成员，两者独立。`DepartmentLeaderPage` 返回当前可见 `items/total/page/pageSize`、`departmentVersion`、`restricted` 与可见 `primaryLeader`。关联规模数据不得用父列表 `ids` 回查或首屏覆盖全集。

负责人资料仅含已授权的成员最小投影及可见任职；姓名、头像分别遵循成员字段策略，不返回联系方式或账号权限。原始头像只存 `bucket/objectName`，经字段策略后由 OSS 序列化签发时效链接；缺失或隐藏时省略，不从更宽接口补回。列表主负责人头像只取主负责人对应记录。

## 统一编辑、版本与差量

`PATCH /v1/tenant/departments/{id}` 接收 `expectedVersion`、可选 `department` 基本资料和可选 `leaders`，至少提供一部分。负责人差量为 `addMemberIds/removeMemberIds/primaryMemberId?`，省略主负责人表示保留原主；首次非空保存或移除原主后仍非空时须明确指定新主。清空可操作关系不代表删除不可见或未加载关系。

按实际变化分别验证基本信息与负责人操作。事务内按组织锁、部门锁、成员 ID 顺序加锁，重验租户、范围、成员资格与部门版本；两部分整体成功或回滚，部门版本只递增一次，审计失败同样回滚。并发冲突返回 `RevisionConflict`（409），不得自动重试覆盖。原 `PUT /departments/{id}` 与 `PATCH /departments/{id}/leaders` 仍可使用自身契约。

组织成员对话框的默认设主只修改前端草稿；最终仍以服务端差量、版本及资格校验为准。

负责人保存只增删变化关系；主负责人未变时不清空重设主标记。主负责人切换先解除旧标记再设置新标记，保留原事务、版本和审计契约；关系未变不代表父对象版本与审计一律跳过。参见[关系持久化规则](../database-persistence/SPEC.md)。

## 成员列表筛选

`GET /v1/tenant/members` 可按直属 `departmentId` 与 `status=ACTIVE|SUSPENDED|REMOVED` 过滤。不传 `departmentId` 时返回当前身份可见的组织成员，不按部门任职收窄；组织管理选中根部门时不传该参数。不传 `status` 时排除已移出成员。精确 `phone` / `email` 筛选保持不变。

## 组织菜单种子

组织管理下「成员与部门」为二级目录，下面再挂「成员」「部门」两个页面。新库由 `tools/iam/generate_database.py` 写入；已有库执行 `023_tenant_members_departments_menus.sql`。

| 名称 | route_name | path | view_path | 绑定 ACTION |
|---|---|---|---|---|
| 成员与部门 | `org.iam.members.root` | `/org/iam/members/root` | `layout.simple` | `iam-tenant:member:read`、`iam-tenant:department:read` |
| 成员 | `org.iam.members` | `/org/iam/members` | `org.iam.members` | `iam-tenant:member:read` |
| 部门 | `org.iam.departments` | `/org/iam/departments` | `org.iam.departments` | `iam-tenant:department:read` |

带子集的二级及以下目录默认 `view_path=layout.simple`。顶层目录仍用 `layout.main`。组织管理排序为成员与部门、用户组、组织设置。

## 表格导入与导出

`/v1/tenant/departments/workbook/*` 不改部门 JSON 接口。列名、路径分隔符、1000 行上限和 24 小时保留期都是命名常量。创建模板列为部门名称、上级部门、排序。更新导出在最前增加「部门 ID」。不导出人数、负责人或导航骨架。表头前有填写须知，解析按「部门名称」定位表头。

上级部门必填，写从根开始的名称路径，用 `/` 分层。路径不存在、同级重名导致不唯一，或会形成环，该行失败，不自动补中间部门。同一上级下名称必须唯一，含本文件中已成功的前序行；单条创建接口不因此改严。名称不能含 `/`，最长 128。排序留空按 0。根部门可以改名称和排序，不能改上级，也不能通过导入再造根。创建行填写了部门 ID 则该行失败。更新时列不在表头则不改该字段；名称或上级在表头但为空则该行失败。

`GET /workbook/template` 要 `iam-tenant:department:create`，同步返回空白模板。

`POST /workbook/export` 要 `iam-tenant:department:export`。请求为组织版本和可选 `departmentIds`；空或省略表示当前可见的全部部门。登记、版本校验和审计在同一事务内交给下载中心，返回下载任务 ID。来源 `iam.department-workbook`，标题「部门导出」，文件名 `部门信息.xlsx`。工作线程重新校验导出权限，按部门树展开勾选部门及其下级，并按树前序写出，保证上级行在下级之前。文件生成与下载见 [公共下载中心](../../framework/download-center/SPEC.md)。

`POST /workbook/import` 接收 multipart `file` 和 `mode`（`CREATE` 或 `UPDATE`）。创建要 `department:create`，更新要 `department:update`。非 xlsx、空文件、无法解析、缺少表头或超过 1000 行时整份拒绝，不登记任务。通过后在事务内写入 `iam_department_workbook` 的 `PENDING` 行，记下组织版本和审计，提交后再异步逐行调用现有创建或更新。每行读取当前部门版本再更新，冲突只失败该行。一行失败不回滚其他行。逐行处理结束即标 `SUCCEEDED`，结果含成功数、失败数和失败明细；执行异常才标 `FAILED`。

任务只对发起成员可见。读写时把到期任务标为 `EXPIRED` 并删除对象。`GET /workbook/history` 返回当前操作者最近 10 条导入。`GET /workbook/{id}/status` 与 `/file?artifact=` 只服务导入：产物为工作簿或错误报告，没有一次性密码；进行中下载返回 503，失败、过期或不存在返回 404。原始工作簿和错误报告存对象存储，库内只留 `bucket/objectName`。

`iam-tenant:department:export` 写入引导种子并授给租户治理模板角色。已有自定义角色不自动补权；拥有者仍走现有动态授权。已有库执行 `031_department_workbook.sql`。

## 兼容与回滚

`memberCount`、负责人摘要、详情聚合及头像为可选响应字段，旧客户端可忽略；原 DTO 构造方式和独立更新/关联接口保留兼容。负责人关系独立于角色和授权表；应用回退不得删除已有关系或重建业务库。数据库升级依据见[迁移说明](../../../../databases/iam/migrations/README.md)。

## 工作簿任务并发与对象清理

导入登记沿现有权限、版本、审计和状态竞争规则；到期行按主键清理。存储对象在提交后删除，删除失败保留路径供后续重试；审计失败不删除旧产物，并补偿新上传。部门分支导出在下载中心签发时继续校验发起人与当前资格。
