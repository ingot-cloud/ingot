package com.ingot.cloud.iam.application.authorization.candidate;

import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.framework.authorization.ResourceObjectProvider;
import com.ingot.framework.commons.model.iam.ActionScopeCeiling;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.cloud.iam.application.assignment.model.AssignmentInput;
import com.ingot.cloud.iam.application.assignment.model.ValidationIssue;
import java.util.*;
import com.ingot.cloud.iam.infrastructure.persistence.RoleRepository;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import com.ingot.framework.authorization.*;
import org.springframework.stereotype.Service;

/**
 * <p>提交时校验固定角色绑定与委派对象上限，不依赖候选筛选结果。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformScopeBindingValidator {
    private final IamAccess access;
    private final RoleRepository roleStore;
    private final PlatformScopeObjectResolver objects;

    /**
     * 提交路径验证固定角色参数类型、资源归属及对象真实存在，不能信任前端候选筛选。
     * @param context 当前命令准入校验过的操作者身份
     * @param input 分配定义
     * @return 问题集合
     */
    public List<ValidationIssue> validateBindings(AuthorizationContext context, AssignmentInput input) {
        long revisionId = IamIds.require(input.roleRevisionRef().id());
        var definitions = roleStore.listParameters(revisionId);
        var bindings = input.scopeBindings();
        if (bindings == null) { return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT, "缺少范围参数")); }
        if (bindings.size() != definitions.size()
                || definitions.stream().anyMatch(parameter -> !bindings.containsKey(parameter.getParameterKey()))) {
            return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT,
                    "固定角色版本的范围参数必须全部配置"));
        }
        for (var entry : bindings.entrySet()) {
            var parameter = definitions.stream().filter(value -> value.getParameterKey().equals(entry.getKey())).findFirst().orElse(null);
            if (parameter == null || entry.getValue().kind() != parameter.getBindingKind()) {
                return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT, "参数未声明或类型不匹配"));
            }
            var resource = objects.objectResource(objects.objectActions(input.roleRevisionRef().id(), entry.getKey(), null, null));
            if (entry.getValue() == null || entry.getValue().ids() == null
                    || entry.getValue().ids().isEmpty() || !objectsExist(resource, entry.getValue(),context)) {
                return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT, "资源不支持对象查询或包含无效对象"));
            }
        }
        return List.of();
    }

    /**
     * 委派范围提交时按实际受保护对象标识校验。
     * @param ceiling 逐操作上限
     * @return 对象合法时 true
     */
    public boolean validCeilingObjects(ActionScopeCeiling ceiling) {
        var resource = objects.objectResource(objects.objectActions(null, null, ceiling.actionId(), null));
        var context=access.requireCurrent().context();
        return ceiling.scopeBindings().values().stream().allMatch(value -> objectsExist(resource, value,context));
    }

    private boolean objectsExist(ResourceObjectProvider provider,ScopeBinding binding,AuthorizationContext context) {
        if(provider==null || binding==null || binding.kind()!=ScopeBindingKind.OBJECTS)return false;
        return provider.objectsExist(context,binding.ids());
    }
}
