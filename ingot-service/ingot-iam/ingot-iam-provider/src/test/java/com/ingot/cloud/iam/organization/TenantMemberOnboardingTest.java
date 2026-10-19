package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.extension.BuiltinResourceProviders;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AccountQueryRepository;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.framework.authorization.field.FieldProjectionEngine;
import com.ingot.framework.authorization.field.FieldWriteExecutor;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberDepartmentBinding;
import com.ingot.framework.commons.model.iam.TenantMemberCreateInput;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.port.inbound.RegisterUserUseCase;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>验证按手机号添加租户成员：新建账号、复用账号与冲突拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class TenantMemberOnboardingTest {
    private static final String PHONE = "13800138000";
    private static final String PASSWORD = "once-secret";
    private static final AuthorizationContext TENANT = new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1",
            "101");

    private IamAccess access;
    private AccountQueryRepository accounts;
    private MemberQueryRepository members;
    private RegisterUserUseCase registerUser;
    private InitialPasswordService initialPasswords;
    private TenantMemberOnboarding service;

    @BeforeEach
    void setUp() {
        access = mock(IamAccess.class);
        ResourceAccess scopes = mock(ResourceAccess.class);
        AssignmentService assignments = mock(AssignmentService.class);
        members = mock(MemberQueryRepository.class);
        accounts = mock(AccountQueryRepository.class);
        FieldProjectionEngine projection = mock(FieldProjectionEngine.class);
        FieldWriteExecutor fieldWrites = mock(FieldWriteExecutor.class);
        IamAuditWriter audits = mock(IamAuditWriter.class);
        registerUser = mock(RegisterUserUseCase.class);
        initialPasswords = mock(InitialPasswordService.class);
        AtomicLong ids = new AtomicLong(9000);
        when(access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_CREATE))
                .thenReturn(new ActiveIdentity(TENANT, "0", "0", "0"));
        when(access.nextId()).thenAnswer(invocation -> ids.getAndIncrement());
        when(projection.submitted(any(), any(), eq(BuiltinResourceProviders.TENANT_MEMBER_RESOURCE)))
                .thenReturn(Map.of());
        when(members.lockDepartment(anyLong(), anyLong())).thenReturn(true);
        service = new TenantMemberOnboarding(access, scopes, assignments, members, accounts, projection, fieldWrites,
                audits, registerUser, initialPasswords, passthroughTransactions());
    }

    @Test
    void createsAccountAndMemberWhenPhoneIsNew() {
        when(accounts.listByPhone(PHONE)).thenReturn(List.of());
        when(initialPasswords.generate()).thenReturn(PASSWORD);
        when(registerUser.register(any())).thenReturn(UserAccount.builder().id(501L).username(PHONE).build());
        IamAccountEntity created = account(501, PHONE, true);
        when(accounts.find(501L)).thenReturn(created);

        var result = service.create(input());

        assertEquals("9001", result.id());
        assertTrue(result.accountCreated());
        assertEquals(PASSWORD, result.initialPassword());
        ArgumentCaptor<RegisterUserUseCase.RegisterUserCommand> command = ArgumentCaptor
                .forClass(RegisterUserUseCase.RegisterUserCommand.class);
        verify(registerUser).register(command.capture());
        assertEquals(PHONE, command.getValue().getUsername());
        assertEquals(PHONE, command.getValue().getPhone());
        assertEquals(PASSWORD, command.getValue().getPassword());
        verify(members).insertTenant(9001L, 10L, 501L, "张三", null, PHONE, "a@example.com");
    }

    @Test
    void joinsExistingAccountWithoutPassword() {
        when(accounts.listByPhone(PHONE)).thenReturn(List.of(account(88, PHONE, true)));

        var result = service.create(input());

        assertFalse(result.accountCreated());
        assertNull(result.initialPassword());
        verify(registerUser, never()).register(any());
        verify(members).insertTenant(9000L, 10L, 88L, "张三", null, PHONE, "a@example.com");
    }

    @Test
    void rejectsDuplicateMembership() {
        when(accounts.listByPhone(PHONE)).thenReturn(List.of(account(88, PHONE, true)));
        doThrow(new DuplicateKeyException("dup")).when(members).insertTenant(anyLong(), anyLong(), anyLong(), any(),
                any(), any(), any());

        BizException failure = assertThrows(BizException.class, () -> service.create(input()));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), failure.getCode());
        assertEquals(TenantMemberOnboarding.MEMBERSHIP_EXISTS, failure.getMessage());
    }

    @Test
    void rejectsAmbiguousPhone() {
        when(accounts.listByPhone(PHONE)).thenReturn(List.of(account(1, PHONE, true), account(2, PHONE, true)));

        BizException failure = assertThrows(BizException.class, () -> service.create(input()));
        assertEquals(TenantMemberOnboarding.PHONE_AMBIGUOUS, failure.getMessage());
        verify(members, never()).insertTenant(anyLong(), anyLong(), anyLong(), any(), any(), any(), any());
    }

    @Test
    void rejectsDisabledAccount() {
        when(accounts.listByPhone(PHONE)).thenReturn(List.of(account(88, PHONE, false)));

        BizException failure = assertThrows(BizException.class, () -> service.create(input()));
        assertEquals(IamReasonCode.IDENTITY_INVALID.getCode(), failure.getCode());
        assertEquals(TenantMemberOnboarding.PHONE_ACCOUNT_DISABLED, failure.getMessage());
    }

    @Test
    void rejectsPhoneUsedAsUsername() {
        when(accounts.listByPhone(PHONE)).thenReturn(List.of());
        when(accounts.findByUsername(PHONE)).thenReturn(account(9, null, true));

        BizException failure = assertThrows(BizException.class, () -> service.create(input()));
        assertEquals(TenantMemberOnboarding.PHONE_USED_AS_USERNAME, failure.getMessage());
        verify(registerUser, never()).register(any());
    }

    private static TenantMemberCreateInput input() {
        return new TenantMemberCreateInput("张三", PHONE, "a@example.com",
                List.of(new MemberDepartmentBinding("11", true)));
    }

    private static IamAccountEntity account(long id, String phone, boolean enabled) {
        IamAccountEntity row = new IamAccountEntity();
        row.setId(BigInteger.valueOf(id));
        row.setUsername(phone == null ? "other" : phone);
        row.setPhone(phone);
        row.setEnabled(enabled);
        row.setVersion(BigInteger.ZERO);
        return row;
    }

    private static PlatformTransactionManager passthroughTransactions() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }
}
