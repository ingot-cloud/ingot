package com.ingot.cloud.iam.policy;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ingot.cloud.iam.evaluation.DepartmentClosure;
import com.ingot.cloud.iam.persistence.entity.IamDefaultPolicyRevisionEntity;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.framework.commons.model.iam.DirectoryDefaultScope;
import com.ingot.framework.commons.model.iam.DirectoryPolicyDraft;
import com.ingot.framework.commons.model.iam.DirectoryRule;
import com.ingot.framework.commons.model.iam.PolicyEffect;
import com.ingot.framework.commons.model.iam.Selection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按固定默认版本、匹配允许并集替代默认、扣除全部禁止并恢复本人的顺序求值通讯录可见范围。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class DirectoryVisibilityEvaluator {
    private final PolicyWriteRepository policies;
    private final DepartmentClosure closures;

    /**
     * 计算查看者在当前草稿下的可见成员谓词，不依赖规则书写顺序。
     *
     * @param tenantId 已授权租户
     * @param viewerId 查看者成员
     * @param policy 通讯录草稿；默认覆盖为空时读取固定版本定义
     * @return 可交给 SQL 过滤的谓词
     */
    public DirectoryVisibility evaluate(long tenantId, long viewerId, DirectoryPolicyDraft policy) {
        Set<Long> allow = new HashSet<>();
        Set<Long> deny = new HashSet<>();
        boolean anyAllow = false;
        List<DirectoryRule> rules = policy.rules() == null ? List.of() : policy.rules();
        for (DirectoryRule rule : rules) {
            if (!matches(tenantId, viewerId, rule.viewerSelection())) {
                continue;
            }
            Set<Long> targets = expand(tenantId, rule.targetSelection());
            if (rule.effect() == PolicyEffect.ALLOW) {
                anyAllow = true;
                allow.addAll(targets);
            } else {
                deny.addAll(targets);
            }
        }
        if (anyAllow) {
            allow.removeAll(deny);
            return DirectoryVisibility.explicit(allow, viewerId);
        }
        DirectoryDefaultScope scope = defaultScope(policy);
        if (scope == DirectoryDefaultScope.ALL) {
            return DirectoryVisibility.all(deny, viewerId);
        }
        Set<Long> included = new HashSet<>();
        if (scope == DirectoryDefaultScope.SELF) {
            included.add(viewerId);
        } else if (policy.defaultOverride() != null) {
            included.addAll(expand(tenantId, policy.defaultOverride().selection()));
        }
        included.removeAll(deny);
        return DirectoryVisibility.explicit(included, viewerId);
    }

    private DirectoryDefaultScope defaultScope(DirectoryPolicyDraft policy) {
        if (policy.defaultOverride() != null) {
            return policy.defaultOverride().scope();
        }
        long revisionId = IamIds.require(policy.defaultRevisionId());
        IamDefaultPolicyRevisionEntity revision = policies.findRevision(revisionId, DefaultPolicyKind.DIRECTORY);
        return DefaultPolicyDefinitions.directoryScope(revision == null ? null : revision.getDefinition());
    }

    private boolean matches(long tenantId, long memberId, Selection selection) {
        return expand(tenantId, selection).contains(memberId);
    }

    private Set<Long> expand(long tenantId, Selection selection) {
        Set<Long> ids = new LinkedHashSet<>();
        if (selection == null) {
            return ids;
        }
        for (String memberId : selection.members()) {
            ids.add(IamIds.require(memberId));
        }
        for (DepartmentSelection department : selection.departments()) {
            Set<Long> expanded = closures.expand(BigInteger.valueOf(tenantId),
                    List.of(BigInteger.valueOf(IamIds.require(department.id()))),
                    department.includeDescendants()).stream().map(BigInteger::longValueExact)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (expanded.isEmpty()) {
                continue;
            }
            ids.addAll(policies.memberIdsInDepartments(tenantId, expanded));
        }
        return ids;
    }
}
