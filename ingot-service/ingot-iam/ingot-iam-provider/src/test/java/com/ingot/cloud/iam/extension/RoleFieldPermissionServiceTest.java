package com.ingot.cloud.iam.extension;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.evaluation.ScopeClause;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity;
import com.ingot.framework.authorization.FieldPolicyProcessor;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.extension.ActionDescriptor;
import com.ingot.framework.commons.model.iam.extension.ExecutionMode;
import com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>
 * 检查发布字段快照、缺失快照拒绝及逐贡献字段求值。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
class RoleFieldPermissionServiceTest {

    private static final String RESOURCE = "10";

    private static final String PHONE = "phone";

    private static final String READ = "iam-ops:incident:read";

    private static final String WRITE = "iam-ops:incident:update";

    private static final ResourceKey KEY = new ResourceKey(AuthorizationDomain.PLATFORM, "iam-ops", "incident");

    private static final AuthorizationContext ACTOR = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1",
            "2");

    private static final FieldAccess MASKED = new FieldAccess(FieldVisibility.MASKED, false);

    private static final FieldAccess FULL = new FieldAccess(FieldVisibility.FULL, true);

    private final ResourceFieldMetadata metadata = mock(ResourceFieldMetadata.class);

    private final RoleRepository roles = mock(RoleRepository.class);

    private final ScopeTransportCompiler compiler = new ScopeTransportCompiler(
            mock(com.ingot.cloud.iam.evaluation.DepartmentClosure.class),
            mock(com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper.class));

    private RoleFieldPermissionService service;

    @BeforeEach
    void setup() {
        service = new RoleFieldPermissionService(metadata, roles, compiler, mock(AuthorizationEvaluator.class));
        var descriptor = new ResourceDescriptor(KEY,
                List.of(new ActionDescriptor(READ, ExecutionMode.READ_ONLY),
                        new ActionDescriptor(WRITE, ExecutionMode.MUTATING)),
                List.of(ScopeKind.ALL, ScopeKind.OBJECT_SET),
                List.of(new FieldCapability(PHONE, "手机号", List.of(FieldVisibility.values()), true, false, false)),
                Map.of(PHONE, MASKED), READ, false);
        when(metadata.resourceIds(anyList())).thenReturn(Set.of(BigInteger.TEN));
        when(metadata.load(anyCollection())).thenReturn(Map.of(RESOURCE, descriptor));
        when(metadata.require(KEY)).thenReturn(new ResourceFieldMetadata.Entry(RESOURCE, descriptor));
    }

    @Test
    void freezesSafeDefaultsAndRejectsUnregisteredFieldsResourcesAndEditing() {
        var grants = List.of(new ActionGrant("1", List.of(new ScopeExpression(ScopeKind.ALL, null, false))));
        assertEquals(Map.of(RESOURCE, Map.of(PHONE, MASKED)), service.freeze(RoleKind.PLATFORM_CUSTOM, grants, null));
        assertThrows(BizException.class,
                () -> service.freeze(RoleKind.PLATFORM_CUSTOM, grants, Map.of("other", Map.of(PHONE, FULL))));
        assertThrows(BizException.class,
                () -> service.freeze(RoleKind.PLATFORM_CUSTOM, grants, Map.of(RESOURCE, Map.of("unknown", FULL))));
        assertThrows(BizException.class, () -> service.freeze(RoleKind.PLATFORM_CUSTOM, grants,
                Map.of(RESOURCE, Map.of(PHONE, new FieldAccess(FieldVisibility.MASKED, true)))));
        assertThrows(BizException.class,
                () -> service.freeze(RoleKind.SHARED, grants, Map.of(RESOURCE, Map.of(PHONE, FULL))));
    }

    @Test
    void missingSnapshotIsRejectedAndEmptySnapshotDoesNotGrantFields() {
        assertThrows(BizException.class, () -> RoleFieldPermissionService.snapshot(null));
        assertThrows(BizException.class, () -> RoleFieldPermissionService.snapshot("null"));
        when(roles.findRevisions(anyCollection())).thenReturn(List.of(revision(1, null)));
        assertThrows(BizException.class, () -> service.evaluate(KEY, ACTOR,
                view(List.of(source(1, READ, ScopeClause.universe()))), List.of(READ)));
        when(roles.findRevisions(anyCollection())).thenReturn(List.of(revision(2, "{}")));
        var result = service.evaluate(KEY, ACTOR, view(List.of(source(2, READ, ScopeClause.universe()))),
                List.of(READ));
        assertEquals(FieldVisibility.HIDDEN, access(result.get(READ), "A").visibility());
    }

    @Test
    void loadsVersionsOnceAndRetainsScopeAndWriteBoundaries() {
        when(roles.findRevisions(anyCollection()))
            .thenReturn(List.of(revision(1, "{\"10\":{\"phone\":{\"visibility\":\"FULL\",\"editable\":true}}}"),
                    revision(2, "{\"10\":{\"phone\":{\"visibility\":\"MASKED\",\"editable\":false}}}")));
        var narrow = new ScopeClause(false, false, false, false, List.of(), false, List.of("A"));
        var contributions = List.of(source(1, READ, narrow), source(2, READ, ScopeClause.universe()),
                source(2, WRITE, ScopeClause.universe()));
        var result = service.evaluate(KEY, ACTOR, view(contributions), List.of(READ, WRITE));
        assertEquals(FULL, access(result.get(READ), "A"));
        assertEquals(MASKED, access(result.get(READ), "B"));
        assertEquals(MASKED, access(result.get(WRITE), "A"));
        verify(roles, times(1)).findRevisions(anyCollection());
        var removed = service.evaluate(KEY, ACTOR, view(List.of(source(2, READ, ScopeClause.universe()))),
                List.of(READ));
        assertEquals(MASKED, access(removed.get(READ), "A"));
        var revoked = service.evaluate(KEY, ACTOR, view(List.of()), List.of(READ));
        assertEquals(FieldVisibility.HIDDEN, access(revoked.get(READ), "A").visibility());
    }

    private static IamRoleRevisionEntity revision(long id, String fields) {
        var row = new IamRoleRevisionEntity();
        row.setId(BigInteger.valueOf(id));
        row.setResourceFieldPermissions(fields);
        return row;
    }

    private static AuthorizationEvaluator.RoleFieldSource source(long revision, String action, ScopeClause scope) {
        return new AuthorizationEvaluator.RoleFieldSource(BigInteger.valueOf(revision + 10), null, null, revision,
                action, List.of(scope));
    }

    private static AuthorizationEvaluator.AuthorizationView view(List<AuthorizationEvaluator.RoleFieldSource> sources) {
        return new AuthorizationEvaluator.AuthorizationView(List.of(READ, WRITE), List.of(), Map.of(), "1",
                Instant.now().plusSeconds(30), sources);
    }

    private static FieldAccess access(FieldPolicyDecision policy, String id) {
        return FieldPolicyProcessor.access(policy, new ScopeTarget(id, id, null, List.of())).get(PHONE);
    }

}
