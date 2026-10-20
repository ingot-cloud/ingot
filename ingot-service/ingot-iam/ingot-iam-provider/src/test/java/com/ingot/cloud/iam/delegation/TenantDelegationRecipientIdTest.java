package com.ingot.cloud.iam.delegation;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.iam.assignment.TenantLiveAssignmentService;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.TenantCurrentRoleRepository;
import com.ingot.cloud.iam.persistence.mapper.TenantLiveMapper;
import com.ingot.cloud.iam.persistence.projection.TenantLiveRows;
import com.ingot.cloud.iam.support.*;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * <p>验证租户委派公共创建入口为每个接收成员显式领取IAM Leaf标识。</p>
 * @author jy
 * @since 1.0.0
 */
class TenantDelegationRecipientIdTest {
    @Test
    void createsRecipientsWithSeparateAllocatedIdsInTheExistingTransaction() {
        var access = mock(IamAccess.class);
        var live = mock(TenantLiveMapper.class);
        var assignments = mock(TenantLiveAssignmentService.class);
        var resources = mock(ResourceAccess.class);
        var manager = mock(PlatformTransactionManager.class);
        var actor = new ActiveIdentity(new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101"), "0", "0", "0");
        var sequence = new AtomicLong(900000);
        when(access.nextId()).thenAnswer(invocation -> sequence.incrementAndGet());
        when(access.requireGoverned(eq(AuthorizationDomain.TENANT), any())).thenReturn(actor);
        when(live.lockTenant(BigInteger.TEN)).thenReturn(new TenantLiveRows.Owner(BigInteger.valueOf(101), BigInteger.ZERO));
        when(resources.objects(eq(actor.context()), any())).thenReturn(ObjectScope.all());
        when(live.members(eq(BigInteger.TEN), anyList())).thenReturn(1L);
        var role = new TenantLiveRows.Role(BigInteger.ONE, BigInteger.TEN, RoleKind.TENANT_CUSTOM,
                "fixture", "fixture", null, null, true, BigInteger.ZERO, BigInteger.ONE);
        when(live.role(BigInteger.TEN, BigInteger.ONE)).thenReturn(role);
        when(assignments.activeDefinition(actor, role)).thenReturn(new CurrentRoleDefinition(List.of(), List.of()));
        when(live.insertDelegation(any())).thenReturn(1);
        var transaction = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(transaction);
        var service = new TenantLiveDelegationService(access, live, mock(TenantCurrentRoleRepository.class),
                assignments, resources, new TenantLiveTransaction(manager), mock(IamAuditWriter.class),
                new AuthorizationChangeNotifier(event -> { }));
        var result = service.create(new TenantDelegationInput("101", List.of("1"),
                new Selection(List.of("102", "103"), List.of()), List.of(), null, null,
                Duration.ofHours(1), AssignmentDurationMode.LIMITED));
        assertEquals("900001", result.id());
        verify(live).recipientMember(BigInteger.valueOf(900002), BigInteger.TEN, BigInteger.valueOf(900001), BigInteger.valueOf(102));
        verify(live).recipientMember(BigInteger.valueOf(900003), BigInteger.TEN, BigInteger.valueOf(900001), BigInteger.valueOf(103));
        verify(manager).commit(transaction);
    }
}
