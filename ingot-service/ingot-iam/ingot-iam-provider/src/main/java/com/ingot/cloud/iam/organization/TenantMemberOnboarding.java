package com.ingot.cloud.iam.organization;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.extension.BuiltinResourceProviders;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AccountQueryRepository;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.authorization.field.FieldProjectionEngine;
import com.ingot.framework.authorization.field.FieldWriteExecutor;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberDepartmentBinding;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.TenantMemberCreateInput;
import com.ingot.framework.commons.model.iam.TenantMemberCreated;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.port.inbound.RegisterUserUseCase;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>
 * 按手机号把全局账号加入当前租户；没有账号时先签发初始密码再创建成员。命中已移出资格时复用原成员行重新加入。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class TenantMemberOnboarding {
    static final String PHONE_AMBIGUOUS = "该手机号对应多个账号，请联系平台管理员";
    static final String PHONE_ACCOUNT_DISABLED = "该手机号对应的账号已停用";
    static final String PHONE_USED_AS_USERNAME = "该手机号已被其他账号用作登录名";
    static final String MEMBERSHIP_EXISTS = "成员资格已存在";
    private static final String ACCOUNT_ENABLED = "ENABLED";
    private static final String ACCOUNT_VERSION = "account";
    private static final String MEMBER_TARGET = "member";
    private static final String ACCOUNT_TARGET = "account";
    private static final String INITIAL_VERSION = "0";

    private final IamAccess access;
    private final ResourceAccess scopes;
    private final AssignmentService assignments;
    private final MemberQueryRepository members;
    private final AccountQueryRepository accounts;
    private final FieldProjectionEngine projection;
    private final FieldWriteExecutor fieldWrites;
    private final IamAuditWriter audits;
    private final RegisterUserUseCase registerUser;
    private final InitialPasswordService initialPasswords;
    private final MemberRejoin rejoins;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、账号注册与成员写入。
     * <p>
     * TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。
     * </p>
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param assignments 授权锁
     * @param members 成员持久化
     * @param accounts 账号查询
     * @param projection 提交字段投影
     * @param fieldWrites 字段写门禁
     * @param audits 同事务审计
     * @param registerUser 注册用例
     * @param initialPasswords 初始密码
     * @param rejoins 已移出成员重新加入
     * @param transactionManager 同一数据源事务
     */
    public TenantMemberOnboarding(IamAccess access, ResourceAccess scopes, AssignmentService assignments,
            MemberQueryRepository members, AccountQueryRepository accounts, FieldProjectionEngine projection,
            FieldWriteExecutor fieldWrites, IamAuditWriter audits, RegisterUserUseCase registerUser,
            InitialPasswordService initialPasswords, MemberRejoin rejoins,
            PlatformTransactionManager transactionManager) {
        this.access = access;
        this.scopes = scopes;
        this.assignments = assignments;
        this.members = members;
        this.accounts = accounts;
        this.projection = projection;
        this.fieldWrites = fieldWrites;
        this.audits = audits;
        this.registerUser = registerUser;
        this.initialPasswords = initialPasswords;
        this.rejoins = rejoins;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 按手机号匹配或新建全局账号，并创建当前租户成员。
     *
     * @param input 姓名、手机号、邮箱与任职
     * @return 新成员及可选一次性密码
     */
    public TenantMemberCreated create(TenantMemberCreateInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_CREATE);
        if (input.hasUnknownFields()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        String phone = input.phone().trim();
        String displayName = input.displayName().trim();
        String email = blankToNull(input.email());
        Set<String> departmentIds = new LinkedHashSet<>();
        Map<String, Boolean> departmentBindings = new LinkedHashMap<>();
        for (MemberDepartmentBinding binding : input.departments()) {
            departmentIds.add(binding.id());
            departmentBindings.put(binding.id(), binding.primary());
        }
        return transaction.execute(status -> {
            assignments.lockAuthorization(AuthorizationDomain.TENANT);
            access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_CREATE);
            ResolvedAccount resolved = resolveAccount(actor, phone, email);
            long tenantId = IamIds.require(actor.context().tenantId());
            IamTenantMemberEntity existing = members.lockTenantByAccount(tenantId, resolved.accountId());
            if (existing != null && existing.getStatus() != MemberStatus.REMOVED) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MEMBERSHIP_EXISTS);
            }
            long id = existing == null ? access.nextId() : existing.getId().longValueExact();
            scopes.requireCreate(actor.context(), IamAction.TENANT_MEMBER_CREATE, departmentIds);
            var resource = BuiltinResourceProviders.TENANT_MEMBER_RESOURCE;
            var submitted = projection.submitted(input, input.suppliedFields(), resource).getOrDefault(resource,
                    Map.of());
            fieldWrites.require(resource, IamAction.VALUE_TENANT_MEMBER_CREATE,
                    new ScopeTarget(IamIds.text(id), IamIds.text(id), actor.context().tenantId(),
                            List.copyOf(departmentIds)),
                    submitted);
            if (existing != null) {
                return rejoins.applyLocked(actor, existing, displayName, phone, email, departmentBindings);
            }
            try {
                members.insertTenant(id, tenantId, resolved.accountId(), displayName, null, phone, email);
                replaceDepartments(tenantId, id, departmentBindings);
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MEMBERSHIP_EXISTS);
            }
            audits.write(actor.context(), access.nextId(), MEMBER_TARGET, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, displayName), Map.of(MEMBER_TARGET, INITIAL_VERSION));
            return new TenantMemberCreated(IamIds.text(id), INITIAL_VERSION, resolved.created(),
                    resolved.password(), Boolean.FALSE);
        });
    }

    private ResolvedAccount resolveAccount(ActiveIdentity actor, String phone, String email) {
        List<IamAccountEntity> matches = accounts.listByPhone(phone);
        if (matches.size() > 1) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), PHONE_AMBIGUOUS);
        }
        if (matches.size() == 1) {
            IamAccountEntity existing = matches.getFirst();
            if (!Boolean.TRUE.equals(existing.getEnabled())) {
                throw new BizException(IamReasonCode.IDENTITY_INVALID.getCode(), PHONE_ACCOUNT_DISABLED);
            }
            return new ResolvedAccount(existing.getId().longValueExact(), false, null);
        }
        if (accounts.findByUsername(phone) != null) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), PHONE_USED_AS_USERNAME);
        }
        String password = initialPasswords.generate();
        UserAccount saved;
        try {
            saved = registerUser.register(RegisterUserUseCase.RegisterUserCommand.builder()
                    .creationSource(RegisterUserUseCase.CreationSource.ADMIN_CREATE)
                    .userType(UserTypeEnum.ADMIN)
                    .username(phone)
                    .password(password)
                    .phone(phone)
                    .email(email)
                    .createdBy(IamIds.require(actor.context().accountId()))
                    .eventSource(EventSource.IAM)
                    .build());
        } catch (IllegalArgumentException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), PHONE_USED_AS_USERNAME);
        }
        if (saved == null || saved.getId() == null) {
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
        IamAccountEntity row = accounts.find(saved.getId());
        if (row == null) {
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
        audits.write(actor.context(), access.nextId(), ACCOUNT_TARGET, row.getId().toString(), AuditChangeType.CREATE,
                Map.of(), Map.of(AuditField.NAME, row.getUsername(), AuditField.STATUS, ACCOUNT_ENABLED),
                Map.of(ACCOUNT_TARGET, version(row)));
        return new ResolvedAccount(saved.getId(), true, password);
    }

    private void replaceDepartments(long tenantId, long memberId, Map<String, Boolean> departments) {
        for (String departmentId : departments.keySet()) {
            if (!members.lockDepartment(tenantId, IamIds.require(departmentId))) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
        }
        members.replaceDepartments(tenantId, memberId, departments);
    }

    private static String version(IamAccountEntity row) {
        return row.getVersion() == null ? INITIAL_VERSION : row.getVersion().toString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ResolvedAccount(long accountId, boolean created, String password) {
    }
}
