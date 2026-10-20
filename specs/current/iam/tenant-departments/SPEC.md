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
| 维护负责人 | `iam-tenant:department:leaders` 与目标部门范围，并重验相关成员读取范围及资格 |
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

## 兼容与回滚

`memberCount`、负责人摘要、详情聚合及头像为可选响应字段，旧客户端可忽略；原 DTO 构造方式和独立更新/关联接口保留兼容。负责人关系独立于角色和授权表；应用回退不得删除已有关系或重建业务库。数据库升级依据见[迁移说明](../../../../databases/iam/migrations/README.md)。
