package com.ingot.cloud.iam.delegation;

import java.math.BigInteger;
import java.time.*;
import java.util.*;
import com.ingot.cloud.iam.assignment.PlatformAuthorizationEditor;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.*;
import com.ingot.cloud.iam.persistence.entity.*;
import com.ingot.cloud.iam.role.*;
import com.ingot.cloud.iam.support.*;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>委派收窄逐维度检查派生授权，预览与事务提交均不能保存冲突。</p>
 * @author jy
 * @since 1.0.0
 */
class PlatformDelegationConflictTest {
    private static final AuthorizationDomain DOMAIN = AuthorizationDomain.PLATFORM;
    private final IamAccess access = mock(IamAccess.class);
    private final DelegationRepository store = mock(DelegationRepository.class);
    private final AssignmentRepository assignments = mock(AssignmentRepository.class);
    private final RoleService roles = mock(RoleService.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final Instant from = Instant.now().minus(Duration.ofDays(1));
    private final Instant until = Instant.now().plus(Duration.ofDays(1));
    private final IamRoleAssignmentEntity derived = new IamRoleAssignmentEntity();
    private final SimpleTransactionStatus tx = new SimpleTransactionStatus();
    private final DelegationService service;

    PlatformDelegationConflictTest() {
        var actor = new ActiveIdentity(new AuthorizationContext(DOMAIN, null, "1", "1001"), "0", "0", "0");
        when(access.requireGoverned(eq(DOMAIN), any())).thenReturn(actor);
        when(access.allows(eq(DOMAIN), any(), eq(true))).thenReturn(true);
        when(manager.getTransaction(any())).thenReturn(tx);
        var validator = mock(RoleGrantValidator.class);
        when(validator.validate(eq(DOMAIN), anyList(), anyList())).thenReturn(List.of());
        var editor = mock(PlatformAuthorizationEditor.class);
        when(editor.validCeilingObjects(any())).thenReturn(true);
        when(store.platformMemberExists(1001)).thenReturn(true);
        when(assignments.platformMemberActive(anyLong())).thenReturn(true);
        var revision = new IamRoleRevisionJoin(); revision.setKind(RoleKind.PLATFORM_CUSTOM);
        revision.setDomain(DOMAIN); revision.setEnabled(true);
        when(assignments.findRevision(anyLong())).thenReturn(revision);
        when(roles.synthesizedGrants(anyLong())).thenReturn(List.of(new ActionGrant("501",
                List.of(new ScopeExpression(ScopeKind.OBJECT_SET, "targets", null)))));
        var grant = new IamDelegationGrantEntity(); grant.setId(BigInteger.valueOf(60)); grant.setVersion(BigInteger.ZERO);
        grant.setPlatformAdministratorId(BigInteger.valueOf(1001)); grant.setStatus(GrantStatus.ACTIVE);
        grant.setMaxAssignmentDurationSeconds(Duration.ofDays(7).toSeconds()); grant.setMaxAssignmentDurationNanos(0);
        when(store.find(DOMAIN, null, 60)).thenReturn(grant);
        when(store.lock(DOMAIN, null, 60)).thenReturn(BigInteger.valueOf(60));
        when(store.children(DOMAIN, List.of(BigInteger.valueOf(60))))
                .thenReturn(Map.of(BigInteger.valueOf(60), new DelegationRepository.Children(
                        List.of(), List.of(), List.of(), List.of())));
        when(store.platformAdministratorNames(List.of(BigInteger.valueOf(1001)))).thenReturn(Map.of());
        when(access.capabilities(eq(actor), anyList())).thenReturn(new IamCapabilities(Map.of()));
        derived.setId(BigInteger.valueOf(90)); derived.setDomain(DOMAIN); derived.setRevisionId(BigInteger.valueOf(31));
        derived.setRevisionKind(RoleKind.PLATFORM_CUSTOM); derived.setSubjectType(SubjectType.MEMBER);
        derived.setPlatformMemberId(BigInteger.valueOf(1002)); derived.setScopeBindings(IamJson.object(Map.of(
                "targets", new ScopeBinding(ScopeBindingKind.OBJECTS, List.of("1002", "1003")))));
        derived.setValidFrom(LocalDateTime.ofInstant(from, ZoneOffset.UTC));
        derived.setValidUntil(LocalDateTime.ofInstant(until, ZoneOffset.UTC));
        when(store.activeDerivedAssignments(60)).thenReturn(List.of(derived));
        service = new DelegationService(access, mock(IamAuditWriter.class), new AuthorizationChangeNotifier(event -> { }),
                store, roles, assignments, validator, editor, manager);
    }
    private List<RoleRevisionRef> versions() { return List.of(new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "31")); }
    private DelegationInput input(List<RoleRevisionRef> versions, List<String> people, List<String> objects,
                                  Instant start, Instant end, Duration maximum) {
        return new DelegationInput("1001", versions, new Selection(people, List.of()),
                List.of(new ActionScopeCeiling("501", List.of(new ScopeExpression(ScopeKind.OBJECT_SET, "targets", null)),
                        Map.of("targets", new ScopeBinding(ScopeBindingKind.OBJECTS, objects)))), start, end, maximum);
    }
    private DelegationInput valid() {
        return input(versions(), List.of("1002", "1003"), List.of("1002", "1003"), from.minusSeconds(60),
                until.plusSeconds(60), Duration.ofDays(7));
    }
    @Test
    void previewChecksVersionsRecipientsObjectsAndPeriod() {
        var valid = valid();
        assertTrue(service.preview(DOMAIN, "60", new DelegationUpdateInput("0", valid)).valid());
        var cases = List.of(
                input(List.of(new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "32")), List.of("1002", "1003"), List.of("1002", "1003"), valid.validFrom(), valid.validUntil(), Duration.ofDays(7)),
                input(versions(), List.of("1003"), List.of("1002", "1003"), valid.validFrom(), valid.validUntil(), Duration.ofDays(7)),
                input(versions(), List.of("1002", "1003"), List.of("1002"), valid.validFrom(), valid.validUntil(), Duration.ofDays(7)),
                input(versions(), List.of("1002", "1003"), List.of("1002", "1003"), from.plusSeconds(1), valid.validUntil(), Duration.ofDays(7)),
                input(versions(), List.of("1002", "1003"), List.of("1002", "1003"), valid.validFrom(), until.minusSeconds(1), Duration.ofDays(7)),
                input(versions(), List.of("1002", "1003"), List.of("1002", "1003"), valid.validFrom(), valid.validUntil(), Duration.ofHours(1)));
        for (var next : cases) {
            var preview = service.preview(DOMAIN, "60", new DelegationUpdateInput("0", next));
            assertFalse(preview.valid()); assertEquals(List.of("90"), preview.effectiveResult().affectedAssignmentIds());
        }
    }
    @Test
    void submitRechecksGroupExpansionAfterPreviewAndRollsBackBeforeWriting() {
        var next = valid();
        assertTrue(service.preview(DOMAIN, "60", new DelegationUpdateInput("0", next)).valid());
        derived.setSubjectType(SubjectType.GROUP); derived.setPlatformGroupId(BigInteger.valueOf(80));
        when(assignments.platformGroupMemberIds(80)).thenReturn(List.of(BigInteger.valueOf(1002), BigInteger.valueOf(9999)));
        var failure = assertThrows(BizException.class, () -> service.replace(DOMAIN, "60", new DelegationUpdateInput("0", next)));
        assertEquals(IamReasonCode.POLICY_CONFLICT.getCode(), failure.getCode());
        verify(assignments).lockAuthorization(DOMAIN); verify(manager).rollback(tx);
        verify(store, never()).deleteChildren(anyLong());
    }
    @Test
    void conflictIdentifiersAndCountsRequireIndependentReadQualification() {
        when(access.allows(DOMAIN, IamAction.PLATFORM_ASSIGNMENT_READ, true)).thenReturn(false);
        var next = input(versions(), List.of("1002"), List.of("1002"), from.minusSeconds(60), until.plusSeconds(60), Duration.ofDays(7));
        var result = service.preview(DOMAIN, "60", new DelegationUpdateInput("0", next));
        assertFalse(result.valid()); assertTrue(result.effectiveResult().affectedAssignmentIds().isEmpty());
        assertNull(result.impactSummary().affectedAssignments()); assertTrue(result.impactSummary().restricted());
    }
    @Test
    void unlimitedAllowsLongDerivedAssignmentButLimitedShrinkRejectsEntireChange() {
        derived.setValidUntil(null);
        var current = valid();
        var unlimited = new DelegationInput(current.administratorMemberId(), current.allowedRoleRevisionRefs(),
                current.recipientSelection(), current.actionScopeCeilings(), current.validFrom(), current.validUntil(),
                null, AssignmentDurationMode.UNLIMITED);
        assertTrue(service.preview(DOMAIN, "60", new DelegationUpdateInput("0", unlimited)).valid());
        var limited = service.preview(DOMAIN, "60", new DelegationUpdateInput("0", current));
        assertFalse(limited.valid());
        assertEquals(List.of("90"), limited.effectiveResult().affectedAssignmentIds());
        assertThrows(BizException.class, () -> service.replace(DOMAIN, "60", new DelegationUpdateInput("0", current)));
        verify(store, never()).deleteChildren(anyLong());
    }
    @Test
    void boundedUnlimitedSourceCannotBeNarrowedBeforeFutureDerivedStart() {
        derived.setValidFrom(LocalDateTime.ofInstant(until, ZoneOffset.UTC));
        derived.setValidUntil(null);
        var current = valid();
        var next = new DelegationInput(current.administratorMemberId(), current.allowedRoleRevisionRefs(),
                current.recipientSelection(), current.actionScopeCeilings(), null, until.minusSeconds(1),
                null, AssignmentDurationMode.UNLIMITED);
        assertFalse(service.preview(DOMAIN, "60", new DelegationUpdateInput("0", next)).valid());
        assertThrows(BizException.class, () -> service.replace(DOMAIN, "60", new DelegationUpdateInput("0", next)));
        verify(store, never()).deleteChildren(anyLong());
    }

    @Test
    void administratorCannotBeIncludedAsDirectOrGroupRecipient() {
        var self = input(versions(), List.of("1001", "1002"), List.of("1002", "1003"), null, null, Duration.ofDays(7));
        assertFalse(service.previewCreate(DOMAIN, self).valid());
        derived.setSubjectType(SubjectType.GROUP); derived.setPlatformGroupId(BigInteger.valueOf(80));
        when(assignments.platformGroupMemberIds(80)).thenReturn(List.of(BigInteger.valueOf(1001), BigInteger.valueOf(1002)));
        assertFalse(service.preview(DOMAIN, "60", new DelegationUpdateInput("0", valid())).valid());
    }
    @Test
    void revocationRevokesDerivedAssignmentsAndCommitsTogether() {
        service.delete(DOMAIN, "60");
        verify(store).revoke(60, BigInteger.ZERO); verify(store).revokeDerivedAssignments(60); verify(manager).commit(tx);
    }
}
