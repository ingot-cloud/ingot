package com.ingot.cloud.iam.application.diagnose;

import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldProjection;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamAction;

import com.ingot.cloud.iam.domain.authorization.AuthorizationView;

import com.ingot.cloud.iam.application.audit.model.AuditActor;
import com.ingot.cloud.iam.application.audit.model.AuditEntry;
import com.ingot.cloud.iam.application.audit.model.AuditReference;
import com.ingot.cloud.iam.application.audit.model.AuditTarget;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.cloud.iam.domain.audit.AuditReferenceState;
import com.ingot.cloud.iam.domain.audit.AuditReferenceType;
import com.ingot.cloud.iam.domain.organization.MemberStatus;
import com.ingot.cloud.iam.domain.policy.MemberFieldKey;

import java.math.BigInteger;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.AuditReferenceMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.projection.AuditReferenceRows;
import com.ingot.cloud.iam.application.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.application.policy.FieldPolicySnapshot;
import com.ingot.framework.commons.model.iam.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <p>验证审计对象名称按读取权限、对象范围、字段策略披露，组合名称和已删除快照正确，且不按条数查询。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class AuditReferenceResolverTest {
    private static final ActiveIdentity TENANT_ACTOR = new ActiveIdentity(
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101"), "0", "0", null);
    private AuditReferenceMapper references;
    private IamTenantMemberMapper tenantMembers;
    private IamDepartmentMapper departments;
    private ResourceScopeAccessService scopes;
    private FieldAccessEvaluator fieldAccess;
    private AuditReferenceResolver resolver;

    @BeforeEach
    void setUp() {
        for (Class<?> entity : List.of(IamTenantMemberEntity.class, IamPlatformMemberEntity.class,
                IamDepartmentEntity.class)) {
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new Configuration(), entity.getSimpleName()), entity);
        }
        references = mock(AuditReferenceMapper.class);
        tenantMembers = mock(IamTenantMemberMapper.class);
        departments = mock(IamDepartmentMapper.class);
        scopes = mock(ResourceScopeAccessService.class);
        fieldAccess = mock(FieldAccessEvaluator.class);
        resolver = new AuditReferenceResolver(references, tenantMembers, mock(IamPlatformMemberMapper.class),
                departments, scopes, fieldAccess);
    }

    @Test
    void resolvesVisibleHiddenDeletedAndComposedNames() {
        when(scopes.memberRead(any(), eq(IamAction.TENANT_MEMBER_READ))).thenReturn(ObjectScope.all());
        when(scopes.departmentRead(any(), eq(IamAction.TENANT_DEPARTMENT_READ))).thenReturn(ObjectScope.all());
        when(tenantMembers.selectList(any()))
                .thenReturn(List.of(member(101, "管理者", MemberStatus.ACTIVE), member(102, "张三", MemberStatus.ACTIVE),
                        member(103, "李四", MemberStatus.REMOVED)))
                .thenReturn(List.of(member(101, null, null), member(102, null, null)));
        FieldPolicySnapshot snapshot = mock(FieldPolicySnapshot.class);
        when(fieldAccess.snapshot(10L, com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders.TENANT_MEMBER_RESOURCE)).thenReturn(snapshot);
        when(fieldAccess.access(eq(snapshot), eq(101L), eq(101L), eq(MemberFieldKey.VALUE_DISPLAY_NAME)))
                .thenReturn(new FieldAccess(FieldVisibility.FULL, true));
        when(fieldAccess.access(eq(snapshot), eq(101L), eq(102L), eq(MemberFieldKey.VALUE_DISPLAY_NAME)))
                .thenReturn(new FieldAccess(FieldVisibility.HIDDEN, false));
        when(departments.selectList(any())).thenReturn(List.of(department(7, "研发部")))
                .thenReturn(List.of(department(7, null)));
        when(references.assignments(BigInteger.TEN, java.util.Set.of(BigInteger.valueOf(9))))
                .thenReturn(List.of(new AuditReferenceRows.Assignment(BigInteger.valueOf(9), BigInteger.valueOf(88),
                        SubjectType.MEMBER, BigInteger.valueOf(101), null)));
        when(references.roles(eq(BigInteger.TEN), any())).thenReturn(List.of());
        var view = view(IamAction.TENANT_MEMBER_READ, IamAction.TENANT_DEPARTMENT_READ, IamAction.TENANT_ROLE_READ,
                IamAction.TENANT_ASSIGNMENT_READ);

        Map<String, AuditReference> result = resolver.resolve(TENANT_ACTOR, view, List.of(
                entry("member", "102", AuditChangeType.UPDATE, Map.of(), Map.of(AuditField.OWNER_MEMBER, "103"), "9", "5"),
                entry("role", "88", AuditChangeType.REMOVE, Map.of(AuditField.NAME, "旧角色"), Map.of(), null, null),
                entry("department", "7", AuditChangeType.UPDATE, Map.of(),
                        Map.of(AuditField.SCOPE, "{\"k\":{\"kind\":\"DEPARTMENTS\",\"ids\":[\"7\"]}}"), null, null)));

        assertEquals(AuditReference.visible(AuditReferenceType.MEMBER, "101", "管理者"), result.get("member:101"));
        assertEquals(AuditReference.hidden(AuditReferenceType.MEMBER, "102"), result.get("member:102"));
        assertEquals(AuditReference.deleted(AuditReferenceType.MEMBER, "103", null), result.get("member:103"));
        assertEquals(AuditReference.visible(AuditReferenceType.DEPARTMENT, "7", "研发部"), result.get("department:7"));
        assertEquals(AuditReference.deleted(AuditReferenceType.ROLE, "88", "旧角色"), result.get("role:88"));
        assertEquals(AuditReference.visible(AuditReferenceType.ASSIGNMENT, "9",
                AuditReferenceResolver.DELETED_PART + AuditReferenceResolver.NAME_JOINER + "管理者"),
                result.get("assignment:9"));
        assertEquals(AuditReference.hidden(AuditReferenceType.DELEGATION, "5"), result.get("delegation:5"));
        verify(tenantMembers, times(2)).selectList(any());
        verify(references, never()).delegations(any(), any());
    }

    @Test
    void withoutReadActionsEverythingIsHiddenWithoutQueries() {
        Map<String, AuditReference> result = resolver.resolve(TENANT_ACTOR, view(), List.of(
                entry("department", "7", AuditChangeType.UPDATE, Map.of(), Map.of(), "9", null),
                entry("tenant", "20", AuditChangeType.UPDATE, Map.of(), Map.of(), null, null)));
        assertEquals(AuditReferenceState.HIDDEN, result.get("member:101").state());
        assertEquals(AuditReferenceState.HIDDEN, result.get("department:7").state());
        assertEquals(AuditReferenceState.HIDDEN, result.get("assignment:9").state());
        assertEquals(AuditReferenceState.HIDDEN, result.get("tenant:20").state());
        assertNull(result.get("member:101").name());
        verifyNoInteractions(tenantMembers, departments, references);
    }

    @Test
    void tenantAccountUsesMemberDisclosureAndOwnTenantName() {
        when(references.tenantAccountMembers(eq(BigInteger.TEN), any()))
                .thenReturn(List.of(new AuditReferenceRows.AccountMember(BigInteger.valueOf(500), BigInteger.valueOf(101))));
        when(references.tenants(any())).thenReturn(List.of(new AuditReferenceRows.Name(BigInteger.TEN, "示例组织")));
        when(scopes.memberRead(any(), eq(IamAction.TENANT_MEMBER_READ))).thenReturn(ObjectScope.all());
        when(tenantMembers.selectList(any())).thenReturn(List.of(member(101, "管理者", MemberStatus.ACTIVE)))
                .thenReturn(List.of(member(101, null, null)));
        FieldPolicySnapshot snapshot = mock(FieldPolicySnapshot.class);
        when(fieldAccess.snapshot(anyLong(), any())).thenReturn(snapshot);
        when(fieldAccess.access(eq(snapshot), anyLong(), anyLong(), any()))
                .thenReturn(new FieldAccess(FieldVisibility.MASKED, false));
        Map<String, AuditReference> result = resolver.resolve(TENANT_ACTOR, view(IamAction.TENANT_MEMBER_READ),
                List.of(entry("account", "500", AuditChangeType.CREATE, Map.of(), Map.of(), null, null),
                        entry("entitlement", "10", AuditChangeType.UPDATE, Map.of(), Map.of(), null, null)));
        assertEquals(AuditReferenceState.VISIBLE, result.get("account:500").state());
        assertEquals(FieldProjection.project("管理者", FieldVisibility.MASKED), result.get("account:500").name());
        assertEquals("示例组织", result.get("tenant:10").name());
    }

    @Test
    void collectorReadsStructuredValuesAndIgnoresGarbage() {
        var collector = new AuditReferenceCollector();
        Map<AuditField, String> after = new LinkedHashMap<>();
        after.put(AuditField.DEPARTMENT_LEADERS,
                "{\"addedMemberIds\":[\"201\"],\"removedMemberIds\":[\"202\"],\"primaryMemberId\":\"201\"}");
        after.put(AuditField.RECIPIENT_SELECTION, "{\"type\":\"GROUP\",\"id\":\"301\"}");
        after.put(AuditField.SCOPE, "{\"administratorMemberId\":\"203\",\"allowedRoleIds\":[\"401\"],"
                + "\"resourceScopeCeilings\":[{\"scopeBindings\":{\"p\":{\"kind\":\"DEPARTMENTS\",\"ids\":[\"501\"]},"
                + "\"o\":{\"kind\":\"OBJECTS\",\"ids\":[\"999\"]}}}]}");
        after.put(AuditField.ROLE_REVISION, "not-an-id");
        Map<AuditField, String> before = new LinkedHashMap<>();
        before.put(AuditField.RECIPIENT_SELECTION, "before");
        before.put(AuditField.SCOPE, "{\"departments\":{\"502\":true},\"cleared\":{}}");
        before.put(AuditField.DEPARTMENT_LEADERS, "204");
        collector.collect(entry("department", "7", AuditChangeType.UPDATE, before, after, null, null));
        assertEquals(java.util.Set.of("101", "201", "202", "203", "204"), collector.ids(AuditReferenceType.MEMBER));
        assertEquals(java.util.Set.of("301"), collector.ids(AuditReferenceType.GROUP));
        assertEquals(java.util.Set.of("401"), collector.ids(AuditReferenceType.ROLE));
        assertEquals(java.util.Set.of("7", "501", "502"), collector.ids(AuditReferenceType.DEPARTMENT));
        assertEquals(java.util.Set.of(), collector.ids(AuditReferenceType.ROLE_REVISION));
        var selection = new AuditReferenceCollector();
        selection.collect(entry("delegation", "8", AuditChangeType.CREATE, Map.of(), Map.of(AuditField.RECIPIENT_SELECTION,
                "{\"members\":[\"601\"],\"departments\":[{\"id\":\"602\",\"includeDescendants\":true}]}"), null, null));
        assertEquals(java.util.Set.of("101", "601"), selection.ids(AuditReferenceType.MEMBER));
        assertEquals(java.util.Set.of("602"), selection.ids(AuditReferenceType.DEPARTMENT));
        var relations = new AuditReferenceCollector();
        relations.collect(entry("member", "9", AuditChangeType.UPDATE, Map.of(AuditField.SCOPE, "{\"701\":true}"),
                Map.of(AuditField.SCOPE, "{\"701\":false,\"702\":true}"), null, null));
        assertEquals(java.util.Set.of("701", "702"), relations.ids(AuditReferenceType.DEPARTMENT));
        assertEquals(AuditReferenceType.APPLICATION, AuditReferenceCollector.targetType("audience"));
        assertEquals(AuditReferenceType.ROLE_APPLICATION_TASK,
                AuditReferenceCollector.targetType("ROLE_VERSION_APPLICATION"));
        assertNull(AuditReferenceCollector.targetType("member-workbook"));
    }

    private static AuditEntry entry(String resource, String id, AuditChangeType type, Map<AuditField, String> before,
                                    Map<AuditField, String> after, String assignmentId, String delegationId) {
        return new AuditEntry("1", new AuditActor("1", "101", null), TENANT_ACTOR.context(),
                new AuditTarget(resource, id), type, before, after, Map.of(), delegationId, assignmentId,
                Instant.parse("2026-10-11T01:00:00Z"), null);
    }

    private static AuthorizationView view(IamAction... actions) {
        return new AuthorizationView(
                java.util.Arrays.stream(actions).map(IamAction::getCode).toList(), List.of(), Map.of(), "1",
                Instant.now().plusSeconds(30));
    }

    private static IamTenantMemberEntity member(long id, String name, MemberStatus status) {
        var row = new IamTenantMemberEntity();
        row.setId(BigInteger.valueOf(id));
        row.setDisplayName(name);
        row.setStatus(status);
        return row;
    }

    private static IamDepartmentEntity department(long id, String name) {
        var row = new IamDepartmentEntity();
        row.setId(BigInteger.valueOf(id));
        row.setName(name);
        return row;
    }
}
