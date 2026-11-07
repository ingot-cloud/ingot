package com.ingot.cloud.iam.application.role;

import com.ingot.cloud.iam.domain.role.RoleSynthesis;
import java.util.ArrayList;
import java.util.List;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.diagnosis.model.EffectiveRole;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.shared.model.ImpactSummary;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.role.model.RoleCreateInput;
import com.ingot.cloud.iam.application.role.model.RoleDefinitionDraft;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.cloud.iam.application.role.model.RoleSummary;
import com.ingot.cloud.iam.application.shared.model.UsageSummary;
import com.ingot.cloud.iam.application.assignment.model.ValidationIssue;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>执行对应平台角色用例，保留固定版本、字段权限和乐观锁规则。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformRolePreviewService {
    private final PlatformRoleOperations operationsContext;

    /**
     * 预览待发布定义的合成结果，无写入。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param input 待发布定义
     * @return 合成预览
     */
    public Preview<EffectiveRole> preview(AuthorizationDomain domain, boolean shared, String id,
            RoleDefinitionDraft input) {
        return preview(domain, shared, id, input, null);
    }

    /**
     * 按明确所选历史版本的固定共享基础预览，不误用最新基础。
     * @param domain 管理域
     * @param shared 共享入口
     * @param id 角色ID
     * @param input 草稿
     * @param sourceRevisionId 可选历史来源
     * @return 合成预览
     */
    public Preview<EffectiveRole> preview(AuthorizationDomain domain, boolean shared, String id,
            RoleDefinitionDraft input, String sourceRevisionId) {
        ActiveIdentity actor = operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.PREVIEW));
        ResourceDetail<RoleSummary> role = operationsContext.loadSummary(domain, shared, actor, IamIds.require(id));
        List<ValidationIssue> errors = new ArrayList<>();
        EffectiveRole effective = null;
        try {
            String base = operationsContext.currentBase(IamIds.require(id));
            if (sourceRevisionId != null) {
                var source = operationsContext.roles().findRevision(IamIds.require(sourceRevisionId));
                if (source == null || source.getRoleId().longValueExact() != IamIds.require(id))
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                base = PlatformRoleOperations.text(source.getBaseRevisionId());
            }
            RoleSynthesis.Result synthesized = PlatformRoleOperations.based(base) ? RoleSynthesis.synthesize(operationsContext.baseGrants(base), input.deltas())
                    : RoleSynthesis.synthesize(input.grants(), List.of());
            errors.addAll(operationsContext.validator().validate(PlatformRoleOperations.grantDomain(role.record().kind()), synthesized.grants(),
                    input.parameterDefinitions()));
            effective = new EffectiveRole(role.record(), new RoleRevisionRef(role.record().kind(), role.record().id()),
                    synthesized.grants(), synthesized.origins(),
                    input.parameterDefinitions() == null ? List.of() : input.parameterDefinitions(),
                    new UsageSummary(0L, 0L, false),
                    operationsContext.roleFields().freeze(role.record().kind(), synthesized.grants(), input.resourceFieldPermissions()));
        }
        catch (BizException exception) {
            errors.add(new ValidationIssue("definition", IamReasonCode.POLICY_CONFLICT, exception.getMessage()));
        }
        return new Preview<>(role.version(), errors.isEmpty(), errors, List.of(),
                new ImpactSummary(null, null, null, false), effective);
    }

    /**
     * 创建角色前只读校验完整定义，包括资源字段能力，不分配版本 ID。
     * @param input 平台角色创建草稿
     * @return 固化默认值后的预览
     */
    public Preview<RoleDefinitionDraft> previewCreate(RoleCreateInput input) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, PlatformRoleOperations.action(AuthorizationDomain.PLATFORM, false, PlatformRoleOperations.AccessKind.CREATE));
        List<ValidationIssue> issues = new ArrayList<>();
        RoleDefinitionDraft result = null;
        try {
            if (input.kind() != RoleKind.PLATFORM_CUSTOM)
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            PlatformRoleOperations.requireShape(input.kind(), PlatformRoleOperations.based(input.baseRevisionId()), input.definition());
            var definition = input.definition();
            issues.addAll(operationsContext.validator().validate(AuthorizationDomain.PLATFORM, definition.grants(),
                    definition.parameterDefinitions()));
            var snapshot = operationsContext.roleFields().freeze(input.kind(), definition.grants(), definition.resourceFieldPermissions());
            result = new RoleDefinitionDraft(definition.grants(), definition.deltas(),
                    definition.parameterDefinitions(), definition.metadataOverrides(), snapshot);
        }
        catch (BizException exception) {
            issues.add(new ValidationIssue("definition", IamReasonCode.INVALID_ARGUMENT, exception.getMessage()));
        }
        return new Preview<>("0", issues.isEmpty(), issues, List.of(), new ImpactSummary(null, null, null, false),
                result);
    }
}
