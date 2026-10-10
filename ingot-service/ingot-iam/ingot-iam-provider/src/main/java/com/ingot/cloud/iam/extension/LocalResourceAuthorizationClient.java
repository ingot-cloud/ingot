package com.ingot.cloud.iam.extension;

import java.util.LinkedHashMap;
import java.util.Map;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.framework.authorization.AuthorizationClient;
import com.ingot.framework.authorization.ResourceRegistry;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.ActionDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationRequest;
import com.ingot.framework.commons.model.iam.extension.ExecutionMode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * <p>
 * v2本地求值，身份、准入、范围和字段均来自同一次可信请求。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@Primary
@RequiredArgsConstructor
public class LocalResourceAuthorizationClient implements AuthorizationClient {

    private final CurrentIdentityService identities;

    private final AuthorizationEvaluator evaluator;

    private final ResourceRegistry registry;

    private final ScopeTransportCompiler compiler;

    private final RoleFieldPermissionService roleFields;

    private final AuthorizationCandidateMapper candidates;
    private final com.ingot.cloud.iam.policy.FieldAccessEvaluator tenantFields;
    private final com.ingot.cloud.iam.evaluation.ObjectScopeCompiler objectScopes;

    @Override
    public AuthorizationDecision evaluate(AuthorizationRequest request) { return evaluate(request, false); }

    @Override
    public AuthorizationDecision preview(AuthorizationRequest request) { return evaluate(request, true); }

    private AuthorizationDecision evaluate(AuthorizationRequest request, boolean preview) {
        var actor = identities.requireDomain(request.resource().domain());
        var descriptor = registry.require(request.resource()).descriptor();
        Map<String, ExecutionMode> modes = new LinkedHashMap<>();
        descriptor.actions().forEach(a -> modes.put(a.code(), a.mode()));
        if (request.actionCodes().isEmpty() || request.actionCodes().stream().anyMatch(c -> !modes.containsKey(c)))
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        // Catalog association is checked separately from the naming convention and the
        // provider descriptor.
        var metadata = candidates.actionMetadata(request.actionCodes());
        if (metadata.size() != request.actionCodes().stream().distinct().count() || metadata.stream()
            .anyMatch(a -> !a.applicationCode().equals(request.resource().applicationCode())
                    || !a.resourceCode().equals(request.resource().resourceCode())
                    || a.domain() != request.resource().domain()))
            throw new BizException(IamReasonCode.ACTION_DENIED);
        var view = evaluator.evaluateForExecution(actor.context(),
                !preview && request.actionCodes().stream().anyMatch(c -> modes.get(c) == ExecutionMode.MUTATING));
        Map<String, com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision> fieldDecisions = new LinkedHashMap<>();
        if (actor.context().domain() == com.ingot.framework.commons.model.iam.AuthorizationDomain.PLATFORM)
            fieldDecisions.putAll(roleFields.evaluate(request.resource(), actor.context(), view, request.actionCodes(),
                    !preview && request.actionCodes().stream().anyMatch(code -> modes.get(code) == ExecutionMode.MUTATING)));
        else for (var code : request.actionCodes()) {
            // 对象范围由同次可信授权视图提供；租户字段规则不独立开放对象。
            fieldDecisions.put(code, tenantFields.decision(actor.context(), request.resource(), code,
                    objectScopes.members(actor.context(), view.scope(code)), !preview && modes.get(code) == ExecutionMode.MUTATING));
        }
        Map<String, ActionDecision> actions = new LinkedHashMap<>();
        for (var code : request.actionCodes())
            actions.put(code, new ActionDecision(view.actionCodes().contains(code), view.governedCodes().contains(code),
                    compiler.compile(actor.context(), view.scope(code).clauses()), fieldDecisions.get(code)));
        return new AuthorizationDecision(request.resource(), actor.context(), actions, view.version(),
                fieldDecisions.values().stream().map(com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision::expiresAt)
                        .reduce(view.expiresAt(), com.ingot.framework.commons.model.iam.extension.FieldPolicyLifetime::earliest));
    }

}
