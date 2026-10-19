package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.DepartmentLeaderRepository;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.MemberLifecycleRepository;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.projection.DepartmentLeaderRow;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.policy.FieldPolicySnapshot;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证负责人差量写入的独立门禁、隐私边界及不可用关系保留。</p>
 * @author jy
 * @since 1.0.0
 */
class DepartmentLeaderServiceTest {
    private final IamAccess access = mock(IamAccess.class);
    private final ResourceAccess scopes = mock(ResourceAccess.class);
    private final DepartmentQueryRepository departments = mock(DepartmentQueryRepository.class);
    private final DepartmentLeaderRepository leaders = mock(DepartmentLeaderRepository.class);
    private final MemberLifecycleRepository lifecycle = mock(MemberLifecycleRepository.class);
    private final IamAuditWriter audits = mock(IamAuditWriter.class);
    private final ActiveIdentity actor = new ActiveIdentity(new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101"), "0", "0", "0");
    private final FieldAccessEvaluator fields = mock(FieldAccessEvaluator.class);
    private final DepartmentLeaderService service = new DepartmentLeaderService(access, scopes, departments, leaders,
            lifecycle, fields, audits, mock(OrganizationMemberDepartments.class));
    private IamDepartmentEntity department;

    @BeforeEach
    void setup() {
        department = new IamDepartmentEntity();
        department.setId(id(11)); department.setTenantId(id(10)); department.setParentId(id(1)); department.setVersion(BigInteger.ZERO);
        when(access.require(eq(AuthorizationDomain.TENANT), any())).thenReturn(actor);
        when(lifecycle.lockOwner(10)).thenReturn("101");
        when(departments.lock(10, 11)).thenReturn(department);
        when(departments.find(10, 11)).thenReturn(department);
        when(scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_READ)).thenReturn(ObjectScope.all());
    }

