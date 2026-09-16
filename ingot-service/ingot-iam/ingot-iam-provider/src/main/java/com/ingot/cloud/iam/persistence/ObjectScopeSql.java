package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ObjectScopeClause;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;

/**
 * <p>把类型化对象范围落到 Wrapper 条件，不接受调用方传入的表列名或 SQL 片段。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ObjectScopeSql {
    private ObjectScopeSql() {
    }

    /**
     * 限制平台成员查询；全域不加条件，空范围恒假。
     *
     * @param wrapper 已绑定状态等基础条件的查询
     * @param scope 已编译范围
     */
    public static void restrictPlatformMembers(LambdaQueryWrapper<IamPlatformMemberEntity> wrapper, ObjectScope scope) {
        if (scope.coversAll()) {
            return;
        }
        if (scope.coversNone()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.and(or -> {
            boolean first = true;
            for (ObjectScopeClause clause : scope.clauses()) {
                if (first) {
                    applyPlatform(or, clause);
                    first = false;
                } else {
                    or.or(inner -> applyPlatform(inner, clause));
                }
            }
        });
    }

    /**
     * 限制租户成员查询，任职 EXISTS 使用可信 tenantId。
     *
     * @param wrapper 已绑定租户与状态的查询
     * @param scope 已编译范围
     * @param tenantId 已授权租户 ID
     */
    public static void restrictTenantMembers(LambdaQueryWrapper<IamTenantMemberEntity> wrapper, ObjectScope scope,
                                             BigInteger tenantId) {
        if (scope.coversAll()) {
            return;
        }
        if (scope.coversNone()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.and(or -> {
            boolean first = true;
            for (ObjectScopeClause clause : scope.clauses()) {
                if (first) {
                    applyTenant(or, clause, tenantId);
                    first = false;
                } else {
                    or.or(inner -> applyTenant(inner, clause, tenantId));
                }
            }
        });
    }

    /**
     * 限制账号查询。
     *
     * @param wrapper 已绑定未删除等基础条件的查询
     * @param scope 已编译范围
     */
    public static void restrictAccounts(LambdaQueryWrapper<IamAccountEntity> wrapper, ObjectScope scope) {
        if (scope.coversAll()) {
            return;
        }
        if (scope.coversNone()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.and(or -> {
            boolean first = true;
            for (ObjectScopeClause clause : scope.clauses()) {
                if (first) {
                    applyAccount(or, clause);
                    first = false;
                } else {
                    or.or(inner -> applyAccount(inner, clause));
                }
            }
        });
    }

    /**
     * 判断对象 ID 是否落入范围。
     *
     * @param scope 已编译范围
     * @param objectId 目标对象
     * @return 可见时为 true
     */
    public static boolean matches(ObjectScope scope, long objectId) {
        if (scope.coversAll()) {
            return true;
        }
        if (scope.coversNone()) {
            return false;
        }
        BigInteger id = BigInteger.valueOf(objectId);
        for (ObjectScopeClause clause : scope.clauses()) {
            if (clause.requiredIds().contains(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 限制部门查询。
     *
     * @param wrapper 已绑定租户的查询
     * @param scope 已编译范围
     */
    public static void restrictDepartments(LambdaQueryWrapper<IamDepartmentEntity> wrapper, ObjectScope scope) {
        if (scope.coversAll()) {
            return;
        }
        if (scope.coversNone()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.and(or -> {
            boolean first = true;
            for (ObjectScopeClause clause : scope.clauses()) {
                if (first) {
                    applyDepartment(or, clause);
                    first = false;
                } else {
                    or.or(inner -> applyDepartment(inner, clause));
                }
            }
        });
    }

    private static void applyPlatform(LambdaQueryWrapper<IamPlatformMemberEntity> wrapper, ObjectScopeClause clause) {
        if (clause.requiredIds().isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(IamPlatformMemberEntity::getId, clause.requiredIds());
    }

    private static void applyTenant(LambdaQueryWrapper<IamTenantMemberEntity> wrapper, ObjectScopeClause clause,
                                    BigInteger tenantId) {
        if (clause.requiredIds().isEmpty() && clause.departmentSets().isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        if (!clause.requiredIds().isEmpty()) {
            wrapper.in(IamTenantMemberEntity::getId, clause.requiredIds());
        }
        for (Set<BigInteger> departments : clause.departmentSets()) {
            existsMembership(wrapper, tenantId, departments);
        }
    }

    private static void applyDepartment(LambdaQueryWrapper<IamDepartmentEntity> wrapper, ObjectScopeClause clause) {
        if (clause.requiredIds().isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(IamDepartmentEntity::getId, clause.requiredIds());
    }

    private static void applyAccount(LambdaQueryWrapper<IamAccountEntity> wrapper, ObjectScopeClause clause) {
        if (clause.requiredIds().isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(IamAccountEntity::getId, clause.requiredIds());
    }

    private static void existsMembership(LambdaQueryWrapper<IamTenantMemberEntity> wrapper, BigInteger tenantId,
                                         Set<BigInteger> departments) {
        if (departments.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        List<BigInteger> ids = List.copyOf(departments);
        StringBuilder sql = new StringBuilder(
                "SELECT 1 FROM iam_member_department md WHERE md.tenant_id = {0} AND md.member_id = iam_tenant_member.id AND md.department_id IN (");
        Object[] args = new Object[ids.size() + 1];
        args[0] = tenantId;
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append('{').append(i + 1).append('}');
            args[i + 1] = ids.get(i);
        }
        sql.append(')');
        wrapper.exists(sql.toString(), args);
    }
}
