package com.ingot.cloud.iam.evaluation;

import java.util.LinkedHashSet;
import java.util.Set;

import com.ingot.cloud.iam.organization.MemberMutationGuard;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.persistence.ObjectScopeSql;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按已求值范围执行对象可见性与写两端覆盖检查，缺操作拒绝，有操作无对象则读空写拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ResourceAccess {
    private final AuthorizationEvaluator evaluator;
    private final ObjectScopeCompiler compiler;
    private final MemberQueryRepository members;
    private final DepartmentQueryRepository departments;

    /**
     * 编译当前身份对成员列表/详情的范围。
     *
     * @param actor 当前身份
     * @param action 成员读操作
     * @return 类型化范围
     */
    public ObjectScope memberRead(AuthorizationContext actor, IamAction action) {
        evaluator.require(actor, action);
        return compiler.members(actor, evaluator.evaluate(actor).scope(action.getCode()));
    }

    /**
     * 编译当前身份对部门列表/详情的范围。
     *
     * @param actor 当前身份
     * @param action 部门读操作
     * @return 类型化范围
     */
    public ObjectScope departmentRead(AuthorizationContext actor, IamAction action) {
        evaluator.require(actor, action);
        return compiler.departments(actor, evaluator.evaluate(actor).scope(action.getCode()));
    }

    /**
     * 确认目标成员对读操作可见；详情不可见时按对象不存在处理。
     *
     * @param actor 当前身份
     * @param action 读操作
     * @param memberId 目标成员
     */
    public void requireVisibleMember(AuthorizationContext actor, IamAction action, long memberId) {
        if (!members.visible(actor.domain(), tenantId(actor), memberId, memberRead(actor, action))) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    /**
     * 确认目标部门对读操作可见。
     *
     * @param actor 当前身份
     * @param action 读操作
     * @param departmentId 目标部门
     */
    public void requireVisibleDepartment(AuthorizationContext actor, IamAction action, long departmentId) {
        if (!departments.matches(tenantId(actor), departmentId, departmentRead(actor, action))) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    /**
     * 成员写路径覆盖检查：状态/移出覆盖全部部门，调部门只检查受影响关系。
     *
     * @param actor 当前身份
     * @param action 写操作
     * @param memberId 目标成员
     * @param operation 成员变更种类
     * @param oldDepartments 旧部门
     * @param newDepartments 新部门
     */
    public void requireMemberWrite(AuthorizationContext actor, IamAction action, String memberId,
                                   MemberMutationGuard.Operation operation, Set<String> oldDepartments,
                                   Set<String> newDepartments) {
        evaluator.require(actor, action);
        ResolvedActionScope scope = evaluator.evaluate(actor).scope(action.getCode());
        if (scope.isEmpty()) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        Set<String> required = operation == MemberMutationGuard.Operation.CHANGE_DEPARTMENTS
                ? union(oldDepartments, newDepartments) : oldDepartments;
        if (!covers(actor, scope, memberId, required, operation)) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
    }

    /**
     * 创建成员时检查目标任职部门；无部门且非全域时拒绝。
     *
     * @param actor 当前身份
     * @param action 创建操作
     * @param departmentIds 新成员任职
     */
    public void requireCreate(AuthorizationContext actor, IamAction action, Set<String> departmentIds) {
        evaluator.require(actor, action);
        ResolvedActionScope scope = evaluator.evaluate(actor).scope(action.getCode());
        if (scope.isEmpty()) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        if (scope.clauses().stream().anyMatch(ScopeClause::all)) {
            return;
        }
        if (departmentIds == null || departmentIds.isEmpty()) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        ObjectScope departments = compiler.departments(actor, scope);
        for (String departmentId : departmentIds) {
            if (!this.departments.matches(tenantId(actor), Long.parseLong(departmentId), departments)) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
        }
    }

    /**
     * 部门写路径要求目标部门落入范围。
     *
     * @param actor 当前身份
     * @param action 写操作
     * @param departmentId 目标部门
     */
    public void requireDepartmentWrite(AuthorizationContext actor, IamAction action, long departmentId) {
        evaluator.require(actor, action);
        ResolvedActionScope scope = evaluator.evaluate(actor).scope(action.getCode());
        if (scope.isEmpty() || !departments.matches(tenantId(actor), departmentId,
                compiler.departments(actor, scope))) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
    }

    /**
     * 编译当前身份对全局对象的范围。
     *
     * @param actor 当前身份
     * @param action 精确操作
     * @return 类型化范围
     */
    public ObjectScope objects(AuthorizationContext actor, IamAction action) {
        evaluator.require(actor, action);
        return compiler.objects(actor, evaluator.evaluate(actor).scope(action.getCode()));
    }

    /**
     * 确认目标对象对操作可见；不可见时按对象不存在处理。
     *
     * @param actor 当前身份
     * @param action 精确操作
     * @param objectId 目标对象
     */
    public void requireVisibleObject(AuthorizationContext actor, IamAction action, long objectId) {
        if (!ObjectScopeSql.matches(objects(actor, action), objectId)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    /**
     * 判断已求值视图是否包含该操作且目标成员落入读范围。
     *
     * @param actor 当前身份
     * @param view 同一请求复用的授权视图
     * @param action 精确操作
     * @param memberId 目标成员
     * @return 有操作且对象可见时为 true
     */
    public boolean memberVisible(AuthorizationContext actor, AuthorizationEvaluator.AuthorizationView view,
                                 IamAction action, long memberId) {
        if (!hasAction(view, action)) {
            return false;
        }
        return members.visible(actor.domain(), tenantId(actor), memberId,
                compiler.members(actor, view.scope(action.getCode())));
    }

    /**
     * 判断已求值视图是否包含该操作且目标部门落入写范围。
     *
     * @param actor 当前身份
     * @param view 同一请求复用的授权视图
     * @param action 精确操作
     * @param departmentId 目标部门
     * @return 有操作且对象可见时为 true
     */
    public boolean departmentAllowed(AuthorizationContext actor, AuthorizationEvaluator.AuthorizationView view,
                                     IamAction action, long departmentId) {
        if (!hasAction(view, action)) {
            return false;
        }
        ResolvedActionScope scope = view.scope(action.getCode());
        return !scope.isEmpty() && departments.matches(tenantId(actor), departmentId,
                compiler.departments(actor, scope));
    }

    /**
     * 判断已求值视图是否包含该写操作且覆盖目标成员任职。
     *
     * @param actor 当前身份
     * @param view 同一请求复用的授权视图
     * @param action 精确操作
     * @param memberId 目标成员
     * @param operation 成员变更种类
     * @param oldDepartments 旧部门
     * @param newDepartments 新部门
     * @return 有操作且范围覆盖时为 true
     */
    public boolean memberWriteAllowed(AuthorizationContext actor, AuthorizationEvaluator.AuthorizationView view,
                                      IamAction action, String memberId, MemberMutationGuard.Operation operation,
                                      Set<String> oldDepartments, Set<String> newDepartments) {
        if (!hasAction(view, action)) {
            return false;
        }
        ResolvedActionScope scope = view.scope(action.getCode());
        if (scope.isEmpty()) {
            return false;
        }
        Set<String> required = operation == MemberMutationGuard.Operation.CHANGE_DEPARTMENTS
                ? union(oldDepartments, newDepartments) : oldDepartments;
        return covers(actor, scope, memberId, required, operation);
    }

    /**
     * 判断已求值视图是否允许对可选目标执行该操作。
     *
     * @param actor 当前身份
     * @param view 同一请求复用的授权视图
     * @param actionCode 精确操作码
     * @param targetId 可选目标对象
     * @return 无目标时有操作即允许；有目标时还需落入对象范围
     */
    public boolean targetAllowed(AuthorizationContext actor, AuthorizationEvaluator.AuthorizationView view,
                                 String actionCode, String targetId) {
        if (view == null || actionCode == null || !view.actionCodes().contains(actionCode)) {
            return false;
        }
        if (targetId == null || targetId.isBlank()) {
            return true;
        }
        String resource = resourceOf(actionCode);
        if ("member".equals(resource)) {
            return members.visible(actor.domain(), tenantId(actor), IamIds.require(targetId),
                    compiler.members(actor, view.scope(actionCode)));
        }
        if ("department".equals(resource)) {
            return departments.matches(tenantId(actor), IamIds.require(targetId),
                    compiler.departments(actor, view.scope(actionCode)));
        }
        ResolvedActionScope scope = view.scope(actionCode);
        if (scope.isEmpty()) {
            return false;
        }
        for (ScopeClause clause : scope.clauses()) {
            if (clause.all() || clause.objectIds().contains(targetId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAction(AuthorizationEvaluator.AuthorizationView view, IamAction action) {
        return view != null && action != null && view.actionCodes().contains(action.getCode());
    }

    private static String resourceOf(String actionCode) {
        int first = actionCode.indexOf(':');
        int last = actionCode.lastIndexOf(':');
        if (first < 0 || last <= first) {
            return "";
        }
        return actionCode.substring(first + 1, last);
    }

    private boolean covers(AuthorizationContext actor, ResolvedActionScope scope, String memberId,
                           Set<String> departments, MemberMutationGuard.Operation operation) {
        ObjectScope members = compiler.members(actor, scope);
        ObjectScope departmentScope = compiler.departments(actor, scope);
        long id = Long.parseLong(memberId);
        if (this.members.visible(actor.domain(), tenantId(actor), id, members)
                && (departments == null || departments.isEmpty())) {
            return true;
        }
        if (departments == null || departments.isEmpty()) {
            return this.members.visible(actor.domain(), tenantId(actor), id, members);
        }
        boolean departmentCovered = departments.stream()
                .allMatch(item -> this.departments.matches(tenantId(actor), Long.parseLong(item), departmentScope));
        if (operation == MemberMutationGuard.Operation.CHANGE_DEPARTMENTS) {
            return departmentCovered;
        }
        return departmentCovered && this.members.visible(actor.domain(), tenantId(actor), id, members);
    }

    private static Long tenantId(AuthorizationContext actor) {
        return actor.tenantId() == null ? null : Long.parseLong(actor.tenantId());
    }

    private static Set<String> union(Set<String> left, Set<String> right) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (left != null) {
            values.addAll(left);
        }
        if (right != null) {
            values.addAll(right);
        }
        return values;
    }
}
