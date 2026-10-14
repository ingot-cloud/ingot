package com.ingot.cloud.iam.role;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Map;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.RoleApplicationRepository;
import com.ingot.cloud.iam.persistence.entity.IamRoleApplicationItemEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleApplicationTaskEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAdmission;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>以替代内部适配器验证共用流程不固定平台身份或版本引用，生产仍只有平台实现。</p>
 * @author jy
 * @since 1.0.0
 */
class RoleApplicationDomainFlowTest {
    private final IamAccess access = mock(IamAccess.class);
    private final RoleApplicationDomainSupport domain = mock(RoleApplicationDomainSupport.class);
    private final RoleApplicationRepository store = mock(RoleApplicationRepository.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final IamAuditWriter audits = mock(IamAuditWriter.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final RoleApplicationService service = new RoleApplicationService(access, domain, store, manager, audits, events);
    private final IamAdmission admission = new IamAdmission(new ActiveIdentity(new AuthorizationContext(
            AuthorizationDomain.TENANT, "3", "1", "2"), "1", "1", "1"), true);
    private final BigInteger roleId = BigInteger.valueOf(10);
    private final BigInteger revisionId = BigInteger.valueOf(20);
    private final BigInteger taskId = BigInteger.valueOf(30);

    @BeforeEach
    void setUp() {
        when(manager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        when(domain.admit()).thenReturn(admission);
        when(domain.scope(eq(admission), anyBoolean())).thenReturn(ObjectScope.all());
        when(domain.reference(revisionId)).thenReturn(new RoleRevisionRef(RoleKind.TENANT_CUSTOM, revisionId.toString()));
    }

    @Test
    void preparesThroughAdapterWithoutAssumingPlatformRoleKind() {
        var revision = new IamRoleRevisionEntity();
        revision.setId(revisionId); revision.setRoleId(roleId); revision.setKind(RoleKind.TENANT_CUSTOM);
        revision.setRevision(BigInteger.valueOf(4)); revision.setVersionName("范围调整");
        when(domain.requireTarget("10", "20")).thenReturn(revision);
        when(domain.findRevision(20L)).thenReturn(revision);
        when(access.nextId()).thenReturn(30L, 31L);
        var result = service.prepare("10", new RoleApplicationPrepareInput("20", RoleApplicationSelection.ALL,
                null, null, "prepare"));
        assertEquals(RoleApplicationStatus.READY, result.status());
        assertEquals("20", result.targetRevisionId());
        var captured = ArgumentCaptor.forClass(IamRoleApplicationTaskEntity.class);
        verify(store).insert(captured.capture());
        assertEquals(BigInteger.ONE, captured.getValue().getActorAccountId());
        assertEquals(BigInteger.TWO, captured.getValue().getActorMemberId());
        verify(domain).lockRole(10L);
        verify(domain).requireRole("10", admission, true);
        verify(domain, times(2)).scope(admission, true);
        verifyNoInteractions(events);
        verify(access, never()).admit(any(), any());
    }

    @Test
    void executesBatchWithRestoredIdentityAndAdapterReferenceInsideExistingTransactions() {
        var task = running(); var item = item();
        when(domain.restore(task)).thenReturn(admission);
        when(store.scan(eq(30L), eq(BigInteger.ZERO), any(), anyList()))
                .thenReturn(List.of(item), List.of());
        assertEquals(RoleApplicationWork.BatchResult.STOP, service.executeTask(taskId));
        assertEquals(RoleApplicationStatus.SUCCEEDED, task.getStatus());
        assertEquals(RoleApplicationItemStatus.SUCCEEDED, item.getStatus());
        var input = ArgumentCaptor.forClass(AssignmentUpgradeInput.class);
        var order = inOrder(domain, store);
        order.verify(domain).lockRole(10L);
        order.verify(store).claim(eq(taskId), anyString());
        order.verify(domain).lockRole(10L);
        order.verify(store).lock(30L);
        order.verify(domain).restore(task);
        order.verify(domain).requireRole("10", admission, true);
        order.verify(domain).apply(eq(admission), input.capture());
        assertEquals(RoleKind.TENANT_CUSTOM, input.getValue().targetRevisionRef().kind());
        assertEquals("40", input.getValue().items().getFirst().id());
        assertEquals("5", input.getValue().items().getFirst().expectedVersion());
        verify(manager, times(2)).commit(any());
        verify(domain, never()).admit();
        verifyNoInteractions(access, events);
    }

    @Test
    void revokedGovernanceRollsBackBatchAndPersistsPauseWithoutApplyingAssignments() {
        var task = running(); var item = item();
        when(domain.restore(task)).thenThrow(new BizException(IamReasonCode.ACTION_DENIED));
        when(store.scan(eq(30L), eq(BigInteger.ZERO), any(), anyList())).thenReturn(List.of(item));
        assertEquals(RoleApplicationWork.BatchResult.STOP, service.executeTask(taskId));
        assertEquals(RoleApplicationStatus.PAUSED, task.getStatus());
        assertEquals(RoleApplicationItemStatus.BLOCKED, item.getStatus());
        assertNull(task.getLeaseOwner());
        verify(manager).rollback(any());
        verify(manager, times(2)).commit(any());
        verify(domain, never()).apply(any(), any());
        verify(domain, never()).admit();
        verifyNoInteractions(access, events);
    }

    @Test
    void listBatchKeepsTaskOrderTotalsAndVersionMetadataWithoutPerTaskQueries() {
        var records = new java.util.ArrayList<IamRoleApplicationTaskEntity>();
        var counts = new java.util.LinkedHashMap<BigInteger, Map<RoleApplicationItemStatus, Long>>();
        for (int id = 1; id <= 20; id++) {
            var row = new IamRoleApplicationTaskEntity();
            row.setId(BigInteger.valueOf(id)); row.setRoleId(roleId); row.setTargetRevisionId(revisionId);
            row.setStatus(RoleApplicationStatus.READY); row.setVersion(BigInteger.ZERO);
            row.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC)); row.setUpdatedAt(row.getCreatedAt());
            records.add(row); counts.put(row.getId(), Map.of(RoleApplicationItemStatus.PENDING, (long) id));
        }
        var page = new Page<IamRoleApplicationTaskEntity>(1,20,25);
        page.setRecords(records);
        when(store.pageTasks(10, 1, 20)).thenReturn(page);
        var revision = new IamRoleRevisionEntity(); revision.setId(revisionId);
        revision.setRevision(BigInteger.valueOf(4)); revision.setVersionName("当前名称");
        when(domain.findRevisions(List.of(revisionId))).thenReturn(List.of(revision));
        when(store.counts(anyCollection(), any())).thenReturn(counts);
        var result = service.list("10", 1, 20);
        assertEquals(25, result.total()); assertEquals(20, result.items().size());
        assertEquals("1", result.items().getFirst().id()); assertEquals("20", result.items().getLast().id());
        assertEquals("当前名称", result.items().getFirst().targetVersionName());
        verify(domain, times(1)).findRevisions(List.of(revisionId));
        verify(store, times(1)).counts(anyCollection(), any());
        verify(domain, never()).findRevision(anyLong()); verify(store, never()).counts(anyLong(), any());
    }

    @Test
    void emptyTaskPageDoesNotLoadVersionsOrCounts() {
        when(store.pageTasks(10, 1, 20)).thenReturn(new Page<>(1,20,0));
        assertTrue(service.list("10", 1, 20).items().isEmpty());
        verify(domain, never()).findRevisions(anyCollection()); verify(store, never()).counts(anyCollection(), any());
    }

    private IamRoleApplicationTaskEntity running() {
        var task = new IamRoleApplicationTaskEntity();
        task.setId(taskId); task.setRoleId(roleId); task.setTargetRevisionId(revisionId);
        task.setActorAccountId(BigInteger.ONE); task.setActorMemberId(BigInteger.TWO);
        task.setVersion(BigInteger.ZERO); task.setStatus(RoleApplicationStatus.RUNNING);
        task.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC)); task.setUpdatedAt(task.getCreatedAt());
        when(store.runnableTask(eq(taskId), anyString())).thenReturn(task);
        when(store.lock(30L)).thenReturn(task); when(store.find(30L)).thenReturn(task);
        when(store.claim(eq(taskId), anyString())).thenAnswer(invocation -> {
            task.setLeaseOwner(invocation.getArgument(1)); return true;
        });
        return task;
    }

    private IamRoleApplicationItemEntity item() {
        var item = new IamRoleApplicationItemEntity();
        item.setId(BigInteger.valueOf(50)); item.setTaskId(taskId); item.setAssignmentId(BigInteger.valueOf(40));
        item.setExpectedAssignmentVersion(BigInteger.valueOf(5)); item.setConfiguredBindings("{}");
        item.setStatus(RoleApplicationItemStatus.PENDING);
        return item;
    }
}