    @Test
    void departmentEditingDoesNotSubstituteForIndependentLeaderPermission() {
        when(access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_LEADERS))
                .thenThrow(new BizException(IamReasonCode.ACTION_DENIED));
        assertThrows(BizException.class, () -> service.update("11", input(List.of("102"), List.of(), "102")));
        verifyNoInteractions(leaders, audits);
    }

    @Test
    void departmentScopeIsRecheckedInsideMutation() {
        doThrow(new BizException(IamReasonCode.ACTION_DENIED)).when(scopes)
                .requireDepartmentWrite(actor.context(), IamAction.TENANT_DEPARTMENT_LEADERS, 11);
        assertThrows(BizException.class, () -> service.update("11", input(List.of("102"), List.of(), "102")));
        verify(leaders, never()).save(any(), any(), anyMap(), anyMap(), any());
    }

    @Test
    void rootAndStaleVersionRejectWithoutChangingRelations() {
        department.setParentId(null);
        assertThrows(BizException.class, () -> service.update("11", input(List.of(), List.of(), null)));
        department.setParentId(id(1)); department.setVersion(BigInteger.ONE);
        assertThrows(BizException.class, () -> service.update("11", input(List.of(), List.of(), null)));
        verify(leaders, never()).lockRelations(any(), any());
    }

    @Test
    void addingAMemberDoesNotRequireLoadingOrDeletingAnInvisibleExistingLeader() {
        when(leaders.lockRelations(id(10), id(11))).thenReturn(List.of(row(101, true, true)));
        when(leaders.lockMember(id(10), id(102))).thenReturn(row(102, false, true));
        when(leaders.visibleMember(eq(id(10)), eq(id(102)), any())).thenReturn(true);
        var result = service.update("11", input(List.of("102"), List.of(), null));
        assertEquals("1", result.version());
        verify(leaders).save(id(10), id(11), Map.of(id(101), true), Map.of(id(101), true, id(102), false), BigInteger.ZERO);
        verify(leaders, never()).visibleMember(eq(id(10)), eq(id(101)), any());
    }

    @Test
    void switchingPrimaryRequiresVisibilityOfBothOldAndNewLeader() {
        when(leaders.lockRelations(id(10), id(11))).thenReturn(List.of(row(101, true, true), row(102, false, true)));
        when(leaders.lockMember(id(10), id(101))).thenReturn(row(101, true, true));
        when(leaders.visibleMember(eq(id(10)), eq(id(101)), any())).thenReturn(false);
        assertThrows(BizException.class, () -> service.update("11", input(List.of(), List.of(), "102")));
        verify(leaders, never()).save(any(), any(), anyMap(), anyMap(), any());
    }

    @Test
    void unavailablePrimaryCanBePreservedButCannotBeNewlyAppointed() {
        when(leaders.lockRelations(id(10), id(11))).thenReturn(List.of(row(101, true, false)));
        assertEquals("0", service.update("11", input(List.of(), List.of(), null)).version());
        when(leaders.lockMember(id(10), id(102))).thenReturn(row(102, false, false));
        when(leaders.visibleMember(eq(id(10)), eq(id(102)), any())).thenReturn(true);
        assertThrows(BizException.class, () -> service.update("11", input(List.of("102"), List.of(), null)));
        verify(leaders, never()).save(any(), any(), anyMap(), anyMap(), any());
    }

    @Test
    void removingPrimaryRequiresExplicitReplacementAndDuplicateChangesReject() {
        when(leaders.lockRelations(id(10), id(11))).thenReturn(List.of(row(101, true, true), row(102, false, true)));
        assertThrows(BizException.class, () -> service.update("11", input(List.of(), List.of("101"), null)));
        assertThrows(BizException.class, () -> service.update("11", input(List.of("103", "103"), List.of(), null)));
        verify(leaders, never()).save(any(), any(), anyMap(), anyMap(), any());
    }


    @Test
    void missingMemberReadNeverQueriesOrProjectsLeaderIdentities() {
        var summary = service.summaries(actor, List.of(department)).get("11");
        assertTrue(summary.restricted());
        assertEquals(0, summary.visibleCount());
        assertNull(summary.primaryLeader());
        assertNull(service.initialPage(actor, department, summary));
        verifyNoInteractions(leaders, fields);
    }

    @Test
    void detailFirstPageUsesRelationScopeAndFieldProjectionWithoutRecounting() {
        when(access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ, false)).thenReturn(true);
        var raw = row(102, true, true); raw.setDisplayName("隐藏姓名");
        when(leaders.page(any())).thenReturn(List.of(raw));
        var snapshot = new FieldPolicySnapshot(10, PolicyScenario.MANAGEMENT, Map.of(), Map.of(), List.of(),
                List.of(), Map.of(), Map.of(), Map.of());
        when(fields.snapshotFresh(10, PolicyScenario.MANAGEMENT)).thenReturn(snapshot);
        when(fields.project(any(MemberRecord.class), any(), any(), anyMap())).thenAnswer(invocation -> {
            MemberRecord value = invocation.getArgument(0);
            return new MemberRecord(value.id(), null, null, null, null, null, value.status(), List.of());
        });
        var page = service.initialPage(actor, department, new DepartmentLeaderSummary(null, 21, true));
        assertEquals(21, page.total()); assertEquals(1, page.page()); assertEquals(20, page.pageSize());
        assertTrue(page.restricted()); assertNull(page.items().getFirst().displayName());
        verify(leaders).page(argThat(query -> query.tenantId().equals(id(10))
                && query.departmentId().equals(id(11)) && query.size() == 20 && query.offset() == 0
                && !query.candidates() && query.scope().coversAll()));
        verify(leaders, never()).count(any());
    }

    @Test
    void invisibleRelationshipsExposeOnlyRestrictedFlag() {
        when(access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ, false)).thenReturn(true);
        var hidden = row(999, true, true);
        hidden.setDepartmentId(id(11)); hidden.setVisibleCount(3L); hidden.setDisplayName("隐藏成员");
        when(leaders.summaries(any())).thenReturn(List.of(), List.of(hidden));
        var summary = service.summaries(actor, List.of(department)).get("11");
        assertTrue(summary.restricted());
        assertEquals(0, summary.visibleCount());
        assertNull(summary.primaryLeader());
        verify(fields, never()).project(any(), any(), any(), anyMap());
    }

    @Test
    void sharedPrimaryAcrossDepartmentsUsesFieldProjectionAndDoesNotExposeContacts() {
        when(access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ, false)).thenReturn(true);
        var second = new IamDepartmentEntity(); second.setId(id(12)); second.setParentId(id(1));
        var first = row(102, true, true); first.setDepartmentId(id(11)); first.setVisibleCount(1L); first.setDisplayName("原始姓名");
        var another = row(102, true, true); another.setDepartmentId(id(12)); another.setVisibleCount(1L); another.setDisplayName("原始姓名");
        when(leaders.summaries(any())).thenReturn(List.of(first, another));
        var snapshot = new FieldPolicySnapshot(10, PolicyScenario.MANAGEMENT, Map.of(), Map.of(), List.of(),
                List.of(), Map.of(), Map.of(), Map.of());
        when(fields.snapshotFresh(10, PolicyScenario.MANAGEMENT)).thenReturn(snapshot);
        when(fields.project(any(MemberRecord.class), any(), any(), anyMap())).thenAnswer(invocation -> {
            MemberRecord raw = invocation.getArgument(0);
            assertNull(raw.phone()); assertNull(raw.email()); assertNull(raw.loginPhone());
            return new MemberRecord(raw.id(), null, null, null, null, null, raw.status(), List.of());
        });
        var result = service.summaries(actor, List.of(department, second));
        assertEquals(2, result.size());
        assertNull(result.get("11").primaryLeader().displayName());
        assertNull(result.get("12").primaryLeader().displayName());
        assertFalse(result.get("11").restricted());
        verify(leaders, times(2)).summaries(any());
    }

    private static DepartmentLeaderUpdateInput input(List<String> added, List<String> removed, String primary) {
        return new DepartmentLeaderUpdateInput("0", added, removed, primary);
    }
    private static BigInteger id(long value) { return BigInteger.valueOf(value); }
    private static DepartmentLeaderRow row(long member, boolean primary, boolean available) {
        var result = new DepartmentLeaderRow(); result.setMemberId(id(member)); result.setPrimaryFlag(primary); result.setAvailable(available);
        return result;
    }
}
