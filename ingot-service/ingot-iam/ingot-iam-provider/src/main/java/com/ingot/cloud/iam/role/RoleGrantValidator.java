package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleParameterDefinition;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.ValidationIssue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>核对角色版本引用的操作与范围是否成立：操作存在且启用、所属应用域与角色一致、范围在资源能力内、管理范围参数已声明。</p>
 *
 * <p>发布、预览与升级共用同一套校验，无法证明成立时按失败关闭拒绝，不把越界授权留到求值期。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class RoleGrantValidator {
    private static final TypeReference<List<ScopeKind>> CAPABILITIES = new TypeReference<>() {
    };
    private static final String GRANTS = "grants";
    private final RoleRepository roles;

    /**
     * 校验一份合成后的授权是否可被角色版本引用。
     *
     * @param domain 角色要求的应用授权域：平台自定义角色为 PLATFORM，其余业务角色为 TENANT
     * @param grants 合成后的操作授权
     * @param parameters 本版本声明的范围参数
     * @return 校验问题；全部通过时为空
     */
    public List<ValidationIssue> validate(AuthorizationDomain domain, List<ActionGrant> grants,
                                         List<RoleParameterDefinition> parameters) {
        if (grants == null || grants.isEmpty()) {
            return List.of();
        }
        Map<String, ScopeBindingKind> declared = declared(parameters);
        Map<BigInteger, AuthorizationEvalRows.Capability> capabilities = capabilities(grants);
        List<ValidationIssue> errors = new ArrayList<>();
        for (ActionGrant grant : grants) {
            AuthorizationEvalRows.Capability capability = capabilities.get(BigInteger.valueOf(
                    IamIds.require(grant.actionId())));
            if (capability == null) {
                errors.add(new ValidationIssue(GRANTS, IamReasonCode.OBJECT_NOT_FOUND, "操作不存在"));
                continue;
            }
            if (!Boolean.TRUE.equals(capability.enabled()) || !Boolean.TRUE.equals(capability.appEnabled())
                    || !Boolean.TRUE.equals(capability.resourceEnabled())) {
                errors.add(new ValidationIssue(GRANTS, IamReasonCode.ACTION_DENIED, "操作或其应用、资源已停用"));
                continue;
            }
            if (capability.domain() != domain) {
                errors.add(new ValidationIssue(GRANTS, IamReasonCode.INVALID_ARGUMENT, "操作不属于该角色的管理域"));
                continue;
            }
            errors.addAll(scopes(grant, capability, declared));
        }
        return List.copyOf(errors);
    }

    private List<ValidationIssue> scopes(ActionGrant grant, AuthorizationEvalRows.Capability capability,
                                         Map<String, ScopeBindingKind> declared) {
        Set<ScopeKind> supported = supported(capability);
        List<ValidationIssue> errors = new ArrayList<>();
        for (ScopeExpression scope : grant.scopes() == null ? List.<ScopeExpression>of() : grant.scopes()) {
            if (scope.kind() == null || !supported.contains(scope.kind())) {
                errors.add(new ValidationIssue(GRANTS, IamReasonCode.INVALID_ARGUMENT, "资源不支持该数据范围"));
                continue;
            }
            ScopeBindingKind required = requiredBinding(scope.kind());
            if (required == null) {
                continue;
            }
            if (scope.parameterKey() == null || scope.parameterKey().isBlank()) {
                errors.add(new ValidationIssue(GRANTS, IamReasonCode.INVALID_ARGUMENT, "管理范围必须声明参数"));
            } else if (declared.get(scope.parameterKey()) != required) {
                errors.add(new ValidationIssue(GRANTS, IamReasonCode.INVALID_ARGUMENT, "范围参数未声明或种类不匹配"));
            }
        }
        return errors;
    }

    private Map<BigInteger, AuthorizationEvalRows.Capability> capabilities(List<ActionGrant> grants) {
        Set<BigInteger> ids = new LinkedHashSet<>();
        for (ActionGrant grant : grants) {
            ids.add(BigInteger.valueOf(IamIds.require(grant.actionId())));
        }
        Map<BigInteger, AuthorizationEvalRows.Capability> rows = new LinkedHashMap<>();
        for (AuthorizationEvalRows.Capability row : roles.listCapabilities(ids)) {
            rows.put(row.id(), row);
        }
        return rows;
    }

    private static Set<ScopeKind> supported(AuthorizationEvalRows.Capability capability) {
        List<ScopeKind> kinds = IamJson.read(capability.scopeCapabilities(), CAPABILITIES);
        return kinds == null ? Set.of() : new LinkedHashSet<>(kinds);
    }

    private static Map<String, ScopeBindingKind> declared(List<RoleParameterDefinition> parameters) {
        Map<String, ScopeBindingKind> declared = new LinkedHashMap<>();
        if (parameters != null) {
            for (RoleParameterDefinition parameter : parameters) {
                declared.put(parameter.key(), parameter.kind());
            }
        }
        return declared;
    }

    private static ScopeBindingKind requiredBinding(ScopeKind kind) {
        return switch (kind) {
            case MANAGED_DEPARTMENTS -> ScopeBindingKind.DEPARTMENTS;
            case OBJECT_SET -> ScopeBindingKind.OBJECTS;
            default -> null;
        };
    }
}
