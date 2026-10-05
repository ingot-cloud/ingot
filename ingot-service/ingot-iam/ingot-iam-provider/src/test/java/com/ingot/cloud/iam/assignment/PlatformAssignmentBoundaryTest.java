package com.ingot.cloud.iam.assignment;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.delegation.DelegationAdmission;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.entity.*;
import com.ingot.cloud.iam.role.RoleService;
import com.ingot.cloud.iam.support.*;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>平台分配管理边界：不从业务操作或入口资格推断全域治理能力。</p>
 * @author jy
 * @since 1.0.0
 */
class PlatformAssignmentBoundaryTest {
    private final IamAccess access = mock(IamAccess.class);
    private final AssignmentRepository store = mock(AssignmentRepository.class);
    private final ActiveIdentity actor = new ActiveIdentity(
            new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001"), "0", "0", "0");
    private AssignmentService service(boolean governed) {
        when(access.require(eq(AuthorizationDomain.PLATFORM), any())).thenReturn(actor);
        when(access.requireCurrent()).thenReturn(actor);
        when(access.admit(eq(AuthorizationDomain.PLATFORM), any())).thenReturn(new IamAdmission(actor, governed));
        when(access.allows(eq(AuthorizationDomain.PLATFORM), any(), eq(false))).thenReturn(true);
        when(access.allows(eq(AuthorizationDomain.PLATFORM), any(), eq(true))).thenReturn(governed);
        when(access.capabilities(any(), anyList())).thenReturn(new IamCapabilities(Map.of()));
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return new AssignmentService(access, mock(IamAuditWriter.class), new AuthorizationChangeNotifier(event -> { }),
                mock(RoleService.class), mock(RoleRepository.class), store, mock(DelegationAdmission.class),
                mock(ResourceAccess.class), mock(PlatformAuthorizationEditor.class),
                mock(TenantScopeCandidates.class), manager);
    }
    @Test
    void restrictedListUsesOwnedSourcesRatherThanGlobalPage() {
        var service = service(false);
        when(store.pageOwned(1001, 1, 20, null, null, null))
                .thenReturn(new Page<IamRoleAssignmentEntity>(1, 20).setRecords(List.of()));
        assertTrue(service.list(AuthorizationDomain.PLATFORM, 1, 20).items().isEmpty());
        verify(store).pageOwned(1001, 1, 20, null, null, null);
        verify(store, never()).pagePlatform(anyInt(), anyInt(), any(), any(), any());
        verify(store, never()).page(any(), any(), anyInt(), anyInt());
    }
    @Test
    void restrictedListKeepsOwnerBoundaryWhenFilteringGroupNames() {
        var service = service(false);
        when(store.pageOwned(1001, 2, 20, SubjectType.GROUP, "运维", null))
                .thenReturn(new Page<IamRoleAssignmentEntity>(2, 20).setRecords(List.of()));
        assertTrue(service.list(AuthorizationDomain.PLATFORM, 2, 20, SubjectType.GROUP, " 运维 ").items().isEmpty());
        verify(store).pageOwned(1001, 2, 20, SubjectType.GROUP, "运维", null);
        verify(store, never()).pagePlatform(anyInt(), anyInt(), any(), any(), any());
    }
    @Test
    void stateFilterKeepsRestrictedAndGovernedQueriesSeparate() {
        var restricted = service(false);
        when(store.pageOwned(1001, 2, 20, SubjectType.GROUP, "运维", null, AssignmentEffectiveStatus.SOURCE_INVALID))
                .thenReturn(new Page<IamRoleAssignmentEntity>(2, 20).setRecords(List.of()));
        assertTrue(restricted.list(AuthorizationDomain.PLATFORM, 2, 20, SubjectType.GROUP, " 运维 ",
                AssignmentEffectiveStatus.SOURCE_INVALID).items().isEmpty());
        verify(store).pageOwned(1001, 2, 20, SubjectType.GROUP, "运维", null, AssignmentEffectiveStatus.SOURCE_INVALID);
        verify(store, never()).pagePlatform(anyInt(), anyInt(), any(), any(), any(), any());

        var governed = service(true);
        when(store.pagePlatform(1, 20, null, null, null, AssignmentEffectiveStatus.ACTIVE))
                .thenReturn(new Page<IamRoleAssignmentEntity>(1, 20).setRecords(List.of()));
        assertTrue(governed.list(AuthorizationDomain.PLATFORM, 1, 20, null, null,
                AssignmentEffectiveStatus.ACTIVE).items().isEmpty());
        verify(store).pagePlatform(1, 20, null, null, null, AssignmentEffectiveStatus.ACTIVE);
    }
    @Test
    void restrictedDetailAndRevokeRejectDirectAndOtherAdministratorsSources() {
        var service = service(false);
        var row = row(null); when(store.find(AuthorizationDomain.PLATFORM, null, 90)).thenReturn(row);
        when(store.lock(AuthorizationDomain.PLATFORM, null, 90)).thenReturn(row);
        assertThrows(BizException.class, () -> service.detail(AuthorizationDomain.PLATFORM, "90"));
        assertThrows(BizException.class, () -> service.selectedCandidates("90", AuthorizationCandidateKind.ROLE_REVISION, null, 1, 20));
        assertThrows(BizException.class, () -> service.delete(AuthorizationDomain.PLATFORM, "90"));
        row.setDelegationGrantId(BigInteger.valueOf(60));
        var source = new IamDelegationGrantEntity(); source.setPlatformAdministratorId(BigInteger.valueOf(1002));
        when(store.findDelegation(AuthorizationDomain.PLATFORM, null, 60)).thenReturn(source);
        assertThrows(BizException.class, () -> service.detail(AuthorizationDomain.PLATFORM, "90"));
        assertThrows(BizException.class, () -> service.selectedCandidates("90", AuthorizationCandidateKind.ROLE_REVISION, null, 1, 20));
        assertThrows(BizException.class, () -> service.delete(AuthorizationDomain.PLATFORM, "90"));
        verify(store, never()).revoke(anyLong(), any());
    }
    @Test
    void assignmentEditCannotSwitchVersionOrClearSourceEvenForGovernance() {
        var service = service(true); var row = row(BigInteger.valueOf(60));
        when(store.lock(AuthorizationDomain.PLATFORM, null, 90)).thenReturn(row);
        var next = new AssignmentInput(new SubjectRef(SubjectType.MEMBER, "1002"),
                new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "32"), Map.of(), Instant.now(), null, null);
        assertThrows(BizException.class, () -> service.replace(AuthorizationDomain.PLATFORM, "90", new AssignmentUpdateInput("0", next)));
        verify(store, never()).update(anyLong(), anyLong(), any(), any(), any(), any(), any());
    }
    @Test
    void restrictedPersonnelShortcutCannotAddOrRemoveSimpleRoles() {
        var service = service(false);
        assertThrows(BizException.class, () -> service.grantDirectRoles(AuthorizationDomain.PLATFORM, "1002", List.of("22")));
        assertThrows(BizException.class, () -> service.replaceDirectRoles(AuthorizationDomain.PLATFORM, "1002", List.of()));
        verify(store, never()).insert(any()); verify(store, never()).revoke(anyLong(), any());
    }
    @Test
    void recordDistinguishesCreationValidityStatesAndUnknownAuditAuthor() {
        var service = service(true);
        var row = row(null);
        row.setSource(AssignmentSource.MANUAL);
        row.setScopeBindings("{}");
        var createdAt = java.time.LocalDateTime.of(2026, 9, 1, 0, 0);
        row.setCreatedAt(createdAt);
        var now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        row.setValidFrom(now.plusDays(1));
        when(store.find(AuthorizationDomain.PLATFORM, null, 90)).thenReturn(row);
        var display = new com.ingot.cloud.iam.persistence.projection.AssignmentPresentation(
                row.getId(), "接收人", "角色", BigInteger.ONE, null, null, true);
        when(store.presentation(List.of(row.getId()))).thenReturn(Map.of(row.getId(), display));
        var detail = service.detail(AuthorizationDomain.PLATFORM, "90").record();
        assertEquals(AssignmentEffectiveStatus.PENDING, detail.effectiveStatus());
        assertEquals(createdAt.toInstant(java.time.ZoneOffset.UTC), detail.createdAt());
        assertNotEquals(detail.createdAt(), detail.assignment().validFrom());
        assertNull(detail.grantedBy().memberId());
        assertEquals("未知", detail.grantedBy().name());
        row.setValidFrom(now.minusDays(1));
        assertEquals(AssignmentEffectiveStatus.ACTIVE, service.detail(AuthorizationDomain.PLATFORM, "90").record().effectiveStatus());
        row.setValidUntil(now.minusHours(1));
        assertEquals(AssignmentEffectiveStatus.EXPIRED, service.detail(AuthorizationDomain.PLATFORM, "90").record().effectiveStatus());
        row.setValidUntil(null);
        row.setStatus(GrantStatus.REVOKED);
        assertEquals(AssignmentEffectiveStatus.REVOKED, service.detail(AuthorizationDomain.PLATFORM, "90").record().effectiveStatus());
        row.setStatus(GrantStatus.ACTIVE);
        when(store.presentation(List.of(row.getId()))).thenReturn(Map.of(row.getId(),
                new com.ingot.cloud.iam.persistence.projection.AssignmentPresentation(
                        row.getId(), "接收人", "角色", BigInteger.ONE, null, null, false)));
        var invalid = service.detail(AuthorizationDomain.PLATFORM, "90");
        assertEquals(AssignmentEffectiveStatus.SOURCE_INVALID, invalid.record().effectiveStatus());
        assertFalse(invalid.capabilities().get(IamAction.PLATFORM_ASSIGNMENT_UPDATE.getCode()).allowed());
    }
    private static IamRoleAssignmentEntity row(BigInteger source) {
        var row = new IamRoleAssignmentEntity(); row.setId(BigInteger.valueOf(90)); row.setVersion(BigInteger.ZERO);
        row.setStatus(GrantStatus.ACTIVE); row.setDomain(AuthorizationDomain.PLATFORM); row.setSubjectType(SubjectType.MEMBER);
        row.setPlatformMemberId(BigInteger.valueOf(1002)); row.setRevisionKind(RoleKind.PLATFORM_CUSTOM);
        row.setRevisionId(BigInteger.valueOf(31)); row.setDelegationGrantId(source); return row;
    }
}
