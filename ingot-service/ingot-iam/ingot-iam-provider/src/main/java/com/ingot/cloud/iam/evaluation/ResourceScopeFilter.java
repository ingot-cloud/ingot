package com.ingot.cloud.iam.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * <p>把已求值范围编译为数据库谓词，查询与计数使用同一 EXISTS/IN 条件。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ResourceScopeFilter {
    private ResourceScopeFilter() {
    }

    /**
     * <p>范围谓词及其命名参数。</p>
     *
     * @param sql 可直接拼入 WHERE 的括号表达式
     * @param parameters 命名参数
     * @author jy
     * @since 1.0.0
     */
    public record Predicate(String sql, Map<String, Object> parameters) {
    }

    /**
     * 编译成员对象范围。空范围得到恒假条件。
     *
     * @param jdbc 用于展开部门下级
     * @param actor 当前身份
     * @param scope 已求值范围
     * @param memberIdSql 目标成员 ID 列
     * @return SQL 谓词
     */
    public static Predicate members(NamedParameterJdbcTemplate jdbc, AuthorizationContext actor,
                                    ResolvedActionScope scope, String memberIdSql) {
        return compile(jdbc, actor, scope, memberIdSql, true);
    }

    /**
     * 编译部门对象范围。空范围得到恒假条件。
     *
     * @param jdbc 用于展开部门下级
     * @param actor 当前身份
     * @param scope 已求值范围
     * @param departmentIdSql 目标部门 ID 列
     * @return SQL 谓词
     */
    public static Predicate departments(NamedParameterJdbcTemplate jdbc, AuthorizationContext actor,
                                        ResolvedActionScope scope, String departmentIdSql) {
        return compile(jdbc, actor, scope, departmentIdSql, false);
    }

    private static Predicate compile(NamedParameterJdbcTemplate jdbc, AuthorizationContext actor,
                                     ResolvedActionScope scope, String idSql, boolean memberTarget) {
        Map<String, Object> parameters = new HashMap<>();
        if (scope == null || scope.isEmpty()) {
            return new Predicate("(1=0)", parameters);
        }
        List<String> parts = new ArrayList<>();
        int index = 0;
        for (ScopeClause clause : scope.clauses()) {
            if (clause.empty()) {
                continue;
            }
            String fragment = memberTarget
                    ? memberClause(jdbc, actor, clause, idSql, index, parameters)
                    : departmentClause(jdbc, actor, clause, idSql, index, parameters);
            if (fragment != null) {
                parts.add(fragment);
                index++;
            }
        }
        if (parts.isEmpty()) {
            return new Predicate("(1=0)", parameters);
        }
        return new Predicate("(" + String.join(" OR ", parts) + ")", parameters);
    }

    private static String memberClause(NamedParameterJdbcTemplate jdbc, AuthorizationContext actor, ScopeClause clause,
                                       String memberIdSql, int index, Map<String, Object> parameters) {
        List<String> constraints = new ArrayList<>();
        if (clause.all()) {
            return "(1=1)";
        }
        if (clause.self()) {
            String key = "scopeSelf" + index;
            parameters.put(key, Long.parseLong(actor.memberId()));
            constraints.add(memberIdSql + "=:" + key);
        }
        if (!clause.objectIds().isEmpty()) {
            String key = "scopeObjects" + index;
            parameters.put(key, longs(clause.objectIds()));
            constraints.add(memberIdSql + " IN (:" + key + ")");
        }
        if (actor.domain() == AuthorizationDomain.TENANT) {
            long tenantId = Long.parseLong(actor.tenantId());
            if (clause.memberDepartments()) {
                Set<Long> ids = actorDepartments(jdbc, tenantId, Long.parseLong(actor.memberId()));
                ids = DepartmentClosure.expand(jdbc, tenantId, ids, clause.memberDepartmentDescendants());
                constraints.add(departmentExists(memberIdSql, tenantId, ids, "scopeMemberDept" + index, parameters));
            }
            if (!clause.departmentIds().isEmpty()) {
                Set<Long> ids = DepartmentClosure.expand(jdbc, tenantId, longs(clause.departmentIds()),
                        clause.departmentDescendants());
                constraints.add(departmentExists(memberIdSql, tenantId, ids, "scopeManagedDept" + index, parameters));
            }
        } else if (clause.memberDepartments() || !clause.departmentIds().isEmpty()) {
            return null;
        }
        return constraints.isEmpty() ? null : "(" + String.join(" AND ", constraints) + ")";
    }

    private static String departmentClause(NamedParameterJdbcTemplate jdbc, AuthorizationContext actor,
                                           ScopeClause clause, String departmentIdSql, int index,
                                           Map<String, Object> parameters) {
        if (actor.domain() != AuthorizationDomain.TENANT) {
            return clause.all() ? "(1=1)" : null;
        }
        List<String> constraints = new ArrayList<>();
        if (clause.all()) {
            return "(1=1)";
        }
        if (clause.self()) {
            return null;
        }
        if (!clause.objectIds().isEmpty()) {
            String key = "scopeDeptObjects" + index;
            parameters.put(key, longs(clause.objectIds()));
            constraints.add(departmentIdSql + " IN (:" + key + ")");
        }
        long tenantId = Long.parseLong(actor.tenantId());
        if (clause.memberDepartments()) {
            Set<Long> ids = actorDepartments(jdbc, tenantId, Long.parseLong(actor.memberId()));
            ids = DepartmentClosure.expand(jdbc, tenantId, ids, clause.memberDepartmentDescendants());
            constraints.add(inDepartments(departmentIdSql, ids, "scopeActorDept" + index, parameters));
        }
        if (!clause.departmentIds().isEmpty()) {
            Set<Long> ids = DepartmentClosure.expand(jdbc, tenantId, longs(clause.departmentIds()),
                    clause.departmentDescendants());
            constraints.add(inDepartments(departmentIdSql, ids, "scopeBoundDept" + index, parameters));
        }
        return constraints.isEmpty() ? null : "(" + String.join(" AND ", constraints) + ")";
    }

    private static String departmentExists(String memberIdSql, long tenantId, Set<Long> departmentIds, String key,
                                           Map<String, Object> parameters) {
        if (departmentIds.isEmpty()) {
            return "(1=0)";
        }
        parameters.put(key + "Tenant", tenantId);
        parameters.put(key, departmentIds);
        return "EXISTS (SELECT 1 FROM iam_member_department md_" + key
                + " WHERE md_" + key + ".tenant_id=:" + key + "Tenant AND md_" + key + ".member_id=" + memberIdSql
                + " AND md_" + key + ".department_id IN (:" + key + "))";
    }

    private static String inDepartments(String departmentIdSql, Set<Long> departmentIds, String key,
                                        Map<String, Object> parameters) {
        if (departmentIds.isEmpty()) {
            return "(1=0)";
        }
        parameters.put(key, departmentIds);
        return departmentIdSql + " IN (:" + key + ")";
    }

    private static Set<Long> actorDepartments(NamedParameterJdbcTemplate jdbc, long tenantId, long memberId) {
        return new LinkedHashSet<>(jdbc.queryForList("""
                SELECT department_id FROM iam_member_department
                 WHERE tenant_id=:tenantId AND member_id=:memberId
                """, Map.of("tenantId", tenantId, "memberId", memberId), Long.class));
    }

    private static Set<Long> longs(List<String> ids) {
        Set<Long> values = new LinkedHashSet<>();
        for (String id : ids) {
            values.add(Long.parseLong(id));
        }
        return values;
    }
}
