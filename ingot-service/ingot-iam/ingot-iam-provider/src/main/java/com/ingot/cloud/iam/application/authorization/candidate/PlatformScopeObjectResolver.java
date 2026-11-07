package com.ingot.cloud.iam.application.authorization.candidate;

import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.framework.authorization.ResourceObjectProvider;
import com.ingot.framework.authorization.ResourceRegistry;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.DelegationInput;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ScopeKind;
import java.math.BigInteger;
import java.util.*;
import com.ingot.cloud.iam.infrastructure.persistence.AssignmentRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.application.role.PlatformRoleFacade;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import com.ingot.framework.authorization.*;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import org.springframework.stereotype.Service;

/**
 * <p>按固定版本与实际应用、资源归属解析对象查询提供者。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformScopeObjectResolver {
    private final AssignmentRepository assignments;
    private final PlatformRoleFacade roles;
    private final AuthorizationCandidateMapper candidates;
    private final ResourceRegistry registry;

    /** 按真实资源元数据解析，缺失与跨资源定义保持原拒绝语义。 */

    public ResourceObjectProvider objectResource(List<AuthorizationCandidateMapper.ActionRow> actions) {
        ResourceObjectProvider provider=null;
        for(var action:actions) {
            var key=new ResourceKey(AuthorizationDomain.PLATFORM,action.applicationCode(),action.resourceCode());
            var next=registry.find(key);
            if(next==null || provider!=null && provider!=next)return null;
            provider=next;
        }
        return provider;
    }
    /** 按真实资源元数据解析，缺失与跨资源定义保持原拒绝语义。 */

    public List<AuthorizationCandidateMapper.ActionRow> objectActions(String revisionId, String key, String actionId,
                                                                      DelegationInput basis) {
        List<String> actions;
        if (revisionId != null && !revisionId.isBlank()) {
            if (basis != null && basis.allowedRoleRevisionRefs().stream().noneMatch(ref -> ref.id().equals(revisionId))) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            var revision = assignments.findRevision(IamIds.require(revisionId));
            if (revision == null || revision.getDomain() != AuthorizationDomain.PLATFORM
                    || revision.getTenantId() != null || !Boolean.TRUE.equals(revision.getEnabled())) {
                throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
            }
            actions = roles.synthesizedGrants(IamIds.require(revisionId)).stream().filter(grant -> grant.scopes().stream()
                    .anyMatch(scope -> scope.kind() == ScopeKind.OBJECT_SET && Objects.equals(key, scope.parameterKey())))
                    .map(ActionGrant::actionId).distinct().toList();
        } else {
            actions = actionId == null || actionId.isBlank() ? List.of() : List.of(actionId);
        }
        if (actions.isEmpty()) { return List.of(); }
        var rows = candidates.actions(actions.stream().map(value -> BigInteger.valueOf(IamIds.require(value))).toList());
        if (rows.size() != actions.size() || rows.stream().map(AuthorizationCandidateMapper.ActionRow::applicationId)
                .distinct().count() != 1 || rows.stream().map(AuthorizationCandidateMapper.ActionRow::resourceId)
                .distinct().count() != 1) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return rows;
    }
}
