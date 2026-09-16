package com.ingot.cloud.iam.evaluation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>把已求值范围编译为类型化对象范围，同一编译结果供列表、计数和可见性检查复用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class ObjectScopeCompiler {
    private final DepartmentClosure closures;
    private final IamMemberDepartmentMapper memberDepartments;

    /**
     * 编译成员对象范围。空范围得到恒假条件。
     *
     * @param actor 当前身份
     * @param scope 已求值范围
     * @return 类型化成员范围
     */
    public ObjectScope members(AuthorizationContext actor, ResolvedActionScope scope) {
        return compile(actor, scope, true);
    }

    /**
     * 编译部门对象范围。空范围得到恒假条件。
     *
     * @param actor 当前身份
     * @param scope 已求值范围
     * @return 类型化部门范围
     */
    public ObjectScope departments(AuthorizationContext actor, ResolvedActionScope scope) {
        return compile(actor, scope, false);
    }

    private ObjectScope compile(AuthorizationContext actor, ResolvedActionScope scope, boolean memberTarget) {
        if (scope == null || scope.isEmpty()) {
            return ObjectScope.none();
        }
        CompileContext context = new CompileContext(actor);
        List<ObjectScopeClause> clauses = new ArrayList<>();
        for (ScopeClause clause : scope.clauses()) {
            if (clause.empty()) {
                continue;
            }
            ObjectScopeClause compiled = memberTarget ? memberClause(context, clause) : departmentClause(context, clause);
            if (compiled == ALL) {
                return ObjectScope.all();
            }
            if (compiled != null) {
                clauses.add(compiled);
            }
        }
        return ObjectScope.of(clauses);
    }

    /**
     * 编译全局对象范围，只接受 ALL 与 OBJECT_SET。
     *
     * @param actor 当前身份
     * @param scope 已求值范围
     * @return 类型化对象范围
     */
    public ObjectScope objects(AuthorizationContext actor, ResolvedActionScope scope) {
        if (scope == null || scope.isEmpty()) {
            return ObjectScope.none();
        }
        List<ObjectScopeClause> clauses = new ArrayList<>();
        for (ScopeClause clause : scope.clauses()) {
            if (clause.empty()) {
                continue;
            }
            if (clause.all()) {
                return ObjectScope.all();
            }
            if (!clause.objectIds().isEmpty()) {
                clauses.add(new ObjectScopeClause(ids(clause.objectIds()), List.of()));
            }
        }
        return ObjectScope.of(clauses);
    }

    private ObjectScopeClause memberClause(CompileContext context, ScopeClause clause) {
        if (clause.all()) {
            return ALL;
        }
        Set<BigInteger> requiredIds = null;
        if (clause.self()) {
            requiredIds = intersect(requiredIds, Set.of(context.memberId()));
            if (requiredIds.isEmpty()) {
                return null;
            }
        }
        if (!clause.objectIds().isEmpty()) {
            requiredIds = intersect(requiredIds, ids(clause.objectIds()));
            if (requiredIds.isEmpty()) {
                return null;
            }
        }
        List<Set<BigInteger>> departmentSets = new ArrayList<>();
        if (context.actor.domain() == AuthorizationDomain.TENANT) {
            if (clause.memberDepartments()) {
                Set<BigInteger> departments = context.expand(context.actorDepartments(),
                        clause.memberDepartmentDescendants());
                if (departments.isEmpty()) {
                    return null;
                }
                departmentSets.add(departments);
            }
            if (!clause.departmentIds().isEmpty()) {
                Set<BigInteger> departments = context.expand(ids(clause.departmentIds()),
                        clause.departmentDescendants());
                if (departments.isEmpty()) {
                    return null;
                }
                departmentSets.add(departments);
            }
        } else if (clause.memberDepartments() || !clause.departmentIds().isEmpty()) {
            return null;
        }
        if ((requiredIds == null || requiredIds.isEmpty()) && departmentSets.isEmpty()) {
            return null;
        }
        return new ObjectScopeClause(requiredIds == null ? Set.of() : requiredIds, departmentSets);
    }

    private ObjectScopeClause departmentClause(CompileContext context, ScopeClause clause) {
        if (context.actor.domain() != AuthorizationDomain.TENANT) {
            return clause.all() ? ALL : null;
        }
        if (clause.all()) {
            return ALL;
        }
        if (clause.self()) {
            return null;
        }
        Set<BigInteger> requiredIds = null;
        if (!clause.objectIds().isEmpty()) {
            requiredIds = intersect(requiredIds, ids(clause.objectIds()));
            if (requiredIds.isEmpty()) {
                return null;
            }
        }
        if (clause.memberDepartments()) {
            requiredIds = intersect(requiredIds, context.expand(context.actorDepartments(),
                    clause.memberDepartmentDescendants()));
            if (requiredIds == null || requiredIds.isEmpty()) {
                return null;
            }
        }
        if (!clause.departmentIds().isEmpty()) {
            requiredIds = intersect(requiredIds, context.expand(ids(clause.departmentIds()),
                    clause.departmentDescendants()));
            if (requiredIds == null || requiredIds.isEmpty()) {
                return null;
            }
        }
        if (requiredIds == null || requiredIds.isEmpty()) {
            return null;
        }
        return new ObjectScopeClause(requiredIds, List.of());
    }

    private static Set<BigInteger> ids(List<String> values) {
        Set<BigInteger> result = new LinkedHashSet<>();
        for (String value : values) {
            result.add(new BigInteger(value));
        }
        return result;
    }

    private static Set<BigInteger> intersect(Set<BigInteger> current, Set<BigInteger> next) {
        if (current == null) {
            return new LinkedHashSet<>(next);
        }
        current.retainAll(next);
        return current;
    }

    private static final ObjectScopeClause ALL = new ObjectScopeClause(Set.of(), List.of());

    private final class CompileContext {
        private final AuthorizationContext actor;
        private Map<BigInteger, List<BigInteger>> children;
        private Set<BigInteger> actorDepartments;

        private CompileContext(AuthorizationContext actor) {
            this.actor = actor;
        }

        private BigInteger memberId() {
            return new BigInteger(actor.memberId());
        }

        private BigInteger tenantId() {
            return new BigInteger(actor.tenantId());
        }

        private Set<BigInteger> actorDepartments() {
            if (actorDepartments == null) {
                actorDepartments = new LinkedHashSet<>();
                for (IamMemberDepartmentEntity row : memberDepartments.selectList(
                        Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                                .select(IamMemberDepartmentEntity::getDepartmentId)
                                .eq(IamMemberDepartmentEntity::getTenantId, tenantId())
                                .eq(IamMemberDepartmentEntity::getMemberId, memberId()))) {
                    actorDepartments.add(row.getDepartmentId());
                }
            }
            return actorDepartments;
        }

        private Set<BigInteger> expand(Set<BigInteger> roots, boolean includeDescendants) {
            if (!includeDescendants) {
                return new LinkedHashSet<>(roots);
            }
            if (children == null) {
                children = closures.loadChildren(tenantId());
            }
            return closures.expand(children, roots, true);
        }
    }
}
