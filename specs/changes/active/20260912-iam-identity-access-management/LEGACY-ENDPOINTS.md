# 旧接口盘点（T01 输入）

扫描范围：PMS 与 Security provider 的 `web` 下直接声明的 RestController。记录源代码事实，不代表运行时安全审计通过，也不是新 ACTION 映射表。注解只记录类/方法直接声明；组合注解、过滤器、服务层和 SQL 仍须分别核验。路径为服务内部路径，不含网关前缀。

共 38 个控制器、163 个 HTTP 方法映射。所有新域/资源/ACTION/执行器映射仍须在 T01 完成后确认；不能把旧权限码自动当成新权限。

| 控制器方法 | HTTP | 旧路径 | 直接安全/范围注解 |
|---|---|---|---|
| `TestAPI.test` | GET | `/test/limit` | @Permit |
| `InnerAuthorizationAPI.snapshot` | POST | `/inner/authorization/snapshot` | @Permit(mode = PermitMode.INNER) |
| `InnerDictAPI.items` | POST | `/inner/dict/items` | @Permit(mode = PermitMode.INNER) |
| `InnerDictAPI.nodes` | POST | `/inner/dict/nodes` | @Permit(mode = PermitMode.INNER) |
| `InnerDictAPI.batchItems` | POST | `/inner/dict/batch` | @Permit(mode = PermitMode.INNER) |
| `InnerIdAPI.appId` | GET | `/inner/id/appId` | @Permit(mode = PermitMode.INNER) |
| `InnerLoginRecordAPI.record` | POST | `/inner/user/login/record` | @Permit(mode = PermitMode.INNER) |
| `InnerSocialDetailsAPI.getSocialDetailsByType` | GET | `/inner/social/detailList/{type}` | @Permit(mode = PermitMode.INNER) |
| `InnerSocialDetailsAPI.getDetailsByAppId` | GET | `/inner/social/appId/{appId}` | @Permit(mode = PermitMode.INNER) |
| `InnerTenantDetailsAPI.getUserTenantDetails` | POST | `/inner/tenant/details/{username}` | @Permit(mode = PermitMode.INNER) |
| `InnerTenantDetailsAPI.getTenantByIds` | POST | `/inner/tenant/detailsList` | @Permit(mode = PermitMode.INNER) |
| `InnerTenantDetailsAPI.getTenantById` | GET | `/inner/tenant/{id}` | @Permit(mode = PermitMode.INNER) |
| `InnerUserDetailsAPI.getUserAuthDetail` | POST | `/inner/user/details` | @Permit(mode = PermitMode.INNER) |
| `InnerUserDetailsAPI.getUserInfo` | GET | `/inner/user/{id}` | @Permit(mode = PermitMode.INNER) |
| `InnerUserDetailsAPI.getAllUserInfo` | POST | `/inner/user/list` | @Permit(mode = PermitMode.INNER) |
| `AuthUserAPI.getUserInfo` | GET | `/v1/auth/user/info` | 未见方法/类授权注解；需核对过滤器与服务层 |
| `AuthUserAPI.getUserMenus` | GET | `/v1/auth/user/menus` | 未见方法/类授权注解；需核对过滤器与服务层 |
| `AuthUserAPI.getUserPermissions` | GET | `/v1/auth/user/permissions` | 未见方法/类授权注解；需核对过滤器与服务层 |
| `OSSCommonAPI.upload` | POST | `/v1/oss/upload` | 未见方法/类授权注解；需核对过滤器与服务层 |
| `OrgAuthAPI.getOrgAuthTree` | GET | `/v1/org/auth/tree` | @AdminOrHasAnyAuthority("org:contacts:auth:query") |
| `OrgDeptAPI.tree` | GET | `/v1/org/dept/tree` | @AdminOrHasAnyAuthority({"org:contacts:user:query", "contacts:dept:query"}) |
| `OrgDeptAPI.tree2` | GET | `/v1/org/dept/tree2` | @AdminOrHasAnyAuthority({"org:contacts:user:query", "contacts:dept:query"}) |
| `OrgDeptAPI.simpleTree` | GET | `/v1/org/dept/simpleTree` | @AdminOrHasAnyAuthority({"org:contacts:user:query", "contacts:dept:query"}) |
| `OrgDeptAPI.create` | POST | `/v1/org/dept` | @AdminOrHasAnyAuthority({"org:contacts:dept:create"}) |
| `OrgDeptAPI.update` | PUT | `/v1/org/dept` | @AdminOrHasAnyAuthority({"org:contacts:dept:update"}) |
| `OrgDeptAPI.removeById` | DELETE | `/v1/org/dept/{id}` | @AdminOrHasAnyAuthority({"org:contacts:dept:delete"}) |
| `OrgRoleAPI.options` | GET | `/v1/org/role/options` | @AdminOrHasAnyAuthority({"org:contacts:role:query"}) |
| `OrgRoleAPI.tree` | GET | `/v1/org/role/tree` | @AdminOrHasAnyAuthority({"org:contacts:role:query"}) |
| `OrgRoleAPI.create` | POST | `/v1/org/role` | @AdminOrHasAnyAuthority({"org:contacts:role:create"}) |
| `OrgRoleAPI.update` | PUT | `/v1/org/role` | @AdminOrHasAnyAuthority({"org:contacts:role:update"}) |
| `OrgRoleAPI.removeById` | DELETE | `/v1/org/role/{id}` | @AdminOrHasAnyAuthority({"org:contacts:role:delete"}) |
| `OrgRoleAPI.sort` | PUT | `/v1/org/role/sort` | @AdminOrHasAnyAuthority({"org:contacts:role:sort"}) |
| `OrgRoleAPI.assignUsers` | PUT | `/v1/org/role/{id}/users` | @AdminOrHasAnyAuthority({"org:contacts:role:user:assign"}) |
| `OrgRoleAPI.setPermissions` | PUT | `/v1/org/role/{id}/permissions` | @AdminOrHasAnyAuthority("org:contacts:role:permissions:set") |
| `OrgRoleAPI.getRolePermissionsTree` | GET | `/v1/org/role/{id}/permissions` | @AdminOrHasAnyAuthority("org:contacts:role:permissions:query") |
| `OrgRoleAPI.getDataRules` | GET | `/v1/org/role/{id}/data-rules` | @AdminOrHasAnyAuthority("org:contacts:role:data-rule:query") |
| `OrgRoleAPI.setDataRules` | PUT | `/v1/org/role/{id}/data-rules` | @AdminOrHasAnyAuthority("org:contacts:role:data-rule:set") |
| `OrgUserAPI.page` | GET | `/v1/org/user/page` | @AdminOrHasAnyAuthority({"org:contacts:user:query"}) |
| `OrgUserAPI.pageWithRoleStatus` | GET | `/v1/org/user/role/{roleId}/page` | @AdminOrHasAnyAuthority({"org:contacts:user:query"}) |
| `OrgUserAPI.create` | POST | `/v1/org/user` | @AdminOrHasAnyAuthority({"org:contacts:user:create"}) |
| `OrgUserAPI.update` | PUT | `/v1/org/user` | @AdminOrHasAnyAuthority({"org:contacts:user:update"}) |
| `OrgUserAPI.removeById` | DELETE | `/v1/org/user/{id}` | @AdminOrHasAnyAuthority({"org:contacts:user:delete"}) |
| `OrgUserAPI.userProfile` | GET | `/v1/org/user/detail/{id}` | @AdminOrHasAnyAuthority({"org:contacts:user:detail"}) |
| `OrgUserAPI.initFixPwd` | PUT | `/v1/org/user/pwd/init` | @AdminOrHasAnyAuthority({PermissionConstants.INIT_PASSWORD}) |
| `OrgUserAPI.fixPwd` | PUT | `/v1/org/user/pwd` | 未见方法/类授权注解；需核对过滤器与服务层 |
| `SystemDeptAPI.tree` | GET | `/v1/platform/admin/dept/tree/{orgId}` | @AdminOrHasAnyAuthority({"platform:admin:dept:query"}) |
| `SystemRoleAPI.options` | GET | `/v1/platform/admin/role/tree/{orgId}` | @AdminOrHasAnyAuthority({"platform:admin:role:query"}) |
| `SystemUserAPI.searchByPhone` | GET | `/v1/platform/admin/user/searchByPhone` | @AdminOrHasAnyAuthority({"platform:admin:user:search"}) |
| `SystemUserAPI.page` | GET | `/v1/platform/admin/user/page` | @AdminOrHasAnyAuthority({"platform:system:user:query"}) |
| `SystemUserAPI.create` | POST | `/v1/platform/admin/user` | @AdminOrHasAnyAuthority({"platform:system:user:create"}) |
| `SystemUserAPI.update` | PUT | `/v1/platform/admin/user` | @AdminOrHasAnyAuthority({"platform:system:user:update"}) |
| `SystemUserAPI.removeById` | DELETE | `/v1/platform/admin/user/{id}` | @AdminOrHasAnyAuthority({"platform:system:user:delete"}) |
| `SystemUserAPI.orgInfo` | GET | `/v1/platform/admin/user/orgInfo/{userId}` | @AdminOrHasAnyAuthority({"platform:system:user:org:details"}) |
| `SystemUserAPI.userOrgEdit` | PUT | `/v1/platform/admin/user/org` | @AdminOrHasAnyAuthority({"platform:system:user:org:update"}) |
| `SystemUserAPI.userOrgLeave` | DELETE | `/v1/platform/admin/user/org` | @AdminOrHasAnyAuthority({"platform:system:user:org:delete"}) |
| `SystemUserAPI.resetPwd` | PUT | `/v1/platform/admin/user/{userId}/reset-password` | @AdminOrHasAnyAuthority({"platform:system:user:reset"}) |
| `SystemUserAPI.userProfile` | GET | `/v1/platform/admin/user/profile/{id}` | @AdminOrHasAnyAuthority({"platform:system:user:details"}) |
| `SystemUserAPI.updateUserBaseInfo` | PUT | `/v1/platform/admin/user/base` | 未见方法/类授权注解；需核对过滤器与服务层 |
| `SystemUserAPI.enableAccount` | PUT | `/v1/platform/admin/user/{userId}/enable` | @AdminOrHasAnyAuthority({"platform:system:user:update"}) |
| `SystemUserAPI.disableAccount` | PUT | `/v1/platform/admin/user/{userId}/disable` | @AdminOrHasAnyAuthority({"platform:system:user:update"}) |
| `SystemUserAPI.lockAccount` | PUT | `/v1/platform/admin/user/{userId}/lock` | @AdminOrHasAnyAuthority({"platform:system:user:lock"}) |
| `SystemUserAPI.unlockAccount` | PUT | `/v1/platform/admin/user/{userId}/unlock` | @AdminOrHasAnyAuthority({"platform:system:user:lock"}) |
| `PlatformApplicationAPI.page` | GET | `/v1/platform/config/apps/page` | @AdminOrHasAnyAuthority({"platform:config:app:query"}) |
| `PlatformApplicationAPI.detail` | GET | `/v1/platform/config/apps/{appId}` | @AdminOrHasAnyAuthority({"platform:config:app:query"}) |
| `PlatformApplicationAPI.create` | POST | `/v1/platform/config/apps` | @AdminOrHasAnyAuthority({"platform:config:app:create"}) |
| `PlatformApplicationAPI.update` | PUT | `/v1/platform/config/apps/{appId}` | @AdminOrHasAnyAuthority({"platform:config:app:update"}) |
| `PlatformApplicationAPI.patchStatus` | PATCH | `/v1/platform/config/apps/{appId}/status` | @AdminOrHasAnyAuthority({"platform:config:app:update"}) |
| `PlatformApplicationAPI.delete` | DELETE | `/v1/platform/config/apps/{appId}` | @AdminOrHasAnyAuthority({"platform:config:app:delete"}) |
| `PlatformApplicationAPI.menuTree` | GET | `/v1/platform/config/apps/{appId}/menus/tree` | @AdminOrHasAnyAuthority({"platform:config:app:menu:query"}) |
| `PlatformApplicationAPI.createMenu` | POST | `/v1/platform/config/apps/{appId}/menus` | @AdminOrHasAnyAuthority({"platform:config:app:menu:create"}) |
| `PlatformApplicationAPI.updateMenu` | PUT | `/v1/platform/config/apps/{appId}/menus/{menuId}` | @AdminOrHasAnyAuthority({"platform:config:app:menu:update"}) |
| `PlatformApplicationAPI.deleteMenu` | DELETE | `/v1/platform/config/apps/{appId}/menus/{menuId}` | @AdminOrHasAnyAuthority({"platform:config:app:menu:delete"}) |
| `PlatformApplicationAPI.permissionTree` | GET | `/v1/platform/config/apps/{appId}/permissions/tree` | @AdminOrHasAnyAuthority({"platform:config:app:permission:query"}) |
| `PlatformApplicationAPI.createPermission` | POST | `/v1/platform/config/apps/{appId}/permissions` | @AdminOrHasAnyAuthority({"platform:config:app:permission:create"}) |
| `PlatformApplicationAPI.updatePermission` | PUT | `/v1/platform/config/apps/{appId}/permissions/{permissionId}` | @AdminOrHasAnyAuthority({"platform:config:app:permission:update"}) |
| `PlatformApplicationAPI.deletePermission` | DELETE | `/v1/platform/config/apps/{appId}/permissions/{permissionId}` | @AdminOrHasAnyAuthority({"platform:config:app:permission:delete"}) |
| `PlatformApplicationAPI.listResources` | GET | `/v1/platform/config/apps/{appId}/resources` | @AdminOrHasAnyAuthority({"platform:config:app:resource:query"}) |
| `PlatformApplicationAPI.createResource` | POST | `/v1/platform/config/apps/{appId}/resources` | @AdminOrHasAnyAuthority({"platform:config:app:resource:create"}) |
| `PlatformApplicationAPI.updateResource` | PUT | `/v1/platform/config/apps/{appId}/resources/{resourceId}` | @AdminOrHasAnyAuthority({"platform:config:app:resource:update"}) |
| `PlatformApplicationAPI.deleteResource` | DELETE | `/v1/platform/config/apps/{appId}/resources/{resourceId}` | @AdminOrHasAnyAuthority({"platform:config:app:resource:delete"}) |
| `PlatformDictAPI.tree` | GET | `/v1/platform/config/dict/tree` | @AdminOrHasAnyAuthority({"platform:config:dict:query"}) |
| `PlatformDictAPI.page` | GET | `/v1/platform/config/dict/page` | @AdminOrHasAnyAuthority({"platform:config:dict:query"}) |
| `PlatformDictAPI.items` | GET | `/v1/platform/config/dict/items/{code}` | @AdminOrHasAnyAuthority({"platform:config:dict:query"}) |
| `PlatformDictAPI.create` | POST | `/v1/platform/config/dict` | @AdminOrHasAnyAuthority({"platform:config:dict:create"}) |
| `PlatformDictAPI.update` | PUT | `/v1/platform/config/dict` | @AdminOrHasAnyAuthority({"platform:config:dict:update"}) |
| `PlatformDictAPI.changeStatus` | PATCH | `/v1/platform/config/dict/{id}/status/{status}` | @AdminOrHasAnyAuthority({"platform:config:dict:update"}) |
| `PlatformDictAPI.sort` | PUT | `/v1/platform/config/dict/sort` | @AdminOrHasAnyAuthority({"platform:config:dict:update"}) |
| `PlatformDictAPI.removeById` | DELETE | `/v1/platform/config/dict/{id}` | @AdminOrHasAnyAuthority({"platform:config:dict:delete"}) |
| `PlatformMenuAPI.tree` | GET | `/v1/platform/config/menu/tree` | @AdminOrHasAnyAuthority({"platform:config:menu:query"}) |
| `PlatformPermissionAPI.tree` | GET | `/v1/platform/config/permission/tree` | @AdminOrHasAnyAuthority({"platform:config:permission:query"}) |
| `PlatformRoleAPI.options` | GET | `/v1/platform/config/role/options` | @AdminOrHasAnyAuthority({"platform:config:role:query"}) |
| `PlatformRoleAPI.conditionList` | GET | `/v1/platform/config/role/list` | @AdminOrHasAnyAuthority({"platform:config:role:query"}) |
| `PlatformRoleAPI.create` | POST | `/v1/platform/config/role` | @AdminOrHasAnyAuthority({"platform:config:role:create"}) |
| `PlatformRoleAPI.update` | PUT | `/v1/platform/config/role` | @AdminOrHasAnyAuthority({"platform:config:role:update"}) |
| `PlatformRoleAPI.delete` | DELETE | `/v1/platform/config/role/{id}` | @AdminOrHasAnyAuthority({"platform:config:role:delete"}) |
| `PlatformRoleAPI.bindAuthorities` | PUT | `/v1/platform/config/role/{id}/permissions` | @AdminOrHasAnyAuthority({"platform:config:role:permissions:assign"}) |
| `PlatformRoleAPI.getPermissions` | GET | `/v1/platform/config/role/{id}/permissions` | @AdminOrHasAnyAuthority({"platform:config:role:permissions:query"}) |
| `PlatformRoleAPI.getDataRules` | GET | `/v1/platform/config/role/{id}/data-rules` | @AdminOrHasAnyAuthority({"platform:config:role:data-rule:query"}) |
| `PlatformRoleAPI.setDataRules` | PUT | `/v1/platform/config/role/{id}/data-rules` | @AdminOrHasAnyAuthority({"platform:config:role:data-rule:set"}) |
| `DevAuthorizationAuditAPI.audit` | GET | `/v1/platform/dev/authorization/audit` | @RequiredAdmin |
| `DevIdAPI.page` | GET | `/v1/platform/dev/id/page` | @AdminOrHasAnyAuthority({"platform:develop:id:query"}) |
| `DevIdAPI.create` | POST | `/v1/platform/dev/id` | @AdminOrHasAnyAuthority({"platform:develop:id:create"}) |
| `DevIdAPI.update` | PUT | `/v1/platform/dev/id` | @AdminOrHasAnyAuthority({"platform:develop:id:update"}) |
| `DevIdAPI.remove` | DELETE | `/v1/platform/dev/id/{id}` | @AdminOrHasAnyAuthority({"platform:develop:id:delete"}) |
| `DevSocialAPI.page` | GET | `/v1/platform/dev/social/page` | @AdminOrHasAnyAuthority({"platform:develop:social:query"}) |
| `DevSocialAPI.create` | POST | `/v1/platform/dev/social` | @AdminOrHasAnyAuthority({"platform:develop:social:create"}) |
| `DevSocialAPI.update` | PUT | `/v1/platform/dev/social` | @AdminOrHasAnyAuthority({"platform:develop:social:update"}) |
| `DevSocialAPI.remove` | DELETE | `/v1/platform/dev/social/{id}` | @AdminOrHasAnyAuthority({"platform:develop:social:delete"}) |
| `AdminTenantAPI.search` | GET | `/v1/platform/org/tenant/search` | @AdminOrHasAnyAuthority({"platform:org:tenant:search"}) |
| `AdminTenantAPI.getTenantInfo` | GET | `/v1/platform/org/tenant/{id}` | @AdminOrHasAnyAuthority({"platform:org:tenant:detail"}) |
| `AdminTenantAPI.page` | GET | `/v1/platform/org/tenant/page` | @AdminOrHasAnyAuthority({"platform:org:tenant:query"}) |
| `AdminTenantAPI.getApps` | GET | `/v1/platform/org/tenant/apps` | @AdminOrHasAnyAuthority({"platform:org:tenant:app:query"}) |
| `AdminTenantAPI.updateAppStatus` | PUT | `/v1/platform/org/tenant/app/status` | @AdminOrHasAnyAuthority({"platform:org:tenant:app:update"}) |
| `AdminTenantAPI.create` | POST | `/v1/platform/org/tenant` | @AdminOrHasAnyAuthority({"platform:org:tenant:create"}) |
| `AdminTenantAPI.update` | PUT | `/v1/platform/org/tenant` | @AdminOrHasAnyAuthority({"platform:org:tenant:update"}) |
| `AdminTenantAPI.removeById` | DELETE | `/v1/platform/org/tenant/{id}` | @AdminOrHasAnyAuthority({"platform:org:tenant:delete"}) |
| `InnerAccountLockoutPolicyAPI.listPolicies` | GET | `/inner/security/account/lockout-policies` | @Permit(mode = PermitMode.INNER) |
| `InnerCredentialAPI.getPolicyConfigs` | GET | `/inner/credential/policy-configs` | @Permit(mode = PermitMode.INNER) |
| `InnerLoginFailurePolicyAPI.listPolicies` | GET | `/inner/security/access/login-failure-policies` | @Permit(mode = PermitMode.INNER) |
| `InnerSecurityEventAPI.report` | POST | `/inner/security/event/report` | @Permit(mode = PermitMode.INNER) |
| `InnerSecurityEventAPI.reportBatch` | POST | `/inner/security/event/report/batch` | @Permit(mode = PermitMode.INNER) |
| `InnerSecurityPolicyAPI.snapshot` | GET | `/inner/security/policy/snapshot` | @Permit(mode = PermitMode.INNER) |
| `InnerSecurityPolicyAPI.reportBlacklist` | POST | `/inner/security/blacklist/report` | @Permit(mode = PermitMode.INNER) |
| `InnerSessionConcurrencyPolicyAPI.listPolicies` | GET | `/inner/security/session/concurrency-policies` | @Permit(mode = PermitMode.INNER) |
| `AccountLockoutPolicyAPI.list` | GET | `/platform/security/account/lockout-policies` | @AdminOrHasAnyAuthority({"platform:security:account:lockout:query"}) |
| `AccountLockoutPolicyAPI.getByUserType` | GET | `/platform/security/account/lockout-policies/{userType}` | @AdminOrHasAnyAuthority({"platform:security:account:lockout:query"}) |
| `AccountLockoutPolicyAPI.upsert` | PUT | `/platform/security/account/lockout-policies` | @AdminOrHasAnyAuthority({"platform:security:account:lockout:update"}) |
| `CredentialPolicyConfigAPI.getAllPolicyConfigs` | GET | `/platform/security/credential/policy-config/list` | @AdminOrHasAnyAuthority({"platform:security:credential:policy:query"}) |
| `CredentialPolicyConfigAPI.savePolicyConfig` | POST | `/platform/security/credential/policy-config` | @AdminOrHasAnyAuthority({"platform:security:credential:policy:create"}) |
| `CredentialPolicyConfigAPI.updatePolicyConfig` | PUT | `/platform/security/credential/policy-config` | @AdminOrHasAnyAuthority({"platform:security:credential:policy:update"}) |
| `CredentialPolicyConfigAPI.deletePolicyConfig` | DELETE | `/platform/security/credential/policy-config/{id}` | @AdminOrHasAnyAuthority({"platform:security:credential:policy:delete"}) |
| `LoginFailureProtectionAPI.list` | GET | `/platform/security/access/login-failure-policies` | @AdminOrHasAnyAuthority({"platform:security:access:login-failure:query"}) |
| `LoginFailureProtectionAPI.getByDimension` | GET | `/platform/security/access/login-failure-policies/{dimension}` | @AdminOrHasAnyAuthority({"platform:security:access:login-failure:query"}) |
| `LoginFailureProtectionAPI.upsert` | PUT | `/platform/security/access/login-failure-policies` | @AdminOrHasAnyAuthority({"platform:security:access:login-failure:update"}) |
| `PlatformSessionAPI.page` | GET | `/platform/security/sessions` | @AdminOrHasAnyAuthority({"platform:security:session:query"}) |
| `PlatformSessionAPI.getBySid` | GET | `/platform/security/sessions/{sid}` | @AdminOrHasAnyAuthority({"platform:security:session:query"}) |
| `PlatformSessionAPI.revokeBySid` | DELETE | `/platform/security/sessions/{sid}` | @AdminOrHasAnyAuthority({"platform:security:session:revoke"}) |
| `PlatformSessionAPI.revokeByUser` | DELETE | `/platform/security/sessions/user` | @AdminOrHasAnyAuthority({"platform:security:session:revoke"}) |
| `SecurityPolicyAPI.listGroups` | GET | `/platform/security/policy/groups` | @AdminOrHasAnyAuthority({"platform:security:policy:query"}) |
| `SecurityPolicyAPI.saveGroup` | POST | `/platform/security/policy/groups` | @AdminOrHasAnyAuthority({"platform:security:policy:create"}) |
| `SecurityPolicyAPI.updateGroup` | PUT | `/platform/security/policy/groups` | @AdminOrHasAnyAuthority({"platform:security:policy:update"}) |
| `SecurityPolicyAPI.deleteGroup` | DELETE | `/platform/security/policy/groups/{id}` | @AdminOrHasAnyAuthority({"platform:security:policy:delete"}) |
| `SecurityPolicyAPI.listRules` | GET | `/platform/security/policy/rules` | @AdminOrHasAnyAuthority({"platform:security:policy:query"}) |
| `SecurityPolicyAPI.saveRule` | POST | `/platform/security/policy/rules` | @AdminOrHasAnyAuthority({"platform:security:policy:create"}) |
| `SecurityPolicyAPI.updateRule` | PUT | `/platform/security/policy/rules` | @AdminOrHasAnyAuthority({"platform:security:policy:update"}) |
| `SecurityPolicyAPI.deleteRule` | DELETE | `/platform/security/policy/rules/{id}` | @AdminOrHasAnyAuthority({"platform:security:policy:delete"}) |
| `SecurityPolicyAPI.listIpList` | GET | `/platform/security/policy/ip-list` | @AdminOrHasAnyAuthority({"platform:security:policy:query"}) |
| `SecurityPolicyAPI.saveIpList` | POST | `/platform/security/policy/ip-list` | @AdminOrHasAnyAuthority({"platform:security:policy:create"}) |
| `SecurityPolicyAPI.updateIpList` | PUT | `/platform/security/policy/ip-list` | @AdminOrHasAnyAuthority({"platform:security:policy:update"}) |
| `SecurityPolicyAPI.deleteIpList` | DELETE | `/platform/security/policy/ip-list/{id}` | @AdminOrHasAnyAuthority({"platform:security:policy:delete"}) |
| `SecurityPolicyAPI.listEvents` | GET | `/platform/security/policy/events` | @AdminOrHasAnyAuthority({"platform:security:policy:query"}) |
| `SecurityPolicyAPI.listChallengePolicies` | GET | `/platform/security/policy/challenges` | @AdminOrHasAnyAuthority({"platform:security:policy:query"}) |
| `SecurityPolicyAPI.saveChallengePolicy` | POST | `/platform/security/policy/challenges` | @AdminOrHasAnyAuthority({"platform:security:policy:create"}) |
| `SecurityPolicyAPI.updateChallengePolicy` | PUT | `/platform/security/policy/challenges` | @AdminOrHasAnyAuthority({"platform:security:policy:update"}) |
| `SecurityPolicyAPI.deleteChallengePolicy` | DELETE | `/platform/security/policy/challenges/{id}` | @AdminOrHasAnyAuthority({"platform:security:policy:delete"}) |
| `SecurityPolicyAPI.getViolationEscalation` | GET | `/platform/security/policy/violation-escalation` | @AdminOrHasAnyAuthority({"platform:security:policy:query"}) |
| `SecurityPolicyAPI.saveViolationEscalation` | PUT | `/platform/security/policy/violation-escalation` | @AdminOrHasAnyAuthority({"platform:security:policy:update"}) |
| `SecurityPolicyAPI.broadcastInvalidationAll` | POST | `/platform/security/policy/broadcast-invalidation` | @AdminOrHasAnyAuthority({"platform:security:policy:update"}) |
| `SessionConcurrencyPolicyAPI.list` | GET | `/platform/security/session/concurrency-policies` | @AdminOrHasAnyAuthority({"platform:security:session:policy:query"}) |
| `SessionConcurrencyPolicyAPI.getById` | GET | `/platform/security/session/concurrency-policies/{id}` | @AdminOrHasAnyAuthority({"platform:security:session:policy:query"}) |
| `SessionConcurrencyPolicyAPI.create` | POST | `/platform/security/session/concurrency-policies` | @AdminOrHasAnyAuthority({"platform:security:session:policy:update"}) |
| `SessionConcurrencyPolicyAPI.update` | PUT | `/platform/security/session/concurrency-policies` | @AdminOrHasAnyAuthority({"platform:security:session:policy:update"}) |
| `SessionConcurrencyPolicyAPI.delete` | DELETE | `/platform/security/session/concurrency-policies/{id}` | @AdminOrHasAnyAuthority({"platform:security:session:policy:update"}) |

