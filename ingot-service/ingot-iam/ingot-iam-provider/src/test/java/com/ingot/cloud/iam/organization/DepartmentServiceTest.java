package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.evaluation.ObjectCapabilities;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private DepartmentService service;

    @BeforeEach
    void setup() {
        service = new DepartmentService(access, scopes, capabilities, audits, departments, transactionManager);
        when(access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ))
                .thenReturn(new ActiveIdentity(ACTOR, "0", "0", "0"));
    }

    @Test
    void listReturnsFlattenedTreeAndFillsActiveMemberCount() {
        IamDepartmentEntity root = departmentRow(1, null, "英歌特云");
        IamDepartmentEntity product = departmentRow(2, 1L, "产品部");
        IamDepartmentEntity nested = departmentRow(3, 2L, "产品A组");
        ObjectCapabilities.Snapshot snapshot = mock(ObjectCapabilities.Snapshot.class);
        ObjectScope scope = ObjectScope.all();
        when(scopes.departmentRead(ACTOR, IamAction.TENANT_DEPARTMENT_READ)).thenReturn(scope);
        when(capabilities.snapshot(ACTOR)).thenReturn(snapshot);
        when(capabilities.department(snapshot, "1")).thenReturn(Map.of());
        when(capabilities.department(snapshot, "2")).thenReturn(Map.of());
        when(capabilities.department(snapshot, "3")).thenReturn(Map.of());
        when(departments.list(10L, scope)).thenReturn(List.of(root, product, nested));
        when(departments.activeMemberCounts(10L, List.of(1L, 2L, 3L))).thenReturn(Map.of(2L, 2));

        List<DepartmentTreeNode> tree = service.list();
        assertEquals(List.of("1", "2"), tree.stream().map(node -> node.record().id()).toList());
        assertTrue(tree.getFirst().children().isEmpty());
        assertEquals(List.of("3"), tree.get(1).children().stream().map(node -> node.record().id()).toList());
        assertEquals(0, tree.getFirst().record().memberCount());
        assertEquals(2, tree.get(1).record().memberCount());
        verify(departments).activeMemberCounts(10L, List.of(1L, 2L, 3L));
    }

    @Test
    void getOmitsMemberCount() {
        IamDepartmentEntity row = departmentRow(11, null, "研发");
        ObjectCapabilities.Snapshot snapshot = mock(ObjectCapabilities.Snapshot.class);
        when(capabilities.snapshot(ACTOR)).thenReturn(snapshot);
        when(capabilities.department(snapshot, "11")).thenReturn(Map.of());
        when(departments.find(10L, 11L)).thenReturn(row);

        ResourceDetail<DepartmentRecord> detail = service.get("11");
        assertEquals("研发", detail.record().name());
        assertNull(detail.record().memberCount());
        verify(scopes).requireVisibleDepartment(eq(ACTOR), eq(IamAction.TENANT_DEPARTMENT_READ), eq(11L));
        verify(departments).find(10L, 11L);
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
