package com.ingot.framework.authorization;

import java.util.List;
import com.ingot.framework.commons.model.iam.extension.ScopeCondition;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;

/**
 * <p>目标匹配保持条款内AND、条款间OR，不把委派交集展开为并集。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ScopeRules {

    private ScopeRules() {
    }

    /**
     * 匹配真实业务归属。
     * @param clauses 范围并集
     * @param target 实际目标
     * @return 是否匹配
     */
    public static boolean matches(List<ScopeCondition> clauses, ScopeTarget target) {
        if (target == null || clauses == null)
            return false;
        return clauses.stream()
            .anyMatch(c -> c.all()
                    || (!c.objectIds().isEmpty() || c.ownerMemberId() != null || !c.departmentSets().isEmpty())
                            && (c.objectIds().isEmpty() || c.objectIds().contains(target.objectId()))
                            && (c.ownerMemberId() == null || c.ownerMemberId().equals(target.ownerMemberId()))
                            && c.departmentSets()
                                .stream()
                                .allMatch(set -> set.stream().anyMatch(target.departmentIds()::contains)));
    }

}
