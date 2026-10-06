package com.ingot.cloud.iam.extension;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.evaluation.DepartmentClosure;
import com.ingot.cloud.iam.evaluation.ScopeClause;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.extension.ScopeCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>范围传输保留AND/OR关系，按可信租户展开部门，不携带SQL。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class ScopeTransportCompiler {

    private final DepartmentClosure closure;

    private final IamMemberDepartmentMapper memberDepartments;

    /**
     * 编译完整条款。
     * @param actor 可信身份
     * @param clauses 求值条款
     * @return 可传输条款
     */
    public List<ScopeCondition> compile(AuthorizationContext actor, List<ScopeClause> clauses) {
        List<ScopeCondition> result = new ArrayList<>();
        Map<BigInteger, List<BigInteger>> tree = null;
        List<BigInteger> own = null;
        for (var clause : clauses) {
            if (clause.empty())
                continue;
            if (clause.all()) {
                result.add(new ScopeCondition(true, List.of(), null, List.of()));
                continue;
            }
            if (actor.domain() == AuthorizationDomain.PLATFORM
                    && (clause.memberDepartments() || !clause.departmentIds().isEmpty()))
                continue;
            List<List<String>> sets = new ArrayList<>();
            if (clause.memberDepartments() || !clause.departmentIds().isEmpty()) {
                BigInteger tenant = new BigInteger(actor.tenantId());
                if (tree == null)
                    tree = closure.loadChildren(tenant);
                if (clause.memberDepartments()) {
                    if (own == null)
                        own = memberDepartments
                            .selectList(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                                .eq(IamMemberDepartmentEntity::getTenantId, tenant)
                                .eq(IamMemberDepartmentEntity::getMemberId, new BigInteger(actor.memberId())))
                            .stream()
                            .map(IamMemberDepartmentEntity::getDepartmentId)
                            .toList();
                    sets.add(closure.expand(tree, own, clause.memberDepartmentDescendants())
                        .stream()
                        .map(BigInteger::toString)
                        .toList());
                }
                if (!clause.departmentIds().isEmpty())
                    sets.add(closure
                        .expand(tree, clause.departmentIds().stream().map(BigInteger::new).toList(),
                                clause.departmentDescendants())
                        .stream()
                        .map(BigInteger::toString)
                        .toList());
            }
            if (sets.stream().anyMatch(List::isEmpty))
                continue;
            result.add(new ScopeCondition(false, clause.objectIds(), clause.self() ? actor.memberId() : null, sets));
        }
        return List.copyOf(result);
    }

}
