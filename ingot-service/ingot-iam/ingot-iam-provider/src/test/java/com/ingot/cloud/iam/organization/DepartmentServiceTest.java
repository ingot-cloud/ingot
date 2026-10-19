package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.evaluation.ObjectCapabilities;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.DepartmentLeaderRepository;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.DepartmentDraft;
import com.ingot.framework.commons.model.iam.DepartmentUpdateInput;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.DepartmentRecord;
import com.ingot.framework.commons.model.iam.DepartmentTreeNode;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * <p>验证租户部门树写入直属 ACTIVE 人数，并将主部门与一级部门拉平。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {
    private static final AuthorizationContext ACTOR =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");

    @Mock
    private IamAccess access;
    @Mock
    private ResourceAccess scopes;
    @Mock
    private ObjectCapabilities capabilities;
    @Mock
    private IamAuditWriter audits;
    @Mock
    private DepartmentQueryRepository departments;
    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private DepartmentLeaderRepository leaderRelations;
    @Mock
    private DepartmentLeaderService leaders;

    private DepartmentService service;

    @BeforeEach
    void setup() {
        service = new DepartmentService(access, scopes, capabilities, audits, departments, transactionManager,
                leaders,
                leaderRelations);
        lenient().when(access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ))
                .thenReturn(new ActiveIdentity(ACTOR, "0", "0", "0"));
    }

    @Test
    void listReturnsNestedTreeAndFillsActiveMemberCount() {
        IamDepartmentEntity root = departmentRow(1, null, "英歌特云");
        IamDepartmentEntity product = departmentRow(2, 1L, "产品部");
        IamDepartmentEntity nested = departmentRow(3, 2L, "产品A组");
        ObjectCapabilities.Snapshot snapshot = mock(ObjectCapabilities.Snapshot.class);
        ObjectScope scope = ObjectScope.all();
        when(scopes.departmentRead(ACTOR, IamAction.TENANT_DEPARTMENT_READ)).thenReturn(scope);
        when(capabilities.snapshot(ACTOR)).thenReturn(snapshot);
        when(capabilities.departments(snapshot, List.of("1", "2", "3"))).thenReturn(Map.of(
                "1", Map.of(), "2", Map.of(), "3", Map.of()));
        when(departments.list(10L, scope)).thenReturn(List.of(root, product, nested));
        when(departments.activeMemberCounts(10L, List.of(1L, 2L, 3L))).thenReturn(new java.util.HashMap<>(Map.of(2L, 2)));
        when(departments.currentMemberCount(10L)).thenReturn(7);

        List<DepartmentTreeNode> tree = service.list();
        assertEquals(List.of("1"), tree.stream().map(node -> node.record().id()).toList());
        assertEquals(List.of("2"), tree.getFirst().children().stream().map(node -> node.record().id()).toList());
        assertEquals(List.of("3"), tree.getFirst().children().getFirst().children().stream()
                .map(node -> node.record().id()).toList());
        assertEquals(7, tree.getFirst().record().memberCount());
        assertEquals(2, tree.getFirst().children().getFirst().record().memberCount());
        verify(departments).activeMemberCounts(10L, List.of(1L, 2L, 3L));
    }

    @Test
    void getOmitsMemberCount() {
        IamDepartmentEntity row = departmentRow(11, null, "研发");
        ObjectCapabilities.Snapshot snapshot = mock(ObjectCapabilities.Snapshot.class);
        when(capabilities.snapshot(ACTOR)).thenReturn(snapshot);
        when(capabilities.departments(snapshot, List.of("11"))).thenReturn(Map.of("11", Map.of()));
        when(departments.find(10L, 11L)).thenReturn(row);

        ResourceDetail<DepartmentRecord> detail = service.get("11");
        assertEquals("研发", detail.record().name());
        assertNull(detail.record().memberCount());
        verify(scopes).requireVisibleDepartment(eq(ACTOR), eq(IamAction.TENANT_DEPARTMENT_READ), eq(11L));
        verify(departments).find(10L, 11L);
    }

    @Test
    void detailAggregatesVisibleParentAndBoundFirstPageWithoutWideningParentLookup() {
        var row = departmentRow(11, 1L, "研发");
        var actor = new ActiveIdentity(ACTOR, "0", "0", "0");
        var snapshot = mock(ObjectCapabilities.Snapshot.class);
        var scope = ObjectScope.none();
        var summary = new com.ingot.framework.commons.model.iam.DepartmentLeaderSummary(null, 0, false);
        var first = new com.ingot.framework.commons.model.iam.DepartmentLeaderPage(List.of(), 0, 1, 20, "0", false, null);
        when(departments.find(10L, 11L)).thenReturn(row);
        when(scopes.departmentRead(ACTOR, IamAction.TENANT_DEPARTMENT_READ)).thenReturn(scope);
        when(departments.findVisible(10L, 1L, scope)).thenReturn(departmentRow(1, null, "公司"));
        when(capabilities.snapshot(ACTOR)).thenReturn(snapshot);
        when(capabilities.departments(snapshot, List.of("11"))).thenReturn(Map.of("11", Map.of()));
        when(leaders.summaries(actor, List.of(row))).thenReturn(Map.of("11", summary));
        when(leaders.initialPage(actor, row, summary)).thenReturn(first);
        var detail = service.get("11");
        assertEquals("公司", detail.record().parentDepartment().name());
        assertEquals(first, detail.record().leaderPage());
        assertEquals(summary, detail.record().leaderSummary());
        assertNull(detail.record().memberCount());
        verify(departments).findVisible(10L, 1L, scope);
        verify(departments, never()).find(10L, 1L);
        verify(leaders).summaries(actor, List.of(row));
    }

    @Test
    void hiddenParentHasNoNameProjectionAndNoFallbackLookup() {
        var row = departmentRow(11, 1L, "研发");
        var snapshot = mock(ObjectCapabilities.Snapshot.class);
        var scope = ObjectScope.none();
        when(departments.find(10L, 11L)).thenReturn(row);
        when(scopes.departmentRead(ACTOR, IamAction.TENANT_DEPARTMENT_READ)).thenReturn(scope);
        when(capabilities.snapshot(ACTOR)).thenReturn(snapshot);
        when(capabilities.departments(snapshot, List.of("11"))).thenReturn(Map.of("11", Map.of()));
        var detail = service.get("11");
        assertEquals("1", detail.record().parentId());
        assertNull(detail.record().parentDepartment());
        assertNull(detail.record().leaderPage());
        verify(departments).findVisible(10L, 1L, scope);
        verify(departments, never()).find(10L, 1L);
    }

    @Test
    void legacyUpdateDoesNotExposeNewRelationsOutsideDepartmentReadScope() {
        when(access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_UPDATE))
                .thenReturn(new ActiveIdentity(ACTOR, "0", "0", "0"));
        when(access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ, false)).thenReturn(true);
        var row = departmentRow(11, 1L, "研发");
        when(departments.lock(10L, 11L)).thenReturn(row);
        when(departments.find(10L, 11L)).thenReturn(row);
        when(departments.find(10L, 1L)).thenReturn(departmentRow(1, null, "公司"));
        var snapshot = mock(ObjectCapabilities.Snapshot.class);
        when(capabilities.snapshot(ACTOR)).thenReturn(snapshot);
        when(capabilities.departments(snapshot, List.of("11"))).thenReturn(Map.of("11", Map.of()));
        when(scopes.departmentRead(ACTOR, IamAction.TENANT_DEPARTMENT_READ)).thenReturn(ObjectScope.none());
        var detail = service.update("11", new DepartmentUpdateInput("0", new DepartmentDraft("1", "研发", 1)));
        assertNull(detail.record().parentDepartment());
        assertNull(detail.record().leaderPage());
        verify(departments, never()).findVisible(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
        verify(leaders, never()).initialPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }


    @Test
    void departmentWithLeaderRelationshipsCannotBeDeletedEvenWithoutMembers() {
        when(access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_DELETE))
                .thenReturn(new ActiveIdentity(ACTOR, "0", "0", "0"));
        when(departments.lock(10L, 11L)).thenReturn(departmentRow(11, 1L, "研发"));
        when(leaderRelations.relationCount(10L, 11L)).thenReturn(1L);
        assertThrows(BizException.class, () -> service.delete("11"));
        verify(departments, never()).delete(10L, 11L);
    }

    @Test
    void departmentWithLeaderRelationshipsCannotBeConvertedIntoRoot() {
        when(access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_UPDATE))
                .thenReturn(new ActiveIdentity(ACTOR, "0", "0", "0"));
        when(departments.lock(10L, 11L)).thenReturn(departmentRow(11, 1L, "研发"));
        when(leaderRelations.relationCount(10L, 11L)).thenReturn(1L);
        assertThrows(BizException.class, () -> service.update("11", new DepartmentUpdateInput("0", new DepartmentDraft(null, "研发", 0))));
        verify(departments, never()).update(10L, 11L, null, "研发", 0, BigInteger.ZERO);
    }

    private static IamDepartmentEntity departmentRow(long id, Long parentId, String name) {
        IamDepartmentEntity row = new IamDepartmentEntity();
        row.setId(BigInteger.valueOf(id));
        row.setTenantId(BigInteger.TEN);
        row.setParentId(parentId == null ? null : BigInteger.valueOf(parentId));
        row.setName(name);
        row.setSortOrder((int) id);
        row.setVersion(BigInteger.ZERO);
        return row;
    }
}
