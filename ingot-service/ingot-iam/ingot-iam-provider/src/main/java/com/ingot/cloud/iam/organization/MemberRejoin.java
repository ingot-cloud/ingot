package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.extension.BuiltinResourceProviders;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AccountQueryRepository;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.DelegationRepository;
import com.ingot.cloud.iam.persistence.DepartmentLeaderRepository;
import com.ingot.cloud.iam.persistence.EntitlementRepository;
import com.ingot.cloud.iam.persistence.GroupRepository;
import com.ingot.cloud.iam.persistence.MemberLifecycleRepository;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationGrantEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.policy.PolicyWriteRepository;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.authorization.field.FieldProjectionEngine;
import com.ingot.framework.authorization.field.FieldWriteExecutor;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberDepartmentBinding;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.TenantMemberCreateInput;
import com.ingot.framework.commons.model.iam.TenantMemberCreated;
import com.ingot.framework.commons.model.iam.TenantMemberRejoinInput;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>
 * 把已移出租户成员改回在职：覆盖通讯录与任职，并清掉旧授权关系。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class MemberRejoin {
    private static final String MEMBER_TARGET = "member";
    private static final String ASSIGNMENT_TARGET = "assignment";
    private static final String DELEGATION_TARGET = "delegation";
    private static final String MEMBER_VERSION = "member";
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final AssignmentService authorizationLocks;
    private final MemberQueryRepository members;
    private final MemberLifecycleRepository lifecycle;
    private final AccountQueryRepository accounts;
    private final AssignmentRepository assignments;
    private final DelegationRepository delegations;
    private final GroupRepository groups;
    private final DepartmentLeaderRepository leaders;
    private final EntitlementRepository entitlements;
    private final PolicyWriteRepository policies;
    private final FieldProjectionEngine projection;
    private final FieldWriteExecutor fieldWrites;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、字段门禁、关系清理与同一数据源事务。
     * <p>
     * TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。
     * </p>
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param authorizationLocks 授权锁
     * @param members 成员持久化
     * @param lifecycle 组织锁
     * @param accounts 账号查询
     * @param assignments 角色分配
     * @param delegations 委派
     * @param groups 用户组
     * @param leaders 部门负责人
     * @param entitlements 人群
     * @param policies 策略选择器
     * @param projection 提交字段投影
     * @param fieldWrites 字段写门禁
     * @param audits 同事务审计
     * @param changes 授权缓存失效
     * @param transactionManager 同一数据源事务
     */
    public MemberRejoin(IamAccess access, ResourceAccess scopes, AssignmentService authorizationLocks,
            MemberQueryRepository members, MemberLifecycleRepository lifecycle, AccountQueryRepository accounts,
            AssignmentRepository assignments, DelegationRepository delegations, GroupRepository groups,
            DepartmentLeaderRepository leaders, EntitlementRepository entitlements, PolicyWriteRepository policies,
            FieldProjectionEngine projection, FieldWriteExecutor fieldWrites, IamAuditWriter audits,
            AuthorizationChangeNotifier changes, PlatformTransactionManager transactionManager) {
        this.access = access;
        this.scopes = scopes;
        this.authorizationLocks = authorizationLocks;
        this.members = members;
        this.lifecycle = lifecycle;
        this.accounts = accounts;
        this.assignments = assignments;
        this.delegations = delegations;
        this.groups = groups;
        this.leaders = leaders;
        this.entitlements = entitlements;
        this.policies = policies;
        this.projection = projection;
        this.fieldWrites = fieldWrites;
        this.audits = audits;
        this.changes = changes;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 按成员 ID 把已移出资格重新加入当前组织。
     *
     * @param memberId 租户成员 ID
     * @param input 姓名、联系方式、任职与预期版本
     * @return 复用后的成员结果
     */
    public TenantMemberCreated rejoin(String memberId, TenantMemberRejoinInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_CREATE);
        if (input.hasUnknownFields()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        String phone = input.phone().trim();
        String displayName = input.displayName().trim();
        String email = blankToNull(input.email());
        Map<String, Boolean> departmentBindings = departmentBindings(input.departments());
        Set<String> departmentIds = Set.copyOf(departmentBindings.keySet());
        long targetId = IamIds.require(memberId);
        return transaction.execute(status -> {
            authorizationLocks.lockAuthorization(AuthorizationDomain.TENANT);
            access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_CREATE);
            long tenantId = IamIds.require(actor.context().tenantId());
            if (lifecycle.lockOwner(tenantId) == null) {
                throw new BizException(IamReasonCode.IDENTITY_INVALID);
            }
            IamTenantMemberEntity locked = members.lockTenantAny(tenantId, targetId);
            if (locked == null) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            if (locked.getStatus() != MemberStatus.REMOVED) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), TenantMemberOnboarding.MEMBERSHIP_EXISTS);
            }
            IamIds.requireVersion(input.expectedVersion(), version(locked));
            requireEnabledAccount(locked);
            scopes.requireCreate(actor.context(), IamAction.TENANT_MEMBER_CREATE, departmentIds);
            var resource = BuiltinResourceProviders.TENANT_MEMBER_RESOURCE;
            var profile = new TenantMemberCreateInput(displayName, phone, email, input.departments());
            var submitted = projection.submitted(profile, profile.suppliedFields(), resource)
                    .getOrDefault(resource, Map.of());
            fieldWrites.require(resource, IamAction.VALUE_TENANT_MEMBER_CREATE,
                    new ScopeTarget(IamIds.text(targetId), IamIds.text(targetId), actor.context().tenantId(),
                            List.copyOf(departmentIds)),
                    submitted);
            return applyLocked(actor, locked, displayName, phone, email, departmentBindings);
        });
    }

    /**
     * 调用方已持锁且完成创建权限与字段门禁后，复用已移出成员行。
     *
     * @param actor 当前身份
     * @param locked 已移出且已持锁的成员
     * @param displayName 新显示名
     * @param phone 组织通讯录手机号
     * @param email 组织通讯录邮箱，可空
     * @param departments 目标任职
     * @return 复用后的成员结果
     */
    public TenantMemberCreated applyLocked(ActiveIdentity actor, IamTenantMemberEntity locked, String displayName,
            String phone, String email, Map<String, Boolean> departments) {
        long tenantId = locked.getTenantId().longValueExact();
        long memberId = locked.getId().longValueExact();
        Map<String, Integer> cleared = clearRelations(actor, tenantId, memberId);
        for (String departmentId : departments.keySet()) {
            if (!members.lockDepartment(tenantId, IamIds.require(departmentId))) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
        }
        members.replaceDepartments(tenantId, memberId, departments);
        if (members.rejoinTenant(tenantId, memberId, displayName, phone, email, versionValue(locked)) != 1) {
            throw new BizException(IamReasonCode.REVISION_CONFLICT);
        }
        String version = nextVersion(locked);
        audits.write(actor.context(), access.nextId(), MEMBER_TARGET, IamIds.text(memberId), AuditChangeType.ENABLE,
                Map.of(AuditField.STATUS, MemberStatus.REMOVED, AuditField.NAME, locked.getDisplayName()),
                Map.of(AuditField.STATUS, MemberStatus.ACTIVE, AuditField.NAME, displayName, AuditField.SCOPE,
                        Map.of("departments", departments, "cleared", cleared)),
                Map.of(MEMBER_VERSION, version));
        changes.markAll();
        return new TenantMemberCreated(IamIds.text(memberId), version, Boolean.FALSE, null, Boolean.TRUE);
    }

    private Map<String, Integer> clearRelations(ActiveIdentity actor, long tenantId, long memberId) {
        Map<String, Integer> cleared = new LinkedHashMap<>();
        List<IamRoleAssignmentEntity> activeAssignments = assignments.listActiveTenantMember(tenantId, memberId);
        for (IamRoleAssignmentEntity row : activeAssignments) {
            if (assignments.revoke(row.getId().longValueExact(), row.getVersion()) != 1) {
                throw new BizException(IamReasonCode.REVISION_CONFLICT);
            }
            String id = IamIds.text(row.getId().longValueExact());
            audits.write(actor.context(), access.nextId(), ASSIGNMENT_TARGET, id, AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, row.getStatus().name()),
                    Map.of(AuditField.STATUS, GrantStatus.REVOKED.name()),
                    Map.of(ASSIGNMENT_TARGET, nextVersion(row.getVersion())),
                    text(row.getDelegationGrantId()), id);
        }
        cleared.put("assignments", activeAssignments.size());
        List<IamDelegationGrantEntity> administered = delegations.listActiveAdministeredBy(tenantId, memberId);
        for (IamDelegationGrantEntity row : administered) {
            long delegationId = row.getId().longValueExact();
            delegations.revoke(delegationId, row.getVersion());
            delegations.revokeDerivedAssignments(delegationId);
            String id = IamIds.text(delegationId);
            audits.write(actor.context(), access.nextId(), DELEGATION_TARGET, id, AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, GrantStatus.ACTIVE.name()),
                    Map.of(AuditField.STATUS, GrantStatus.REVOKED.name()),
                    Map.of(DELEGATION_TARGET, nextVersion(row.getVersion())), id, null);
        }
        cleared.put("delegations", administered.size());
        cleared.put("recipients", delegations.removeRecipientMember(tenantId, memberId));
        cleared.put("groups", groups.removeTenantMember(tenantId, memberId));
        cleared.put("leaders", leaders.removeMember(tenantId, memberId));
        cleared.put("audiences", entitlements.removeAudienceMember(tenantId, memberId));
        cleared.put("selectors", policies.removeSelectorMember(tenantId, memberId));
        return cleared;
    }

    private void requireEnabledAccount(IamTenantMemberEntity locked) {
        IamAccountEntity account = accounts.find(locked.getAccountId().longValueExact());
        if (account == null || !Boolean.TRUE.equals(account.getEnabled())) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID.getCode(),
                    TenantMemberOnboarding.PHONE_ACCOUNT_DISABLED);
        }
    }

    static Map<String, Boolean> departmentBindings(List<MemberDepartmentBinding> departments) {
        Map<String, Boolean> bindings = new LinkedHashMap<>();
        for (MemberDepartmentBinding binding : departments) {
            bindings.put(binding.id(), binding.primary());
        }
        return bindings;
    }

    private static String version(IamTenantMemberEntity row) {
        return versionValue(row).toString();
    }

    private static BigInteger versionValue(IamTenantMemberEntity row) {
        return row.getVersion() == null ? BigInteger.ZERO : row.getVersion();
    }

    private static String nextVersion(IamTenantMemberEntity row) {
        return versionValue(row).add(BigInteger.ONE).toString();
    }

    private static String nextVersion(BigInteger version) {
        return (version == null ? BigInteger.ZERO : version).add(BigInteger.ONE).toString();
    }

    private static String text(BigInteger id) {
        return id == null ? null : id.toString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
