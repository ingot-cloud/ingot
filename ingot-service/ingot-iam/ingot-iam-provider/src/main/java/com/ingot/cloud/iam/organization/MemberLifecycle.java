package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ingot.cloud.iam.identity.ActiveIdentityService;
import com.ingot.cloud.iam.persistence.MemberLifecycleRepository;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.MemberStatusInput;
import com.ingot.framework.commons.model.iam.VersionInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>原子变更独立成员资格或租户任职关系，保护所有者并同步保存脱敏审计。</p>
 *
 * <p>只修改当前成员，不删除账号、不修改其他域成员。HTTP 必须注入真实 {@link MemberMutationGuard}，
 * 不能使用空实现；受保护入口由 {@link MemberCommandService} 编排。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class MemberLifecycle {
    private static final String MEMBER_RESOURCE = "member";
    private static final String MEMBER_VERSION = "member";
    private static final BigInteger MAX_ID = new BigInteger("18446744073709551615");
    private final TransactionTemplate transaction;
    private final ActiveIdentityService identities;
    private final MemberLifecycleRepository members;
    private final IamAuditWriter audits;

    /**
     * 绑定同一数据源事务、身份校验与成员仓库。
     * @param transactionManager 管理 IAM 数据源的事务管理器；Lombok 无法表达 TransactionTemplate 装配
     * @param identities 实时身份服务
     * @param members 成员行锁与写入
     * @param audits 同事务审计
     */
    public MemberLifecycle(PlatformTransactionManager transactionManager, ActiveIdentityService identities,
                           MemberLifecycleRepository members, IamAuditWriter audits) {
        this.transaction = new TransactionTemplate(transactionManager);
        this.identities = identities;
        this.members = members;
        this.audits = audits;
    }

    /**
     * 暂停或恢复成员资格，要求当前操作范围覆盖该成员全部部门。
     * @param actor 可信当前身份
     * @param memberId 当前域成员 ID
     * @param input 状态和预期版本；不接受 REMOVED
     * @param auditId 服务器发号得到的审计 ID
     * @param guard 事务内实际 ACTION 与范围校验
     * @return 提交后的成员版本
     * @throws BizException 不可见、版本冲突、所有者停用或资格已移出时拒绝
     */
    public String changeStatus(AuthorizationContext actor, String memberId, MemberStatusInput input,
                               long auditId, MemberMutationGuard guard) {
        if (input == null || input.status() == null || input.status() == MemberStatus.REMOVED) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return transition(actor, memberId, input.expectedVersion(), input.status(), auditId, guard);
    }

    /**
     * 保留成员历史标识并移出当前域，不物理删除账号、任职历史引用或其他成员身份。
     * @param actor 可信当前身份
     * @param memberId 当前域成员 ID
     * @param input 预期成员版本
     * @param auditId 服务器发号得到的审计 ID
     * @param guard 全部门覆盖及移出 ACTION 校验
     * @return 提交后的成员版本
     * @throws BizException 所有者、版本冲突或对象不可访问时拒绝
     */
    public String remove(AuthorizationContext actor, String memberId, VersionInput input,
                         long auditId, MemberMutationGuard guard) {
        if (input == null) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return transition(actor, memberId, input.expectedVersion(), MemberStatus.REMOVED, auditId, guard);
    }

    /**
     * 原子替换当前租户成员的部门关系，分别验证旧关系和新关系的权限范围。
     * @param actor 当前租户身份，平台身份拒绝
     * @param memberId 当前租户成员 ID
     * @param plan 完整任职目标，不得带跨租户引用
     * @param auditId 服务器发号得到的审计 ID
     * @param guard 实时操作权及受影响关系校验
     * @return 提交后的成员版本
     * @throws BizException 非租户身份、已移出成员、非法部门或版本冲突时拒绝
     */
    public String replaceDepartments(AuthorizationContext actor, String memberId, MemberDepartmentPlan plan,
                                     long auditId, MemberMutationGuard guard) {
        if (actor == null || actor.domain() != AuthorizationDomain.TENANT || plan == null) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        Objects.requireNonNull(guard, "提交必须提供实时授权校验");
        long tenantId = id(actor.tenantId()).longValueExact();
        long targetId = id(memberId).longValueExact();
        return transaction.execute(status -> {
            lockTenant(actor);
            identities.requireActive(actor);
            MemberLifecycleRepository.LockedMember before = lockMember(actor, targetId);
            Map<String, Boolean> oldRelations = members.lockDepartments(tenantId, targetId);
            Map<String, Boolean> nextRelations = new HashMap<>();
            for (var department : plan.departments()) {
                id(department.id());
                nextRelations.put(department.id(), department.primary());
            }
            Set<String> removed = changed(oldRelations, nextRelations);
            Set<String> added = changed(nextRelations, oldRelations);
            guard.require(actor, MemberMutationGuard.Operation.CHANGE_DEPARTMENTS, memberId,
                    Set.copyOf(removed), Set.copyOf(added));
            requireVersion(before, plan.expectedVersion());
            requirePresent(before);
            for (String departmentId : nextRelations.keySet()) {
                if (!members.lockDepartment(tenantId, id(departmentId).longValueExact())) {
                    throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                }
            }
            if (oldRelations.equals(nextRelations)) {
                return before.version().toString();
            }
            members.replaceDepartments(tenantId, targetId, nextRelations);
            members.incrementVersion(actor.domain(), tenantId, targetId, before.version());
            String version = nextVersion(before);
            audit(actor, memberId, auditId, AuditChangeType.UPDATE,
                    Map.of(AuditField.SCOPE, oldRelations), Map.of(AuditField.SCOPE, nextRelations), version);
            return version;
        });
    }

    private String transition(AuthorizationContext actor, String memberId, String expectedVersion,
                              MemberStatus next, long auditId, MemberMutationGuard guard) {
        Objects.requireNonNull(guard, "提交必须提供实时授权校验");
        long targetId = id(memberId).longValueExact();
        Long tenantId = actor.tenantId() == null ? null : id(actor.tenantId()).longValueExact();
        return transaction.execute(status -> {
            String owner = lockTenant(actor);
            identities.requireActive(actor);
            MemberLifecycleRepository.LockedMember before = lockMember(actor, targetId);
            Set<String> departments = actor.domain() == AuthorizationDomain.TENANT
                    ? Set.copyOf(members.lockDepartments(tenantId, targetId).keySet()) : Set.of();
            guard.require(actor, next == MemberStatus.REMOVED ? MemberMutationGuard.Operation.REMOVE
                    : MemberMutationGuard.Operation.CHANGE_STATUS, memberId, departments, Set.of());
            requireVersion(before, expectedVersion);
            requirePresent(before);
            if (Objects.equals(owner, memberId) && next != MemberStatus.ACTIVE) {
                throw new BizException(IamReasonCode.OBJECT_IN_USE);
            }
            if (before.status() == next) {
                return before.version().toString();
            }
            members.updateStatus(actor.domain(), tenantId, targetId, next, before.version());
            String version = nextVersion(before);
            audit(actor, memberId, auditId, next == MemberStatus.REMOVED ? AuditChangeType.REMOVE
                            : next == MemberStatus.ACTIVE ? AuditChangeType.ENABLE : AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, before.status()), Map.of(AuditField.STATUS, next), version);
            return version;
        });
    }

    private String lockTenant(AuthorizationContext actor) {
        if (actor.domain() == AuthorizationDomain.PLATFORM) {
            return null;
        }
        String owner = members.lockOwner(id(actor.tenantId()).longValueExact());
        if (owner == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        return owner;
    }

    private MemberLifecycleRepository.LockedMember lockMember(AuthorizationContext actor, long memberId) {
        Long tenantId = actor.tenantId() == null ? null : id(actor.tenantId()).longValueExact();
        MemberLifecycleRepository.LockedMember locked = members.lockMember(actor.domain(), tenantId, memberId);
        if (locked == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return locked;
    }

    private Set<String> changed(Map<String, Boolean> source, Map<String, Boolean> other) {
        Set<String> changed = new LinkedHashSet<>();
        source.forEach((departmentId, primary) -> {
            if (!Objects.equals(primary, other.get(departmentId))) {
                changed.add(departmentId);
            }
        });
        return changed;
    }

    private void requireVersion(MemberLifecycleRepository.LockedMember state, String version) {
        if (version == null || !version.matches("0|[1-9][0-9]{0,19}")) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (!state.version().toString().equals(version)) {
            throw new BizException(IamReasonCode.REVISION_CONFLICT);
        }
    }

    private void requirePresent(MemberLifecycleRepository.LockedMember state) {
        if (state.status() == MemberStatus.REMOVED) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private String nextVersion(MemberLifecycleRepository.LockedMember before) {
        return before.version().add(BigInteger.ONE).toString();
    }

    private BigInteger id(String id) {
        if (id == null || !id.matches("[1-9][0-9]{0,19}") || new BigInteger(id).compareTo(MAX_ID) > 0) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return new BigInteger(id);
    }

    private void audit(AuthorizationContext actor, String memberId, long auditId, AuditChangeType type,
                       Map<AuditField, ?> before, Map<AuditField, ?> after, String version) {
        audits.write(actor, auditId, MEMBER_RESOURCE, memberId, type, before, after, Map.of(MEMBER_VERSION, version));
    }
}
