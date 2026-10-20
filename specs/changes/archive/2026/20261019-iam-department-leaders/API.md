# 部门负责人接口

所有路径以 `/v1/tenant/departments/{id}` 为前缀，身份来自当前认证 TENANT 上下文。

- `GET /leaders?page=1&pageSize=20&keyword=`：关系分页，返回 `DepartmentLeaderPage`（items、total、page、pageSize、departmentVersion、restricted）。items 为成员ID、投影姓名、成员状态、主负责人标记、available。
- `GET /leader-candidates?page=1&pageSize=20&keyword=`：成员读取范围内的有效候选，返回同样分页结构；不返回已绑定候选，不携带敏感联系方式。
- `PATCH /leaders`：`DepartmentLeaderUpdateInput={expectedVersion,addMemberIds:[],removeMemberIds:[],primaryMemberId?}`，返回 CreatedResource。差量原子保存；不接收完整覆盖列表。

读取要求 department:read 及对象范围。人员查询、名称投影要求 member:read 及对象/字段范围。候选和写入另要求 `iam-tenant:department:leaders` 及目标部门范围。原、新主负责人和被增删成员均重验可处理范围；不可见关系保留不泄露。无成员读取权限时不返回关联摘要或全员候选。

首次非空保存或删除主负责人后仍非空时必须明确指定新主负责人；未传 primaryMemberId 保留原主负责人；空集合自动清主。新增或新指定主负责人必须有效，旧失效关系可保留或显式移除。

DepartmentRecord 增加可选 leaderSummary：primaryLeader、visibleCount、restricted；仅管理树/详情提供。根部门、导航骨架、通讯录不添加摘要。分页总数只统计可见关系；restricted 仅提示受限，不暴露全量人数或隐藏ID。摘要和分页均实时计算可用状态。

## 2026-10-19 修订契约

- PATCH /v1/tenant/departments/{id}：DepartmentEditInput={expectedVersion, department?:DepartmentDraft, leaders?:DepartmentLeaderChanges}，至少提供一部分；返回 ResourceDetail<DepartmentRecord>。leaders={addMemberIds,removeMemberIds,primaryMemberId?}，不重复携带版本。实际变更才验证相应写操作；整体成功仅递增一次版本。
- GET /v1/tenant/departments/member-picker-navigation?parentId=&page=1&pageSize=20：DepartmentNavigationPage={items:[{id,name}],total,page,pageSize,breadcrumbs:[{id,name}]}。根为当前组织的导航入口，条目不可选；只提供已授权部门名称。部门导航本身不提供或授予成员候选资格。
- GET /v1/tenant/members/picker?departmentId=&keyword=&ids=&page=1&pageSize=20：PageResponse<MemberRecord>，最小资料投影，不携带联系方式。departmentId 为浏览部门，keyword 非空时跨部门搜索；ids 仅少量回显，沿同一成员范围。
- GET /v1/tenant/departments/{id}/leader-candidates 增加可选 browseDepartmentId；不改变目标部门授权。已有负责人仍经独立 GET /leaders 分页回显，不混为新增候选。
- GET /v1/tenant/assignments/options 的 MEMBER 候选增加可选 departmentId，保留直接/委派来源准入、接收范围和排除本人等条件。已选ID回显继续使用原来源。

以上均不修改角色分配或 Token，不新增默认授权；没有部门读取资格的场景仍可在根入口按原候选资格搜索和选择成员。

组织用户组已有成员回显使用独立 `GET /v1/tenant/groups/{id}/members`，按 group:read 与 member:read 范围对关联表分页；不把组的全量成员ID传入候选接口。


### 2026-10-19 对话框精修补充

DepartmentLeaderRecord 增加 departments: MemberDepartmentView[]（默认空数组）；负责人绑定页及候选页随姓名一起返回当前页可见任职。TenantAssignmentOption 增加可选 departments（非成员为空），成员候选须额外具备成员读取和部门读取且命中相应范围才能披露任职；原候选准入与count保持不变。DepartmentNavigationPage 增加可选 organization:{id,name}，仅返回可见根组织名称，未授权时省略。现有端点兼容，无新增写入或默认权限。

## 专用组织成员浏览（2026-10-19，用户已授权）

GET /v1/tenant/member-picker/browse：scene（MEMBER、DEPARTMENT_LEADER、ASSIGNMENT），departmentId?、keyword?、targetDepartmentId?（负责人必填）、delegationGrantId?（仅授权场景）、page/pageSize、departmentPage/departmentPageSize（均默认1/20）。返回 navigationAvailable、navigation（部门独立分页、面包屑及可见组织）、members（id/name/status/available/departments/implicitRoot 的成员分页）。不接受跨场景参数；各场景服务重验原操作、对象/字段范围与来源。接口不需要新增操作权限。

### 姓名搜索接入修复与部署

keyword映射租户member资源的displayName，实际执行清单在browse及保留的picker/leaders/leader-candidates入口声明FILTER。操作准入与对象范围沿原服务；过滤前继续校验字段filterable及整个查询范围FULL。目录能力新增不代表普通用户获得筛选授权；历史默认版本与租户操作规则不自动更新，显式false仍拒绝，拥有者受平台显式上限约束。已有库部署需要025_member_picker_name_filter.sql及对应后端；脚本仅补实际支持能力，不更新角色或token。

/leaders的page/pageSize用于已有负责人关联；browse的page/pageSize用于新增候选，departmentPage/departmentPageSize用于可展开部门，三者独立。左栏候选排除已绑定，已有不可用/受限关系不因未出现在候选中而丢失。

## 头像投影（2026-10-20）

DepartmentLeaderRecord与OrganizationMemberPickerOption增加可选avatar：来源为当前租户成员已授权头像，经member.avatar字段策略处理后使用框架OSS签发时效链接；未设置/隐藏时省略。关联页、候选、详情leaderPage与列表主负责人摘要，以及MEMBER/DEPARTMENT_LEADER组织浏览沿当前查询一次返回，不新增逐人请求或默认权限。ASSIGNMENT保持原资料投影；旧客户端忽略新增字段，无数据库迁移。

前端选人关联快照：initialBoundPage?: Page<IamSelectOption>，records为当前已连续加载的原始可见关联（可包含多页），current为最后成功页，size为原服务页长，total为同范围关联总数。已选集合另带增删草稿，不能用过滤后的右栏人数推断是否有更多。确认事件可附带最新关联快照供父级复用。服务端路由与分页参数保持原契约。

详情增量字段：DepartmentRecord.parentDepartment?: {id:string,name:string}，仅当前租户department:read范围可见时填；parentId存在但父对象不可见时省略此投影。DepartmentRecord.leaderPage?: DepartmentLeaderPage，成员读取资格有效且为普通部门时返回第1页20条，包含可见total、primaryLeader、restricted及departmentVersion。GET /departments/{id}、PATCH /departments/{id}、PUT /departments/{id}同样装配；树和通讯录省略。保持/leaders后续页契约，不将关联分页改成全量详情。没有新路由、查询参数或数据库迁移。
