package com.ingot.cloud.iam.organization;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator.AuthorizationView;
import com.ingot.cloud.iam.evaluation.ObjectCapabilities;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.extension.RoleFieldPermissionService;
import com.ingot.cloud.iam.group.GroupService;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.GroupRepository;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证字段列概览独立于分页样本，创建字段不借用查看或编辑权限。</p>
 * @author jy
 * @since 1.0.0
 */
class PlatformMemberFieldContextTest {
    private final IamAccess access = mock(IamAccess.class);
    private final ObjectCapabilities capabilities = mock(ObjectCapabilities.class);
    private final RoleFieldPermissionService fields = mock(RoleFieldPermissionService.class);
    private final MemberQueryRepository members = mock(MemberQueryRepository.class);
    private final AuthorizationContext actor = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");
    private final FieldAccess hidden = new FieldAccess(FieldVisibility.HIDDEN, false);
    private final FieldAccess full = new FieldAccess(FieldVisibility.FULL, true);
    private MemberQueryService service;

    @BeforeEach
    void setup() {
        when(access.requireCurrent()).thenReturn(new ActiveIdentity(actor, "0", "0", null));
        authorize(List.of(IamAction.VALUE_PLATFORM_MEMBER_READ, IamAction.VALUE_PLATFORM_MEMBER_CREATE));
        service = new MemberQueryService(access, mock(ResourceAccess.class), capabilities,
                mock(FieldAccessEvaluator.class), mock(IamAuditWriter.class), members, mock(GroupRepository.class),
                mock(AssignmentService.class), mock(GroupService.class), mock(PlatformTransactionManager.class), fields, com.ingot.cloud.iam.persistence.FieldTestSupport.projection(), mock(com.ingot.framework.authorization.field.FieldWriteExecutor.class));
    }

    @Test
    void columnsIncludeOffPageGrantButCreateUsesOnlyNewObjectGrant() {
        var defaults = Map.of("displayName", hidden, "phone", hidden, "email", hidden);
        var ceiling = Map.of("displayName", full, "phone", full, "email", full);
        var object = List.of(new ScopeCondition(false, List.of("9999"), null, List.of()));
        var all = List.of(new ScopeCondition(true, List.of(), null, List.of()));
        var read = new FieldPolicyDecision(defaults, ceiling,
                List.of(new ResolvedFieldRule("displayName", all, full), new ResolvedFieldRule("email", object, full)),
                Map.of("displayName", new com.ingot.framework.commons.model.iam.FieldOperations(false, true)), Map.of(), FieldMergeMode.GRANTS);
        var create = new FieldPolicyDecision(defaults, ceiling,
                List.of(new ResolvedFieldRule("phone", all, new FieldAccess(FieldVisibility.MASKED, false))),
                Map.of(), Map.of(), FieldMergeMode.GRANTS);
        when(fields.previewAll(any(), any(), anyList())).thenReturn(Map.of(
                IamAction.PLATFORM_MEMBER_READ, read, IamAction.PLATFORM_MEMBER_CREATE, create));
        var result = service.platformContext();
        assertEquals(FieldVisibility.FULL, result.listFieldVisibility().get("email"));
        assertEquals(FieldVisibility.HIDDEN, result.listFieldVisibility().get("phone"));
        assertEquals(FieldVisibility.MASKED, result.createFieldAccess().get("phone").visibility());
        assertEquals(hidden, result.createFieldAccess().get("email"));
        assertTrue(result.canSearchDisplayName());
        verifyNoInteractions(members);
    }

    @Test
    void partialOriginalVisibilityDoesNotEnableSearchAndMissingCeilingClosesColumn() {
        var object = List.of(new ScopeCondition(false, List.of("9999"), null, List.of()));
        var all = List.of(new ScopeCondition(true, List.of(), null, List.of()));
        var read = new FieldPolicyDecision(Map.of("displayName", hidden, "email", hidden), Map.of("displayName", full),
                List.of(new ResolvedFieldRule("displayName", object, full),
                        new ResolvedFieldRule("displayName", all, new FieldAccess(FieldVisibility.MASKED, false)),
                        new ResolvedFieldRule("email", all, full)),
                Map.of("displayName", new com.ingot.framework.commons.model.iam.FieldOperations(false, true)), Map.of(), FieldMergeMode.GRANTS);
        when(fields.previewAll(any(), any(), anyList())).thenReturn(Map.of(
                IamAction.PLATFORM_MEMBER_READ, read, IamAction.PLATFORM_MEMBER_CREATE, read));
        var result = service.platformContext();
        assertFalse(result.canSearchDisplayName());
        assertEquals(FieldVisibility.HIDDEN, result.listFieldVisibility().get("email"));
    }

    @Test
    void noMemberActionAndTenantIdentityCannotReadContext() {
        authorize(List.of());
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(),
                assertThrows(BizException.class, service::platformContext).getCode());
        when(access.requireCurrent()).thenReturn(new ActiveIdentity(
                new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101"), "0", "0", null));
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(),
                assertThrows(BizException.class, service::platformContext).getCode());
    }

    private void authorize(List<String> actions) {
        when(capabilities.snapshot(actor)).thenReturn(new ObjectCapabilities.Snapshot(actor,
                new AuthorizationView(actions, actions, Map.of(), "1", Instant.now().plusSeconds(60))));
    }
}