## 来源

- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/TestAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/inner/InnerAuthorizationAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/inner/InnerDictAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/inner/InnerIdAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/inner/InnerLoginRecordAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/inner/InnerSocialDetailsAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/inner/InnerTenantDetailsAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/inner/InnerUserDetailsAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/auth/AuthUserAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/common/OSSCommonAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/org/OrgAuthAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/org/OrgDeptAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/org/OrgRoleAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/org/OrgUserAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/admin/SystemDeptAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/admin/SystemRoleAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/admin/SystemUserAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/config/PlatformApplicationAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/config/PlatformDictAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/config/PlatformMenuAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/config/PlatformPermissionAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/config/PlatformRoleAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/dev/DevAuthorizationAuditAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/dev/DevIdAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/dev/DevSocialAPI.java`
- `ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/web/v1/platform/org/AdminTenantAPI.java`
- `ingot-service/ingot-security/ingot-security-provider/src/main/java/com/ingot/cloud/security/web/inner/InnerAccountLockoutPolicyAPI.java`
- `ingot-service/ingot-security/ingot-security-provider/src/main/java/com/ingot/cloud/security/web/inner/InnerCredentialAPI.java`
- `ingot-service/ingot-security/ingot-security-provider/src/main/java/com/ingot/cloud/security/web/inner/InnerLoginFailurePolicyAPI.java`
- `ingot-service/ingot-security/ingot-security-provider/src/main/java/com/ingot/cloud/security/web/inner/InnerSecurityEventAPI.java`
- `ingot-service/ingot-security/ingot-security-provider/src/main/java/com/ingot/cloud/security/web/inner/InnerSecurityPolicyAPI.java`
- `ingot-service/ingot-security/ingot-security-provider/src/main/java/com/ingot/cloud/security/web/inner/InnerSessionConcurrencyPolicyAPI.java`
- `ingot-service/ingot-security/ingot-security-provider/src/main/java/com/ingot/cloud/security/web/platform/security/AccountLockoutPolicyAPI.java`
- `ingot-service/ingot-security/ingot-security-provider/src/main/java/com/ingot/cloud/security/web/platform/security/CredentialPolicyConfigAPI.java`
- `ingot-service/ingot-security/ingot-security-provider/src/main/java/com/ingot/cloud/security/web/platform/security/LoginFailureProtectionAPI.java`
- `ingot-service/ingot-security/ingot-security-provider/src/main/java/com/ingot/cloud/security/web/platform/security/PlatformSessionAPI.java`
- `ingot-service/ingot-security/ingot-security-provider/src/main/java/com/ingot/cloud/security/web/platform/security/SecurityPolicyAPI.java`
- `ingot-service/ingot-security/ingot-security-provider/src/main/java/com/ingot/cloud/security/web/platform/security/SessionConcurrencyPolicyAPI.java`
