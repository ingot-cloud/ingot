package com.ingot.cloud.member.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.service.biz.BizPermissionService;
import com.ingot.cloud.member.service.biz.BizRoleService;
import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.cloud.member.web.v1.platform.MemberPermissionAPI;
import com.ingot.cloud.member.web.v1.platform.MemberRoleAPI;
import com.ingot.cloud.member.web.v1.platform.MemberUserAPI;
import com.ingot.framework.authorization.AuthorizationAccess;
import com.ingot.framework.authorization.AuthorizationClient;
import com.ingot.framework.authorization.SdkAuthorizationException;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.FieldMergeMode;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberAdminResources;
import com.ingot.framework.commons.model.iam.extension.ActionDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationRequest;
import com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.extension.ScopeCondition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <p>验证 App 用户角色写入在服务调用前执行精确授权和平台全局范围校验。</p>
 * @author jy
 * @since 1.0.0
 */
class MemberPlatformAccessTest {
    private static final List<ScopeCondition> ALL = List.of(new ScopeCondition(true, List.of(), null, List.of()));
    private static final long USER_ID = 21L;

    private final AuthorizationClient client = mock(AuthorizationClient.class);
    private final BizUserService service = mock(BizUserService.class);
    private final MemberUserAPI api = new MemberUserAPI(new MemberGlobalAccess(new AuthorizationAccess(client)), service);

    @Test
    void appliesExplicitEmptyReplacementWhenGranted() {
        grant(MemberAdminResources.USER_ROLES_SET, AuthorizationDomain.PLATFORM, ALL);
        api.setUserRoles(USER_ID, emptySelection());
        verify(service).setUserRoles(USER_ID, List.of());
    }

    @Test
    void readPermissionCannotWriteAndTenantCannotUsePlatformOperation() {
        grant(MemberAdminResources.USER_ROLES_QUERY, AuthorizationDomain.PLATFORM, ALL);
        api.getUserRoles(USER_ID);
        verify(service).getUserRoles(USER_ID);
        assertThrows(SdkAuthorizationException.class, () -> api.setUserRoles(USER_ID, emptySelection()));

        grant(MemberAdminResources.USER_ROLES_SET, AuthorizationDomain.TENANT, ALL);
        assertThrows(SdkAuthorizationException.class, () -> api.setUserRoles(USER_ID, emptySelection()));
    }

    @Test
    void scopedDecisionAndUnavailableServiceDoNotExecuteWrites() {
        var scoped = new ScopeCondition(false, List.of(Long.toString(USER_ID)), null, List.of());
        grant(MemberAdminResources.USER_ROLES_SET, AuthorizationDomain.PLATFORM, List.of(scoped));
        assertThrows(SdkAuthorizationException.class, () -> api.setUserRoles(USER_ID, emptySelection()));
        when(client.evaluate(any())).thenThrow(new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE));
        assertThrows(SdkAuthorizationException.class, () -> api.setUserRoles(USER_ID, emptySelection()));
        verifyNoInteractions(service);
    }

    @Test
    void userProfileEditCannotEnableDisableOrUnlockAccount() {
        grant(MemberAdminResources.USER_UPDATE, AuthorizationDomain.PLATFORM, ALL);
        assertThrows(SdkAuthorizationException.class, () -> api.enableAccount(USER_ID, null));
        assertThrows(SdkAuthorizationException.class, () -> api.disableAccount(USER_ID, null));
        assertThrows(SdkAuthorizationException.class, () -> api.unlockAccount(USER_ID, null));
        verifyNoInteractions(service);

        grant(MemberAdminResources.USER_ENABLE, AuthorizationDomain.PLATFORM, ALL);
        api.enableAccount(USER_ID, null);
        verify(service).enableAccount(USER_ID, null);
        assertThrows(SdkAuthorizationException.class, () -> api.disableAccount(USER_ID, null));
    }

    @Test
    void mixedRoleAndPermissionUpdatesRequireBothOperations() {
        var access = new MemberGlobalAccess(new AuthorizationAccess(client));
        var roles = mock(BizRoleService.class);
        var permissions = mock(BizPermissionService.class);
        var role = new MemberRole();
        role.setId(USER_ID);
        role.setName("角色名称");
        role.setStatus(CommonStatusEnum.ENABLE);
        var permission = new MemberPermission();
        permission.setId(USER_ID);
        permission.setName("权限名称");
        permission.setStatus(CommonStatusEnum.ENABLE);
        var roleApi = new MemberRoleAPI(access, roles);
        var permissionApi = new MemberPermissionAPI(access, permissions);

        grant(MemberAdminResources.ROLE, MemberAdminResources.ROLE_CHANGE_STATUS, AuthorizationDomain.PLATFORM, ALL);
        grant(MemberAdminResources.PERMISSION, MemberAdminResources.PERMISSION_UPDATE, AuthorizationDomain.PLATFORM, ALL);
        assertThrows(SdkAuthorizationException.class, () -> roleApi.update(role));
        assertThrows(SdkAuthorizationException.class, () -> permissionApi.update(permission));
        verifyNoInteractions(roles, permissions);

        grant(MemberAdminResources.ROLE, MemberAdminResources.ROLE_UPDATE, AuthorizationDomain.PLATFORM, ALL);
        grant(MemberAdminResources.PERMISSION, MemberAdminResources.PERMISSION_CHANGE_STATUS, AuthorizationDomain.PLATFORM, ALL);
        roleApi.update(role);
        permissionApi.update(permission);
        verify(roles).update(role);
        verify(permissions).updateNonMenuPermission(permission);
    }

    private void grant(String action, AuthorizationDomain domain, List<ScopeCondition> scope) {
        grant(MemberAdminResources.USER, action, domain, scope);
    }

    private void grant(ResourceKey key, String action, AuthorizationDomain domain, List<ScopeCondition> scope) {
        var deadline = Instant.now().plusSeconds(30);
        var context = new AuthorizationContext(domain, domain == AuthorizationDomain.TENANT ? "11" : null, "1", "2");
        var fields = new FieldPolicyDecision(Map.of(), Map.of(), List.of(), Map.of(), Map.of(), FieldMergeMode.GRANTS, deadline);
        when(client.evaluate(new AuthorizationRequest(key, List.of(action))))
                .thenReturn(new AuthorizationDecision(key, context,
                        Map.of(action, new ActionDecision(true, false, scope, fields)), "1", deadline));
    }

    private SetDTO<Long, Long> emptySelection() {
        var selection = new SetDTO<Long, Long>();
        selection.setSetIds(List.of());
        return selection;
    }
}
