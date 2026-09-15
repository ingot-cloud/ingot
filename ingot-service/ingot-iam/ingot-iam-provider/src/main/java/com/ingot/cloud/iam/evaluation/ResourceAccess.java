package com.ingot.cloud.iam.evaluation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import com.ingot.cloud.iam.organization.MemberMutationGuard;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>按已求值范围执行对象可见性与写两端覆盖检查，缺操作拒绝，有操作无对象则读空写拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class ResourceAccess {
    private final JdbcAuthorizationEvaluator evaluator;
    private final NamedParameterJdbcTemplate jdbc;

    /**
     * 绑定求值器与 IAM 目标库。
     *
     * @param evaluator 授权视图
     * @param dataSource 独立 IAM 数据源
     */
    public ResourceAccess(JdbcAuthorizationEvaluator evaluator, DataSource dataSource) {
        this.evaluator = evaluator;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * 编译当前身份对成员列表/详情的范围谓词。
     *
     * @param actor 当前身份
     * @param action 成员读操作
     * @param memberIdSql 成员 ID 列
     * @return SQL 谓词
     */
    public ResourceScopeFilter.Predicate memberRead(AuthorizationContext actor, IamAction action, String memberIdSql) {
        evaluator.require(actor, action);
        return ResourceScopeFilter.members(jdbc, actor, evaluator.evaluate(actor).scope(action.getCode()), memberIdSql);
    }

    /**
     * 编译当前身份对部门列表/详情的范围谓词。
     *
     * @param actor 当前身份
     * @param action 部门读操作
     * @param departmentIdSql 部门 ID 列
     * @return SQL 谓词
     */
    public ResourceScopeFilter.Predicate departmentRead(AuthorizationContext actor, IamAction action,
                                                        String departmentIdSql) {
        evaluator.require(actor, action);
        return ResourceScopeFilter.departments(jdbc, actor, evaluator.evaluate(actor).scope(action.getCode()),
                departmentIdSql);
    }

    /**
     * 确认目标成员对写操作可见；详情不可见时按对象不存在处理。
     *
     * @param actor 当前身份
     * @param action 读操作
     * @param memberId 目标成员
     */
    public void requireVisibleMember(AuthorizationContext actor, IamAction action, long memberId) {
        ResourceScopeFilter.Predicate predicate = memberRead(actor, action, ":targetMemberId");
        String sql = actor.domain() == AuthorizationDomain.PLATFORM
                ? "SELECT COUNT(*) FROM iam_platform_member WHERE id=:targetMemberId AND " + predicate.sql()
                : "SELECT COUNT(*) FROM iam_tenant_member WHERE id=:targetMemberId AND tenant_id=:tenantId AND "
                + predicate.sql();
        if (count(sql, actor, predicate, memberId) < 1) {
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
        ResourceScopeFilter.Predicate predicate = departmentRead(actor, action, ":targetDepartmentId");
        Map<String, Object> parameters = new HashMap<>(predicate.parameters());
        parameters.put("targetDepartmentId", departmentId);
        if (actor.tenantId() != null) {
            parameters.put("tenantId", Long.parseLong(actor.tenantId()));
        }
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_department WHERE id=:targetDepartmentId AND tenant_id=:tenantId AND "
                        + predicate.sql(), parameters, Long.class);
        if (count == null || count < 1) {
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
        for (String departmentId : departmentIds) {
            if (!matchesDepartment(actor, scope, Long.parseLong(departmentId))) {
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
        if (scope.isEmpty()) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        ResourceScopeFilter.Predicate predicate = ResourceScopeFilter.departments(jdbc, actor, scope, ":targetDepartmentId");
        Map<String, Object> parameters = new HashMap<>(predicate.parameters());
        parameters.put("targetDepartmentId", departmentId);
        if (actor.tenantId() != null) {
            parameters.put("tenantId", Long.parseLong(actor.tenantId()));
        }
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM iam_department WHERE id=:targetDepartmentId AND "
                + (actor.tenantId() == null ? "1=1" : "tenant_id=:tenantId") + " AND " + predicate.sql(),
                parameters, Long.class);
        if (count == null || count < 1) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
    }

    private boolean covers(AuthorizationContext actor, ResolvedActionScope scope, String memberId,
                           Set<String> departments, MemberMutationGuard.Operation operation) {
        if (matchesMember(actor, scope, Long.parseLong(memberId)) && (departments == null || departments.isEmpty())) {
            return true;
        }
        if (departments == null || departments.isEmpty()) {
            return matchesMember(actor, scope, Long.parseLong(memberId));
        }
        if (operation == MemberMutationGuard.Operation.CHANGE_DEPARTMENTS) {
            return departments.stream().allMatch(id -> matchesDepartment(actor, scope, Long.parseLong(id)));
        }
        return departments.stream().allMatch(id -> matchesDepartment(actor, scope, Long.parseLong(id)))
                && matchesMember(actor, scope, Long.parseLong(memberId));
    }

    private boolean matchesMember(AuthorizationContext actor, ResolvedActionScope scope, long memberId) {
        ResourceScopeFilter.Predicate predicate = ResourceScopeFilter.members(jdbc, actor, scope, ":targetMemberId");
        String sql = actor.domain() == AuthorizationDomain.PLATFORM
                ? "SELECT COUNT(*) FROM iam_platform_member WHERE id=:targetMemberId AND " + predicate.sql()
                : "SELECT COUNT(*) FROM iam_tenant_member WHERE id=:targetMemberId AND tenant_id=:tenantId AND "
                + predicate.sql();
        return count(sql, actor, predicate, memberId) > 0;
    }

    private boolean matchesDepartment(AuthorizationContext actor, ResolvedActionScope scope, long departmentId) {
        ResourceScopeFilter.Predicate predicate = ResourceScopeFilter.departments(jdbc, actor, scope,
                ":targetDepartmentId");
        Map<String, Object> parameters = new HashMap<>(predicate.parameters());
        parameters.put("targetDepartmentId", departmentId);
        if (actor.tenantId() != null) {
            parameters.put("tenantId", Long.parseLong(actor.tenantId()));
        }
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM iam_department WHERE id=:targetDepartmentId AND "
                + (actor.tenantId() == null ? "1=1" : "tenant_id=:tenantId") + " AND " + predicate.sql(),
                parameters, Long.class);
        return count != null && count > 0;
    }

    private long count(String sql, AuthorizationContext actor, ResourceScopeFilter.Predicate predicate, long memberId) {
        Map<String, Object> parameters = new HashMap<>(predicate.parameters());
        parameters.put("targetMemberId", memberId);
        if (actor.tenantId() != null) {
            parameters.put("tenantId", Long.parseLong(actor.tenantId()));
        } else if (sql.contains(":tenantId")) {
            return 0;
        }
        Long count = jdbc.queryForObject(sql, parameters, Long.class);
        return count == null ? 0 : count;
    }

    private static Set<String> union(Set<String> left, Set<String> right) {
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        if (left != null) {
            values.addAll(left);
        }
        if (right != null) {
            values.addAll(right);
        }
        return values;
    }
}
