package com.ingot.cloud.iam.evaluation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.organization.MemberMutationGuard;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberDepartmentView;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.ObjectCapability;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按同一授权视图批量计算对象操作能力，仅用于展示，提交时必须重新鉴权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ObjectCapabilities {
    private static final List<IamAction> PLATFORM_ACCOUNT = List.of(
            IamAction.PLATFORM_ACCOUNT_UPDATE, IamAction.PLATFORM_ACCOUNT_DELETE,
            IamAction.PLATFORM_ACCOUNT_ENABLE, IamAction.PLATFORM_ACCOUNT_DISABLE,
            IamAction.PLATFORM_ACCOUNT_LOCK, IamAction.PLATFORM_ACCOUNT_UNLOCK,
            IamAction.PLATFORM_ACCOUNT_RESET_PASSWORD);
    private static final List<IamAction> PLATFORM_MEMBER = List.of(
            IamAction.PLATFORM_MEMBER_UPDATE, IamAction.PLATFORM_MEMBER_STATUS, IamAction.PLATFORM_MEMBER_REMOVE);
    private static final List<IamAction> TENANT_MEMBER = List.of(
            IamAction.TENANT_MEMBER_UPDATE, IamAction.TENANT_MEMBER_STATUS, IamAction.TENANT_MEMBER_REMOVE,
            IamAction.TENANT_MEMBER_DEPARTMENTS);
    private static final List<IamAction> TENANT_DEPARTMENT = List.of(
            IamAction.TENANT_DEPARTMENT_UPDATE, IamAction.TENANT_DEPARTMENT_DELETE);
    private static final String ALLOWED_MESSAGE = "当前对象允许该操作";
    private final AuthorizationEvaluator evaluator;
    private final ResourceAccess scopes;

    /**
     * 读取当前身份授权视图，供同一列表请求复用。
     *
     * @param actor 当前身份
     * @return 能力快照
     */
    public Snapshot snapshot(AuthorizationContext actor) {
        return new Snapshot(actor, evaluator.evaluate(actor));
    }

    /**
     * 计算平台账号行上的展示能力，只消费已求值视图，不按写操作重新鉴权。
     *
     * @param snapshot 授权快照
     * @param accountId 目标账号
     * @return 按操作码索引的能力
     */
    public Map<String, ObjectCapability> platformAccount(Snapshot snapshot, String accountId) {
        Map<String, ObjectCapability> result = new LinkedHashMap<>();
        for (IamAction action : PLATFORM_ACCOUNT) {
            result.put(action.getCode(), of(snapshot.view(), action,
                    scopes.targetAllowed(snapshot.actor(), snapshot.view(), action.getCode(), accountId)));
        }
        return result;
    }

    /**
     * 计算平台成员行上的展示能力。
     *
     * @param snapshot 授权快照
     * @param memberId 目标成员
     * @return 按操作码索引的能力
     */
    public Map<String, ObjectCapability> platformMember(Snapshot snapshot, String memberId) {
        Map<String, ObjectCapability> result = new LinkedHashMap<>();
        for (IamAction action : PLATFORM_MEMBER) {
            result.put(action.getCode(), member(snapshot, action, memberId, Set.of(), operation(action)));
        }
        return result;
    }

    /**
     * 计算租户成员行上的展示能力。
     *
     * @param snapshot 授权快照
     * @param record 已读取的成员及其可见部门
     * @return 按操作码索引的能力
     */
    public Map<String, ObjectCapability> tenantMember(Snapshot snapshot, MemberRecord record) {
        Set<String> departments = departmentIds(record);
        Map<String, ObjectCapability> result = new LinkedHashMap<>();
        for (IamAction action : TENANT_MEMBER) {
            result.put(action.getCode(), member(snapshot, action, record.id(), departments, operation(action)));
        }
        return result;
    }

    /**
     * 计算部门行上的展示能力。
     *
     * @param snapshot 授权快照
     * @param departmentId 目标部门
     * @return 按操作码索引的能力
     */
    public Map<String, ObjectCapability> department(Snapshot snapshot, String departmentId) {
        Map<String, ObjectCapability> result = new LinkedHashMap<>();
        long id = Long.parseLong(departmentId);
        for (IamAction action : TENANT_DEPARTMENT) {
            result.put(action.getCode(), of(snapshot.view(), action,
                    scopes.departmentAllowed(snapshot.actor(), snapshot.view(), action, id)));
        }
        return result;
    }

    private ObjectCapability member(Snapshot snapshot, IamAction action, String memberId, Set<String> departments,
                                    MemberMutationGuard.Operation operation) {
        AuthorizationEvaluator.AuthorizationView view = snapshot.view();
        if (!view.actionCodes().contains(action.getCode())) {
            return denied(IamReasonCode.ACTION_DENIED);
        }
        boolean allowed = operation == null
                ? scopes.memberVisible(snapshot.actor(), view, action, Long.parseLong(memberId))
                : scopes.memberWriteAllowed(snapshot.actor(), view, action, memberId, operation, departments,
                departments);
        return of(view, action, allowed);
    }

    private static ObjectCapability of(AuthorizationEvaluator.AuthorizationView view, IamAction action,
                                       boolean objectAllowed) {
        if (!view.actionCodes().contains(action.getCode())) {
            return denied(IamReasonCode.ACTION_DENIED);
        }
        return objectAllowed ? new ObjectCapability(true, null, ALLOWED_MESSAGE)
                : denied(IamReasonCode.DATA_SCOPE_DENIED);
    }

    private static ObjectCapability denied(IamReasonCode reason) {
        return new ObjectCapability(false, reason, reason.getText());
    }

    private static MemberMutationGuard.Operation operation(IamAction action) {
        return switch (action) {
            case PLATFORM_MEMBER_STATUS, TENANT_MEMBER_STATUS -> MemberMutationGuard.Operation.CHANGE_STATUS;
            case PLATFORM_MEMBER_REMOVE, TENANT_MEMBER_REMOVE -> MemberMutationGuard.Operation.REMOVE;
            case TENANT_MEMBER_DEPARTMENTS -> MemberMutationGuard.Operation.CHANGE_DEPARTMENTS;
            default -> null;
        };
    }

    private static Set<String> departmentIds(MemberRecord record) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (record == null || record.departments() == null) {
            return ids;
        }
        for (MemberDepartmentView department : record.departments()) {
            ids.add(department.id());
        }
        return ids;
    }

    /**
     * <p>保存一次列表请求复用的身份与授权视图。</p>
     *
     * @param actor 当前身份
     * @param view 已求值授权
     * @author jy
     * @since 1.0.0
     */
    public record Snapshot(AuthorizationContext actor, AuthorizationEvaluator.AuthorizationView view) {
    }
}
