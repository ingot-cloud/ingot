# IAM 现行接口与测试场景

由 `tools/iam/test-data/operation_matrix.py` 从现行契约生成。
各行登记操作归属、前端能力入口及场景。运行结果见 [验收矩阵](./ACCEPTANCE.md)。
双端同源登录单列 TD17。独立的 Auth、Security、Member、OSS 服务按各自契约归入 TD16 或 TD17。

## 公开操作（246）

| 操作标识 | 方法与路径 | ACTION | TD | 页面或调用入口 |
|---|---|---|---|---|
| `stageBusinessImage` | `POST /v1/assets/uploads` | 按接口执行条件 | TD16 | 成员与个人设置的图片上传 |
| `currentBootstrap` | `GET /v1/me/bootstrap` | 按接口执行条件 | TD02 / TD16 / TD17 | 双端会话与个人设置 |
| `currentCapabilities` | `GET /v1/me/capabilities` | 按接口执行条件 | TD02 / TD16 / TD17 | 双端会话与个人设置 |
| `currentPasswordState` | `GET /v1/me/password` | 按接口执行条件 | TD02 / TD16 / TD17 | 双端会话与个人设置 |
| `currentPutPassword` | `PUT /v1/me/password` | 按接口执行条件 | TD02 / TD16 / TD17 | 双端会话与个人设置 |
| `currentGetProfile` | `GET /v1/me/profile` | 按接口执行条件 | TD02 / TD16 / TD17 | 双端会话与个人设置 |
| `currentPatchProfile` | `PATCH /v1/me/profile` | 按接口执行条件 | TD02 / TD16 / TD17 | 双端会话与个人设置 |
| `platformListAccounts` | `GET /v1/platform/accounts` | `iam-platform:account:read` | TD02 / TD16 | 平台全局账号 |
| `platformCreateAccount` | `POST /v1/platform/accounts` | `iam-platform:account:create` | TD02 / TD16 | 平台全局账号 |
| `platformLookupAccount` | `POST /v1/platform/accounts/lookup` | `iam-platform:account:lookup` | TD02 / TD16 | 平台全局账号 |
| `platformDeleteAccount` | `DELETE /v1/platform/accounts/{id}` | `iam-platform:account:delete` | TD02 / TD16 | 平台全局账号 |
| `platformGetAccount` | `GET /v1/platform/accounts/{id}` | `iam-platform:account:read` | TD02 / TD16 | 平台全局账号 |
| `platformPatchAccount` | `PATCH /v1/platform/accounts/{id}` | `iam-platform:account:update` | TD02 / TD16 | 平台全局账号 |
| `platformDisableAccount` | `POST /v1/platform/accounts/{id}/disable` | `iam-platform:account:disable` | TD02 / TD16 | 平台全局账号 |
| `platformEnableAccount` | `POST /v1/platform/accounts/{id}/enable` | `iam-platform:account:enable` | TD02 / TD16 | 平台全局账号 |
| `platformLockAccount` | `POST /v1/platform/accounts/{id}/lock` | `iam-platform:account:lock` | TD02 / TD16 | 平台全局账号 |
| `platformResetAccountPassword` | `POST /v1/platform/accounts/{id}/reset-password` | `iam-platform:account:reset-password` | TD02 / TD16 | 平台全局账号 |
| `platformUnlockAccount` | `POST /v1/platform/accounts/{id}/unlock` | `iam-platform:account:unlock` | TD02 / TD16 | 平台全局账号 |
| `platformLookupActions` | `POST /v1/platform/actions/lookup` | `iam-platform:action:read` | TD04 / TD13 | 平台应用及授权操作选择器 |
| `platformListApplications` | `GET /v1/platform/applications` | `iam-platform:application:read` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformCreateApplication` | `POST /v1/platform/applications` | `iam-platform:application:create` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformCreateApplicationBundle` | `POST /v1/platform/applications/bundles` | `iam-platform:application:create` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformDeleteApplication` | `DELETE /v1/platform/applications/{id}` | `iam-platform:application:delete` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformGetApplication` | `GET /v1/platform/applications/{id}` | `iam-platform:application:read` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformPatchApplicationStatus` | `PATCH /v1/platform/applications/{id}` | `iam-platform:application:change-status` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformPutApplication` | `PUT /v1/platform/applications/{id}` | `iam-platform:application:update` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformActionCatalog` | `GET /v1/platform/applications/{id}/action-catalog` | `iam-platform:action:read` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformListActions` | `GET /v1/platform/applications/{id}/actions` | `iam-platform:action:read` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformCreateAction` | `POST /v1/platform/applications/{id}/actions` | `iam-platform:action:create` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformDeleteAction` | `DELETE /v1/platform/applications/{id}/actions/{actionId}` | `iam-platform:action:delete` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformPatchActionStatus` | `PATCH /v1/platform/applications/{id}/actions/{actionId}` | `iam-platform:action:change-status` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformPutAction` | `PUT /v1/platform/applications/{id}/actions/{actionId}` | `iam-platform:action:update` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformGrantCatalog` | `GET /v1/platform/applications/{id}/grant-catalog` | `iam-platform:action:read` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformListMenus` | `GET /v1/platform/applications/{id}/menus` | `iam-platform:menu:read` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformCreateMenu` | `POST /v1/platform/applications/{id}/menus` | `iam-platform:menu:create` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformDeleteMenu` | `DELETE /v1/platform/applications/{id}/menus/{menuId}` | `iam-platform:menu:delete` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformPutMenu` | `PUT /v1/platform/applications/{id}/menus/{menuId}` | `iam-platform:menu:update` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformMenuActions` | `GET /v1/platform/applications/{id}/menus/{menuId}/actions` | `iam-platform:menu:read` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformPurgeApplication` | `POST /v1/platform/applications/{id}/purge` | `iam-platform:application:purge` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformListResources` | `GET /v1/platform/applications/{id}/resources` | `iam-platform:resource:read` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformCreateResource` | `POST /v1/platform/applications/{id}/resources` | `iam-platform:resource:create` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformDeleteResource` | `DELETE /v1/platform/applications/{id}/resources/{resourceId}` | `iam-platform:resource:delete` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformPutResource` | `PUT /v1/platform/applications/{id}/resources/{resourceId}` | `iam-platform:resource:update` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformResourceActions` | `GET /v1/platform/applications/{id}/resources/{resourceId}/actions` | `iam-platform:action:read` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformResourceBindings` | `GET /v1/platform/applications/{id}/resources/{resourceId}/field-bindings` | `iam-platform:resource:read` | TD03 / TD04 / TD11 | 平台应用、资源、操作和菜单 |
| `platformListAssignments` | `GET /v1/platform/assignments` | `iam-platform:role-assignment:read` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformCreateAssignments` | `POST /v1/platform/assignments` | `iam-platform:role-assignment:create` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformAssignmentCandidates` | `GET /v1/platform/assignments/candidates` | `iam-platform:role-assignment:read` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformAssignmentContext` | `GET /v1/platform/assignments/context` | `iam-platform:role-assignment:read` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformPreviewAssignments` | `POST /v1/platform/assignments/preview` | `iam-platform:role-assignment:create` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformAssignmentRoleCandidates` | `GET /v1/platform/assignments/role-candidates` | `iam-platform:role-assignment:read` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformAssignmentUpgrade` | `POST /v1/platform/assignments/upgrade` | `iam-platform:role-assignment:apply-version` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformAssignmentUpgradeCandidates` | `GET /v1/platform/assignments/upgrade/candidates` | `iam-platform:role-assignment:apply-version` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformPreviewAssignmentUpgrade` | `POST /v1/platform/assignments/upgrade/preview` | `iam-platform:role-assignment:apply-version` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformAssignmentUpgradeRoles` | `GET /v1/platform/assignments/upgrade/role-candidates` | `iam-platform:role-assignment:apply-version` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformDeleteAssignment` | `DELETE /v1/platform/assignments/{id}` | `iam-platform:role-assignment:revoke` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformAssignmentDetail` | `GET /v1/platform/assignments/{id}` | `iam-platform:role-assignment:read` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformPutAssignment` | `PUT /v1/platform/assignments/{id}` | `iam-platform:role-assignment:update` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformPreviewAssignmentUpdate` | `POST /v1/platform/assignments/{id}/preview` | `iam-platform:role-assignment:update` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformAssignmentSelectedCandidates` | `GET /v1/platform/assignments/{id}/selected-candidates` | `iam-platform:role-assignment:read` | TD06 / TD09 / TD12 | 平台角色与授权 / 角色分配 |
| `platformListAudits` | `GET /v1/platform/authorization/audits` | `iam-platform:authorization-audit:read` | TD13 | 平台权限诊断和审计 |
| `platformDiagnose` | `POST /v1/platform/authorization/diagnose` | `iam-platform:authorization-diagnosis:run` | TD13 | 平台权限诊断和审计 |
| `platformDiagnoseCandidates` | `GET /v1/platform/authorization/diagnose/candidates` | `iam-platform:authorization-diagnosis:run` | TD13 | 平台权限诊断和审计 |
| `platformListDelegations` | `GET /v1/platform/delegations` | `iam-platform:delegation:read` | TD09 / TD12 | 平台角色与授权 / 授权管理员 |
| `platformCreateDelegation` | `POST /v1/platform/delegations` | `iam-platform:delegation:create` | TD09 / TD12 | 平台角色与授权 / 授权管理员 |
| `platformDelegationCandidates` | `GET /v1/platform/delegations/candidates` | `iam-platform:delegation:read` | TD09 / TD12 | 平台角色与授权 / 授权管理员 |
| `platformPreviewDelegationCreate` | `POST /v1/platform/delegations/preview` | `iam-platform:delegation:create` | TD09 / TD12 | 平台角色与授权 / 授权管理员 |
| `platformDelegationRoleCandidates` | `GET /v1/platform/delegations/role-candidates` | `iam-platform:delegation:read` | TD09 / TD12 | 平台角色与授权 / 授权管理员 |
| `platformDeleteDelegation` | `DELETE /v1/platform/delegations/{id}` | `iam-platform:delegation:revoke` | TD09 / TD12 | 平台角色与授权 / 授权管理员 |
| `platformGetDelegation` | `GET /v1/platform/delegations/{id}` | `iam-platform:delegation:read` | TD09 / TD12 | 平台角色与授权 / 授权管理员 |
| `platformPutDelegation` | `PUT /v1/platform/delegations/{id}` | `iam-platform:delegation:update` | TD09 / TD12 | 平台角色与授权 / 授权管理员 |
| `platformPreviewDelegation` | `POST /v1/platform/delegations/{id}/preview` | `iam-platform:delegation:preview` | TD09 / TD12 | 平台角色与授权 / 授权管理员 |
| `platformDelegationSelectedCandidates` | `GET /v1/platform/delegations/{id}/selected-candidates` | `iam-platform:delegation:read` | TD09 / TD12 | 平台角色与授权 / 授权管理员 |
| `platformListDictionaries` | `GET /v1/platform/dictionaries` | `iam-platform:dictionary:read` | TD16 | 平台字典 |
| `platformCreateDictionary` | `POST /v1/platform/dictionaries` | `iam-platform:dictionary:create` | TD16 | 平台字典 |
| `platformSortDictionaries` | `PUT /v1/platform/dictionaries/sort` | `iam-platform:dictionary:update` | TD16 | 平台字典 |
| `platformDeleteDictionary` | `DELETE /v1/platform/dictionaries/{id}` | `iam-platform:dictionary:delete` | TD16 | 平台字典 |
| `platformPutDictionary` | `PUT /v1/platform/dictionaries/{id}` | `iam-platform:dictionary:update` | TD16 | 平台字典 |
| `platformPatchDictionaryStatus` | `PATCH /v1/platform/dictionaries/{id}/status/{status}` | `iam-platform:dictionary:update` | TD16 | 平台字典 |
| `platformListGroups` | `GET /v1/platform/groups` | `iam-platform:group:read` | TD09 | 平台人员 / 用户组 |
| `platformCreateGroup` | `POST /v1/platform/groups` | `iam-platform:group:create` | TD09 | 平台人员 / 用户组 |
| `platformDeleteGroup` | `DELETE /v1/platform/groups/{id}` | `iam-platform:group:delete` | TD09 | 平台人员 / 用户组 |
| `platformGetGroup` | `GET /v1/platform/groups/{id}` | `iam-platform:group:read` | TD09 | 平台人员 / 用户组 |
| `platformPutGroup` | `PUT /v1/platform/groups/{id}` | `iam-platform:group:update` | TD09 | 平台人员 / 用户组 |
| `platformGroupMembers` | `GET /v1/platform/groups/{id}/members` | `iam-platform:group:read` | TD09 | 平台人员 / 用户组 |
| `platformPreviewGroup` | `POST /v1/platform/groups/{id}/preview` | `iam-platform:group:preview` | TD09 | 平台人员 / 用户组 |
| `platformListIdAllocations` | `GET /v1/platform/id-allocations` | `platform-developer:id-allocation:read` | TD16 | 平台业务 ID 发号 |
| `platformCreateIdAllocation` | `POST /v1/platform/id-allocations` | `platform-developer:id-allocation:create` | TD16 | 平台业务 ID 发号 |
| `platformDeleteIdAllocation` | `DELETE /v1/platform/id-allocations/{id}` | `platform-developer:id-allocation:delete` | TD16 | 平台业务 ID 发号 |
| `platformPutIdAllocation` | `PUT /v1/platform/id-allocations/{id}` | `platform-developer:id-allocation:update` | TD16 | 平台业务 ID 发号 |
| `platformListMembers` | `GET /v1/platform/members` | `iam-platform:member:read` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformCreateMember` | `POST /v1/platform/members` | `iam-platform:member:create` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformMemberContext` | `GET /v1/platform/members/context` | `iam-platform:member:read` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformGetMember` | `GET /v1/platform/members/{id}` | `iam-platform:member:read` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformPatchMember` | `PATCH /v1/platform/members/{id}` | `iam-platform:member:update` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformMemberAssignments` | `GET /v1/platform/members/{id}/assignments` | `iam-platform:role-assignment:read` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformMemberBoundRoles` | `GET /v1/platform/members/{id}/bound-roles` | `iam-platform:member:read` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformMemberGroups` | `GET /v1/platform/members/{id}/groups` | `iam-platform:member:read` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformMemberEditPreview` | `POST /v1/platform/members/{id}/preview` | `iam-platform:member:update` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformRemoveMember` | `POST /v1/platform/members/{id}/remove` | `iam-platform:member:remove` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformMemberRoles` | `GET /v1/platform/members/{id}/roles` | `iam-platform:member:read` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformReplaceMemberRoles` | `PUT /v1/platform/members/{id}/roles` | `iam-platform:member:update` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformPatchMemberStatus` | `PATCH /v1/platform/members/{id}/status` | `iam-platform:member:change-status` | TD02 / TD07 / TD09 / TD11 | 平台人员 / 成员 |
| `platformListPlans` | `GET /v1/platform/plans` | `iam-platform:plan:read` | TD03 | 平台应用套餐 |
| `platformCreatePlan` | `POST /v1/platform/plans` | `iam-platform:plan:create` | TD03 | 平台应用套餐 |
| `platformGetPlan` | `GET /v1/platform/plans/{id}` | `iam-platform:plan:read` | TD03 | 平台应用套餐 |
| `platformPutPlan` | `PUT /v1/platform/plans/{id}` | `iam-platform:plan:update` | TD03 | 平台应用套餐 |
| `platformListRoles` | `GET /v1/platform/roles` | `iam-platform:role:read` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformCreateRole` | `POST /v1/platform/roles` | `iam-platform:role:create` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformPreviewRoleCreate` | `POST /v1/platform/roles/preview` | `iam-platform:role:create` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformDeleteRole` | `DELETE /v1/platform/roles/{id}` | `iam-platform:role:delete` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformGetRole` | `GET /v1/platform/roles/{id}` | `iam-platform:role:read` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformPatchRoleStatus` | `PATCH /v1/platform/roles/{id}` | `iam-platform:role:change-status` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformListRoleGrants` | `GET /v1/platform/roles/{id}/grants` | `iam-platform:role:read` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformRoleGroups` | `GET /v1/platform/roles/{id}/groups` | `iam-platform:role:read` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformRoleMembers` | `GET /v1/platform/roles/{id}/members` | `iam-platform:role:read` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformRoleMemberSources` | `GET /v1/platform/roles/{id}/members/{memberId}/assignments` | `iam-platform:role:read` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformPreviewRole` | `POST /v1/platform/roles/{id}/preview` | `iam-platform:role:preview` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformListRoleRevisions` | `GET /v1/platform/roles/{id}/revisions` | `iam-platform:role:read` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformPublishRoleRevision` | `POST /v1/platform/roles/{id}/revisions` | `iam-platform:role:publish-version` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformGetRoleRevision` | `GET /v1/platform/roles/{id}/revisions/{revisionId}` | `iam-platform:role:read` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformListRevisionGrants` | `GET /v1/platform/roles/{id}/revisions/{revisionId}/grants` | `iam-platform:role:read` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformPatchRevisionMetadata` | `PATCH /v1/platform/roles/{id}/revisions/{revisionId}/metadata` | `iam-platform:role:change-status` | TD06 / TD09 / TD11 | 平台角色与授权 / 固定版本 |
| `platformRoleApplicationList` | `GET /v1/platform/roles/{roleId}/version-applications` | `iam-platform:role-assignment:apply-version` | TD06 / TD18 | 平台角色版本应用 |
| `platformRoleApplicationPrepare` | `POST /v1/platform/roles/{roleId}/version-applications` | `iam-platform:role-assignment:apply-version` | TD06 / TD18 | 平台角色版本应用 |
| `platformRoleApplicationCandidates` | `GET /v1/platform/roles/{roleId}/version-applications/candidates` | `iam-platform:role-assignment:apply-version` | TD06 / TD18 | 平台角色版本应用 |
| `platformRoleApplicationDistribution` | `GET /v1/platform/roles/{roleId}/version-applications/distribution` | `iam-platform:role-assignment:apply-version` | TD06 / TD18 | 平台角色版本应用 |
| `platformRoleApplicationPublish` | `POST /v1/platform/roles/{roleId}/version-applications/publish` | `iam-platform:role-assignment:apply-version` | TD06 / TD18 | 平台角色版本应用 |
| `platformRoleApplicationGet` | `GET /v1/platform/roles/{roleId}/version-applications/{id}` | `iam-platform:role-assignment:apply-version` | TD06 / TD18 | 平台角色版本应用 |
| `platformRoleApplicationConfirm` | `POST /v1/platform/roles/{roleId}/version-applications/{id}/confirm` | `iam-platform:role-assignment:apply-version` | TD06 / TD18 | 平台角色版本应用 |
| `platformRoleApplicationItems` | `GET /v1/platform/roles/{roleId}/version-applications/{id}/items` | `iam-platform:role-assignment:apply-version` | TD06 / TD18 | 平台角色版本应用 |
| `platformRoleApplicationConfigure` | `PATCH /v1/platform/roles/{roleId}/version-applications/{id}/items` | `iam-platform:role-assignment:apply-version` | TD06 / TD18 | 平台角色版本应用 |
| `platformRoleApplicationPreflight` | `POST /v1/platform/roles/{roleId}/version-applications/{id}/preflight` | `iam-platform:role-assignment:apply-version` | TD06 / TD18 | 平台角色版本应用 |
| `sharedCurrentListRoles` | `GET /v1/platform/shared-roles` | `iam-platform:shared-role:read` | TD05 / TD11 | 平台共享角色 |
| `sharedCurrentCreateRole` | `POST /v1/platform/shared-roles` | `iam-platform:shared-role:create` | TD05 / TD11 | 平台共享角色 |
| `sharedCurrentCreatePreview` | `POST /v1/platform/shared-roles/preview` | `iam-platform:shared-role:create` | TD05 / TD11 | 平台共享角色 |
| `sharedCurrentDeleteRole` | `DELETE /v1/platform/shared-roles/{id}` | `iam-platform:shared-role:delete` | TD05 / TD11 | 平台共享角色 |
| `sharedCurrentGetRole` | `GET /v1/platform/shared-roles/{id}` | `iam-platform:shared-role:read` | TD05 / TD11 | 平台共享角色 |
| `sharedCurrentProfileRole` | `PATCH /v1/platform/shared-roles/{id}` | `iam-platform:shared-role:change-status` | TD05 / TD11 | 平台共享角色 |
| `sharedCurrentGetGrants` | `GET /v1/platform/shared-roles/{id}/grants` | `iam-platform:shared-role:read` | TD05 / TD11 | 平台共享角色 |
| `sharedCurrentSaveGrants` | `PUT /v1/platform/shared-roles/{id}/grants` | `iam-platform:shared-role:update` | TD05 / TD11 | 平台共享角色 |
| `sharedCurrentPreviewGrants` | `POST /v1/platform/shared-roles/{id}/preview` | `iam-platform:shared-role:preview` | TD05 / TD11 | 平台共享角色 |
| `platformListSocialConfigs` | `GET /v1/platform/social-configs` | `platform-developer:social-config:read` | TD16 | 平台社会化配置 |
| `platformCreateSocialConfig` | `POST /v1/platform/social-configs` | `platform-developer:social-config:create` | TD16 | 平台社会化配置 |
| `platformDeleteSocialConfig` | `DELETE /v1/platform/social-configs/{id}` | `platform-developer:social-config:delete` | TD16 | 平台社会化配置 |
| `platformPutSocialConfig` | `PUT /v1/platform/social-configs/{id}` | `platform-developer:social-config:update` | TD16 | 平台社会化配置 |
| `platformListTenants` | `GET /v1/platform/tenants` | `iam-platform:tenant:read` | TD02 / TD03 | 平台租户及应用开通 |
| `platformCreateTenant` | `POST /v1/platform/tenants` | `iam-platform:tenant:create` | TD02 / TD03 | 平台租户及应用开通 |
| `platformPreviewTenant` | `POST /v1/platform/tenants/preview` | `iam-platform:tenant:preview` | TD02 / TD03 | 平台租户及应用开通 |
| `platformGetTenant` | `GET /v1/platform/tenants/{id}` | `iam-platform:tenant:read` | TD02 / TD03 | 平台租户及应用开通 |
| `platformPatchTenant` | `PATCH /v1/platform/tenants/{id}` | `iam-platform:tenant:update` | TD02 / TD03 | 平台租户及应用开通 |
| `platformListEntitlements` | `GET /v1/platform/tenants/{id}/entitlements` | `iam-platform:application-entitlement:read` | TD02 / TD03 | 平台租户及应用开通 |
| `platformPutEntitlements` | `PUT /v1/platform/tenants/{id}/entitlements` | `iam-platform:application-entitlement:update` | TD02 / TD03 | 平台租户及应用开通 |
| `platformPreviewEntitlements` | `POST /v1/platform/tenants/{id}/entitlements/preview` | `iam-platform:application-entitlement:preview` | TD02 / TD03 | 平台租户及应用开通 |
| `tenantListApplications` | `GET /v1/tenant/applications` | `iam-tenant:application:read` | TD03 / TD04 | 组织应用与应用人群 |
| `tenantApplicationActionCatalog` | `GET /v1/tenant/applications/{id}/action-catalog` | `iam-tenant:application:read` | TD03 / TD04 | 组织应用与应用人群 |
| `tenantListApplicationActions` | `GET /v1/tenant/applications/{id}/actions` | `iam-tenant:application:read` | TD03 / TD04 | 组织应用与应用人群 |
| `tenantGetAudience` | `GET /v1/tenant/applications/{id}/audience` | `iam-tenant:application-audience:read` | TD03 / TD04 | 组织应用与应用人群 |
| `tenantPutAudience` | `PUT /v1/tenant/applications/{id}/audience` | `iam-tenant:application-audience:update` | TD03 / TD04 | 组织应用与应用人群 |
| `tenantListAssignments` | `GET /v1/tenant/assignments` | `iam-tenant:role-assignment:read` | TD05 / TD09 / TD12 | 组织角色与授权 / 角色分配 |
| `tenantCreateAssignments` | `POST /v1/tenant/assignments` | `iam-tenant:role-assignment:create` | TD05 / TD09 / TD12 | 组织角色与授权 / 角色分配 |
| `tenantAssignmentOptions` | `GET /v1/tenant/assignments/options` | `iam-tenant:role-assignment:read` | TD05 / TD09 / TD12 | 组织角色与授权 / 角色分配 |
| `tenantPreviewAssignments` | `POST /v1/tenant/assignments/preview` | `iam-tenant:role-assignment:create` | TD05 / TD09 / TD12 | 组织角色与授权 / 角色分配 |
| `tenantAssignmentRole` | `GET /v1/tenant/assignments/roles/{id}` | `iam-tenant:role-assignment:read` | TD05 / TD09 / TD12 | 组织角色与授权 / 角色分配 |
| `tenantAssignmentScopeCandidates` | `GET /v1/tenant/assignments/scope-candidates` | `iam-tenant:role-assignment:create` | TD05 / TD09 / TD12 | 组织角色与授权 / 角色分配 |
| `tenantDeleteAssignment` | `DELETE /v1/tenant/assignments/{id}` | `iam-tenant:role-assignment:revoke` | TD05 / TD09 / TD12 | 组织角色与授权 / 角色分配 |
| `tenantPutAssignment` | `PUT /v1/tenant/assignments/{id}` | `iam-tenant:role-assignment:update` | TD05 / TD09 / TD12 | 组织角色与授权 / 角色分配 |
| `tenantListAudits` | `GET /v1/tenant/authorization/audits` | `iam-tenant:authorization-audit:read` | TD13 | 组织权限诊断和审计 |
| `tenantDiagnose` | `POST /v1/tenant/authorization/diagnose` | `iam-tenant:authorization-diagnosis:run` | TD13 | 组织权限诊断和审计 |
| `tenantDiagnoseCandidates` | `GET /v1/tenant/authorization/diagnose/candidates` | `iam-tenant:authorization-diagnosis:run` | TD13 | 组织权限诊断和审计 |
| `tenantListDelegations` | `GET /v1/tenant/delegations` | `iam-tenant:delegation:read` | TD09 / TD12 | 组织角色与授权 / 授权管理员 |
| `tenantCreateDelegation` | `POST /v1/tenant/delegations` | `iam-tenant:delegation:create` | TD09 / TD12 | 组织角色与授权 / 授权管理员 |
| `tenantMyDelegations` | `GET /v1/tenant/delegations/mine` | 按接口执行条件 | TD09 / TD12 | 组织角色与授权 / 授权管理员 |
| `tenantDelegationScopeCandidates` | `GET /v1/tenant/delegations/scope-candidates` | `iam-tenant:delegation:create` | TD09 / TD12 | 组织角色与授权 / 授权管理员 |
| `tenantDeleteDelegation` | `DELETE /v1/tenant/delegations/{id}` | `iam-tenant:delegation:revoke` | TD09 / TD12 | 组织角色与授权 / 授权管理员 |
| `tenantGetDelegation` | `GET /v1/tenant/delegations/{id}` | `iam-tenant:delegation:read` | TD09 / TD12 | 组织角色与授权 / 授权管理员 |
| `tenantPutDelegation` | `PUT /v1/tenant/delegations/{id}` | `iam-tenant:delegation:update` | TD09 / TD12 | 组织角色与授权 / 授权管理员 |
| `tenantPreviewDelegation` | `POST /v1/tenant/delegations/{id}/preview` | `iam-tenant:delegation:preview` | TD09 / TD12 | 组织角色与授权 / 授权管理员 |
| `tenantListDepartments` | `GET /v1/tenant/departments` | `iam-tenant:department:read` | TD08 | 组织部门 |
| `tenantCreateDepartment` | `POST /v1/tenant/departments` | `iam-tenant:department:create` | TD08 | 组织部门 |
| `tenantMemberPickerNavigation` | `GET /v1/tenant/departments/member-picker-navigation` | `iam-tenant:department:read` | TD08 | 组织部门 |
| `tenantDepartmentWorkbookExport` | `POST /v1/tenant/departments/workbook/export` | `iam-tenant:department:export` | TD08 / TD14 | 组织部门工作簿 |
| `tenantDepartmentWorkbookHistory` | `GET /v1/tenant/departments/workbook/history` | `iam-tenant:department:create` | TD08 / TD14 | 组织部门工作簿 |
| `tenantDepartmentWorkbookImport` | `POST /v1/tenant/departments/workbook/import` | `iam-tenant:department:create` | TD08 / TD14 | 组织部门工作簿 |
| `tenantDepartmentWorkbookTemplate` | `GET /v1/tenant/departments/workbook/template` | `iam-tenant:department:create` | TD08 / TD14 | 组织部门工作簿 |
| `tenantDepartmentWorkbookFile` | `GET /v1/tenant/departments/workbook/{id}/file` | `iam-tenant:department:create` | TD08 / TD14 | 组织部门工作簿 |
| `tenantDepartmentWorkbookStatus` | `GET /v1/tenant/departments/workbook/{id}/status` | `iam-tenant:department:create` | TD08 / TD14 | 组织部门工作簿 |
| `tenantDeleteDepartment` | `DELETE /v1/tenant/departments/{id}` | `iam-tenant:department:delete` | TD08 | 组织部门 |
| `tenantGetDepartment` | `GET /v1/tenant/departments/{id}` | `iam-tenant:department:read` | TD08 | 组织部门 |
| `tenantEditDepartment` | `PATCH /v1/tenant/departments/{id}` | `iam-tenant:department:read` | TD08 | 组织部门 |
| `tenantPutDepartment` | `PUT /v1/tenant/departments/{id}` | `iam-tenant:department:update` | TD08 | 组织部门 |
| `tenantDepartmentLeaderCandidates` | `GET /v1/tenant/departments/{id}/leader-candidates` | `iam-tenant:department:set-leaders` | TD08 | 组织部门 |
| `tenantListDepartmentLeaders` | `GET /v1/tenant/departments/{id}/leaders` | `iam-tenant:department:read` | TD08 | 组织部门 |
| `tenantUpdateDepartmentLeaders` | `PATCH /v1/tenant/departments/{id}/leaders` | `iam-tenant:department:set-leaders` | TD08 | 组织部门 |
| `tenantDownloadPage` | `GET /v1/tenant/downloads` | 按接口执行条件 | TD14 | 组织下载中心 |
| `tenantDownloadDetail` | `GET /v1/tenant/downloads/{id}` | 按接口执行条件 | TD14 | 组织下载中心 |
| `tenantDownloadLink` | `POST /v1/tenant/downloads/{id}/link` | 按接口执行条件 | TD14 | 组织下载中心 |
| `tenantListGroups` | `GET /v1/tenant/groups` | `iam-tenant:group:read` | TD09 | 组织用户组 |
| `tenantCreateGroup` | `POST /v1/tenant/groups` | `iam-tenant:group:create` | TD09 | 组织用户组 |
| `tenantDeleteGroup` | `DELETE /v1/tenant/groups/{id}` | `iam-tenant:group:delete` | TD09 | 组织用户组 |
| `tenantGetGroup` | `GET /v1/tenant/groups/{id}` | `iam-tenant:group:read` | TD09 | 组织用户组 |
| `tenantPutGroup` | `PUT /v1/tenant/groups/{id}` | `iam-tenant:group:update` | TD09 | 组织用户组 |
| `tenantGroupEffectiveMembers` | `GET /v1/tenant/groups/{id}/effective-members` | `iam-tenant:group:read` | TD09 | 组织用户组 |
| `tenantGroupMemberPickerBound` | `GET /v1/tenant/groups/{id}/members` | `iam-tenant:group:read` | TD09 | 组织用户组 |
| `tenantPreviewGroup` | `POST /v1/tenant/groups/{id}/preview` | `iam-tenant:group:preview` | TD09 | 组织用户组 |
| `tenantMemberPickerBrowse` | `GET /v1/tenant/member-picker/browse` | 按接口执行条件 | TD07 / TD08 / TD09 | 组织成员选择器 |
| `tenantListMembers` | `GET /v1/tenant/members` | `iam-tenant:member:read` | TD07 / TD08 / TD11 | 组织成员 |
| `tenantCreateMember` | `POST /v1/tenant/members` | `iam-tenant:member:create` | TD07 / TD08 / TD11 | 组织成员 |
| `tenantMemberFieldContext` | `GET /v1/tenant/members/context` | `iam-tenant:member:read` | TD07 / TD08 / TD11 | 组织成员 |
| `tenantExportMembers` | `POST /v1/tenant/members/export` | `iam-tenant:member:export` | TD14 | 组织成员 JSON 导出 |
| `tenantDownloadMemberExport` | `GET /v1/tenant/members/export/{id}` | `iam-tenant:member:export` | TD14 | 组织成员 JSON 导出 |
| `tenantGetMemberExportStatus` | `GET /v1/tenant/members/export/{id}/status` | `iam-tenant:member:export` | TD14 | 组织成员 JSON 导出 |
| `tenantMemberPickerCandidates` | `GET /v1/tenant/members/picker` | `iam-tenant:member:read` | TD07 / TD08 / TD11 | 组织成员 |
| `tenantMemberWorkbookExport` | `POST /v1/tenant/members/workbook/export` | `iam-tenant:member:export` | TD07 / TD11 / TD14 | 组织成员工作簿 |
| `tenantMemberWorkbookHistory` | `GET /v1/tenant/members/workbook/history` | 按接口执行条件 | TD07 / TD11 / TD14 | 组织成员工作簿 |
| `tenantMemberWorkbookImport` | `POST /v1/tenant/members/workbook/import` | 按接口执行条件 | TD07 / TD11 / TD14 | 组织成员工作簿 |
| `tenantMemberWorkbookTemplate` | `GET /v1/tenant/members/workbook/template` | `iam-tenant:member:create` | TD07 / TD11 / TD14 | 组织成员工作簿 |
| `tenantMemberWorkbookFile` | `GET /v1/tenant/members/workbook/{id}/file` | 按接口执行条件 | TD07 / TD11 / TD14 | 组织成员工作簿 |
| `tenantMemberWorkbookStatus` | `GET /v1/tenant/members/workbook/{id}/status` | 按接口执行条件 | TD07 / TD11 / TD14 | 组织成员工作簿 |
| `tenantGetMember` | `GET /v1/tenant/members/{id}` | `iam-tenant:member:read` | TD07 / TD08 / TD11 | 组织成员 |
| `tenantPatchMember` | `PATCH /v1/tenant/members/{id}` | `iam-tenant:member:update` | TD07 / TD08 / TD11 | 组织成员 |
| `tenantReplaceMemberDepartments` | `PUT /v1/tenant/members/{id}/departments` | `iam-tenant:member:set-departments` | TD07 / TD08 / TD11 | 组织成员 |
| `tenantMemberGroups` | `GET /v1/tenant/members/{id}/groups` | `iam-tenant:member:read` | TD07 / TD08 / TD11 | 组织成员 |
| `tenantMemberRejoin` | `POST /v1/tenant/members/{id}/rejoin` | `iam-tenant:member:create` | TD07 / TD08 / TD11 | 组织成员 |
| `tenantRemoveMember` | `POST /v1/tenant/members/{id}/remove` | `iam-tenant:member:remove` | TD07 / TD08 / TD11 | 组织成员 |
| `tenantPatchMemberStatus` | `PATCH /v1/tenant/members/{id}/status` | `iam-tenant:member:change-status` | TD07 / TD08 / TD11 | 组织成员 |
| `listFieldPolicyResources` | `GET /v1/tenant/policies/field-resources` | `iam-tenant:field-policy:read` | TD11 | 组织资源字段策略 |
| `getFieldPolicy` | `GET /v1/tenant/policies/fields/{resourceId}` | `iam-tenant:field-policy:read` | TD11 | 组织资源字段策略 |
| `putFieldPolicy` | `PUT /v1/tenant/policies/fields/{resourceId}` | `iam-tenant:field-policy:update` | TD11 | 组织资源字段策略 |
| `tenantCurrentListRoles` | `GET /v1/tenant/roles` | `iam-tenant:role:read` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantCurrentCreateRole` | `POST /v1/tenant/roles` | `iam-tenant:role:create` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantCurrentCatalog` | `GET /v1/tenant/roles/grant-catalog` | `iam-tenant:role:create` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantCurrentCreatePreview` | `POST /v1/tenant/roles/preview` | `iam-tenant:role:create` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantCurrentDeleteRole` | `DELETE /v1/tenant/roles/{id}` | `iam-tenant:role:delete` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantCurrentGetRole` | `GET /v1/tenant/roles/{id}` | `iam-tenant:role:read` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantCurrentProfileRole` | `PATCH /v1/tenant/roles/{id}` | `iam-tenant:role:change-status` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantCurrentGetGrants` | `GET /v1/tenant/roles/{id}/grants` | `iam-tenant:role:read` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantCurrentSaveGrants` | `PUT /v1/tenant/roles/{id}/grants` | `iam-tenant:role:update` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantRoleGroups` | `GET /v1/tenant/roles/{id}/groups` | `iam-tenant:role:read` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantRoleMembers` | `GET /v1/tenant/roles/{id}/members` | `iam-tenant:role:read` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantRoleMemberAssignments` | `GET /v1/tenant/roles/{id}/members/{memberId}/assignments` | `iam-tenant:role:read` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantCurrentPreviewGrants` | `POST /v1/tenant/roles/{id}/preview` | `iam-tenant:role:preview` | TD05 / TD09 / TD11 | 组织角色与授权 / 当前权限 |
| `tenantGetSettings` | `GET /v1/tenant/settings` | `iam-tenant:settings:read` | TD15 | 组织设置 |
| `tenantPutSettings` | `PUT /v1/tenant/settings` | `iam-tenant:settings:update` | TD15 | 组织设置 |
| `tenantTransferOwner` | `POST /v1/tenant/settings/owner-transfer` | `iam-tenant:settings:transfer-owner` | TD15 | 组织设置 |
| `tenantWorkbench` | `GET /v1/tenant/workbench/statistics` | `iam-tenant:workbench:read` | TD04 | 组织工作台 |

## 内部授权与资源操作

| 操作标识 | 方法与路径 | TD | 调用入口 |
|---|---|---|---|
| `iamAuthorizationSnapshot` | `POST /inner/authorization/snapshot` | TD16 / TD18 | 服务消费者 SDK |
| `iamEvaluateV2` | `POST /inner/authorization/v2/evaluate` | TD16 / TD18 | 服务消费者 SDK |
| `iamPreviewV2` | `POST /inner/authorization/v2/preview` | TD16 / TD18 | 服务消费者 SDK |
| `fieldBindingManifest` | `POST /inner/iam/field-bindings/manifest` | TD16 / TD18 | 服务消费者 SDK |
| `resourceObjectQuery` | `POST /inner/iam/resource-objects/query` | TD16 / TD18 | 服务消费者 SDK |
