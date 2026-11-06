package com.ingot.cloud.member.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.cloud.member.web.v1.platform.MemberUserAPI;
import com.ingot.framework.authorization.AuthorizationAccess;
import com.ingot.framework.authorization.AuthorizationClient;
import com.ingot.framework.authorization.SdkAuthorizationException;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.FieldMergeMode;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberAdminResources;
import com.ingot.framework.commons.model.iam.extension.ActionDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationRequest;
import com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision;
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

    private void grant(String action, AuthorizationDomain domain, List<ScopeCondition> scope) {
        var deadline = Instant.now().plusSeconds(30);
        var context = new AuthorizationContext(domain, domain == AuthorizationDomain.TENANT ? "11" : null, "1", "2");
        var fields = new FieldPolicyDecision(Map.of(), Map.of(), List.of(), Map.of(), Map.of(), FieldMergeMode.GRANTS, deadline);
        var key = MemberAdminResources.USER;
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
