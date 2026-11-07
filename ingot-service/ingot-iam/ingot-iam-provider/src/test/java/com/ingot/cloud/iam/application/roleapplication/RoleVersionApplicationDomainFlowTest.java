package com.ingot.cloud.iam.application.roleapplication;

import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationWork;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;

import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationPrepareInput;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationItemStatus;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationSelection;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationStatus;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Map;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.RoleVersionApplicationRepository;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleVersionApplicationItemEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleVersionApplicationTaskEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.domain.shared.AuthorizationAdmission;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpgradeInput;
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
class RoleVersionApplicationDomainFlowTest {
    private final IamAccess access = mock(IamAccess.class);
    private final RoleVersionApplicationPort domain = mock(RoleVersionApplicationPort.class);
    private final RoleVersionApplicationRepository store = mock(RoleVersionApplicationRepository.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final AuthorizationAuditWriter audits = mock(AuthorizationAuditWriter.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final RoleVersionApplicationService service = new RoleVersionApplicationService(access, domain, store, manager, audits, events);
    private final AuthorizationAdmission admission = new AuthorizationAdmission(new ActiveIdentity(new AuthorizationContext(
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
        var result = service.prepare("10", new RoleVersionApplicationPrepareInput("20", RoleVersionApplicationSelection.ALL,
                null, null, "prepare"));
        assertEquals(RoleVersionApplicationStatus.READY, result.status());
        assertEquals("20", result.targetRevisionId());
        var captured = ArgumentCaptor.forClass(IamRoleVersionApplicationTaskEntity.class);
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
        assertEquals(RoleVersionApplicationWork.BatchResult.STOP, service.executeTask(taskId));
        assertEquals(RoleVersionApplicationStatus.SUCCEEDED, task.getStatus());
        assertEquals(RoleVersionApplicationItemStatus.SUCCEEDED, item.getStatus());
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
        assertEquals(RoleVersionApplicationWork.BatchResult.STOP, service.executeTask(taskId));
        assertEquals(RoleVersionApplicationStatus.PAUSED, task.getStatus());
        assertEquals(RoleVersionApplicationItemStatus.BLOCKED, item.getStatus());
        assertNull(task.getLeaseOwner());
        verify(manager).rollback(any());
        verify(manager, times(2)).commit(any());
        verify(domain, never()).apply(any(), any());
        verify(domain, never()).admit();
        verifyNoInteractions(access, events);
    }

    @Test
    void listBatchKeepsTaskOrderTotalsAndVersionMetadataWithoutPerTaskQueries() {
        var records = new java.util.ArrayList<IamRoleVersionApplicationTaskEntity>();
        var counts = new java.util.LinkedHashMap<BigInteger, Map<RoleVersionApplicationItemStatus, Long>>();
        for (int id = 1; id <= 20; id++) {
            var row = new IamRoleVersionApplicationTaskEntity();
            row.setId(BigInteger.valueOf(id)); row.setRoleId(roleId); row.setTargetRevisionId(revisionId);
            row.setStatus(RoleVersionApplicationStatus.READY); row.setVersion(BigInteger.ZERO);
            row.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC)); row.setUpdatedAt(row.getCreatedAt());
            records.add(row); counts.put(row.getId(), Map.of(RoleVersionApplicationItemStatus.PENDING, (long) id));
        }
        var page = new Page<IamRoleVersionApplicationTaskEntity>(1,20,25);
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

    private IamRoleVersionApplicationTaskEntity running() {
        var task = new IamRoleVersionApplicationTaskEntity();
        task.setId(taskId); task.setRoleId(roleId); task.setTargetRevisionId(revisionId);
        task.setActorAccountId(BigInteger.ONE); task.setActorMemberId(BigInteger.TWO);
        task.setVersion(BigInteger.ZERO); task.setStatus(RoleVersionApplicationStatus.RUNNING);
        task.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC)); task.setUpdatedAt(task.getCreatedAt());
        when(store.runnableTask(eq(taskId), anyString())).thenReturn(task);
        when(store.lock(30L)).thenReturn(task); when(store.find(30L)).thenReturn(task);
        when(store.claim(eq(taskId), anyString())).thenAnswer(invocation -> {
            task.setLeaseOwner(invocation.getArgument(1)); return true;
        });
        return task;
    }

    private IamRoleVersionApplicationItemEntity item() {
        var item = new IamRoleVersionApplicationItemEntity();
        item.setId(BigInteger.valueOf(50)); item.setTaskId(taskId); item.setAssignmentId(BigInteger.valueOf(40));
        item.setExpectedAssignmentVersion(BigInteger.valueOf(5)); item.setConfiguredBindings("{}");
        item.setStatus(RoleVersionApplicationItemStatus.PENDING);
        return item;
    }
}
