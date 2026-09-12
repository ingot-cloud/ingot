package com.ingot.cloud.pms.authorization.snapshot;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.domain.PlatformResource;
import com.ingot.cloud.pms.api.model.domain.PlatformRoleDataRule;
import com.ingot.cloud.pms.api.model.domain.TenantAppConfig;
import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.domain.TenantRoleDataRulePrivate;
import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationResourceRuleDTO;
import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationRoleBindingDTO;
import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.cloud.pms.authorization.engine.EffectiveAuthorization;
import com.ingot.cloud.pms.authorization.engine.EffectiveAuthorizationService;
import com.ingot.cloud.pms.authorization.engine.RoleBinding;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.biz.BizUserDeptService;
import com.ingot.cloud.pms.service.domain.PlatformPermissionService;
import com.ingot.cloud.pms.service.domain.PlatformResourceService;
import com.ingot.cloud.pms.service.domain.PlatformRoleDataRuleService;
import com.ingot.cloud.pms.service.domain.TenantAppConfigService;
import com.ingot.cloud.pms.service.domain.TenantRoleDataRulePrivateService;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotConstants;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>从统一授权解析器组装可序列化的授权快照，展开部门子孙并把租户管理员写成资源 ALL。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AuthorizationSnapshotAssembler {

    private final EffectiveAuthorizationService effectiveAuthorizationService;
    private final PlatformPermissionService platformPermissionService;
    private final PlatformResourceService platformResourceService;
    private final PlatformRoleDataRuleService platformRoleDataRuleService;
    private final TenantRoleDataRulePrivateService tenantRoleDataRuleService;
    private final TenantAppConfigService tenantAppConfigService;
    private final BizDeptService bizDeptService;
    private final BizUserDeptService bizUserDeptService;

    /**
     * 在指定租户上下文组装用户授权快照。
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @return 快照，合法空授权仍返回对象但不含热缓存价值
     */
    public AuthorizationSnapshotDTO assemble(long tenantId, long userId) {
        Instant generatedAt = Instant.now();
        return TenantEnv.applyAs(tenantId, () -> {
            EffectiveAuthorization authorization = effectiveAuthorizationService.resolve(tenantId, userId);
            AuthorizationSnapshotDTO snapshot = new AuthorizationSnapshotDTO();
            snapshot.setTenantId(tenantId);
            snapshot.setUserId(userId);
            snapshot.setRoleBindings(toBindingDtos(authorization.safeRoleBindings()));
            snapshot.setPermissionCodes(new LinkedHashSet<>(authorization.safeConcretePermissionCodes()));
            snapshot.setResourceRules(buildResourceRules(authorization, userId));
            snapshot.setSource(AuthorizationSnapshotConstants.SOURCE_REMOTE);
            snapshot.setVersion(generatedAt.toEpochMilli());
            snapshot.setGeneratedAt(generatedAt);
            snapshot.setExpiresAt(resolveExpiresAt(generatedAt));
            return snapshot;
        });
    }

    private List<AuthorizationRoleBindingDTO> toBindingDtos(List<RoleBinding> bindings) {
        List<AuthorizationRoleBindingDTO> result = new ArrayList<>();
        for (RoleBinding binding : bindings) {
            AuthorizationRoleBindingDTO dto = new AuthorizationRoleBindingDTO();
            dto.setRoleId(binding.getRoleId());
            dto.setPlatformRole(binding.isPlatformRole());
            dto.setRoleCode(binding.getRoleCode());
            dto.setDeptId(binding.getDeptId());
            dto.setFilterDept(binding.isFilterDept());
            result.add(dto);
        }
        return result;
    }

    private List<AuthorizationResourceRuleDTO> buildResourceRules(EffectiveAuthorization authorization, long userId) {
        Map<String, PlatformPermission> permissionByCode = new LinkedHashMap<>();
        for (PlatformPermission permission : platformPermissionService.list()) {
            if (permission.getStatus() == CommonStatusEnum.ENABLE && StrUtil.isNotBlank(permission.getCode())) {
                permissionByCode.put(permission.getCode(), permission);
            }
        }
        Map<Long, PlatformResource> resourceById = new LinkedHashMap<>();
        for (PlatformResource resource : platformResourceService.list()) {
            if (resource.getStatus() == CommonStatusEnum.ENABLE) {
                resourceById.put(resource.getId(), resource);
            }
        }
        Map<String, MergedRule> merged = new LinkedHashMap<>();
        if (authorization.isOrgAdmin()) {
            for (String code : authorization.safeConcretePermissionCodes()) {
                PlatformPermission permission = permissionByCode.get(code);
                if (permission == null || permission.getResourceId() == null) {
                    continue;
                }
                PlatformResource resource = resourceById.get(permission.getResourceId());
                if (resource == null || !authorization.safeAccessibleAppIds().contains(resource.getAppId())) {
                    continue;
                }
                merged.computeIfAbsent(key(resource.getCode(), code), k -> new MergedRule(resource.getCode(), code))
                        .all = true;
            }
            return toDtos(merged);
        }
        for (String code : authorization.safeConcretePermissionCodes()) {
            PlatformPermission permission = permissionByCode.get(code);
            if (permission == null || permission.getResourceId() == null) {
                continue;
            }
            PlatformResource resource = resourceById.get(permission.getResourceId());
            if (resource == null) {
                continue;
            }
            MergedRule bucket = merged.computeIfAbsent(key(resource.getCode(), code),
                    k -> new MergedRule(resource.getCode(), code));
            for (RoleBinding binding : authorization.safeRoleBindings()) {
                mergeBindingRules(bucket, binding, permission.getId(), resource.getId(), userId);
            }
        }
        merged.entrySet().removeIf(entry -> entry.getValue().isVacant());
        return toDtos(merged);
    }

    private void mergeBindingRules(MergedRule bucket,
                                   RoleBinding binding,
                                   long permissionId,
                                   long resourceId,
                                   long userId) {
        for (PlatformRoleDataRule rule : loadPlatform(binding)) {
            if (!Objects.equals(rule.getPermissionId(), permissionId)
                    || !Objects.equals(rule.getResourceId(), resourceId)) {
                continue;
            }
            apply(bucket, rule.getScopeType(), rule.getScopes(), binding, userId);
        }
        for (TenantRoleDataRulePrivate rule : tenantRoleDataRuleService.listByRole(
                binding.getRoleId(), binding.isPlatformRole())) {
            if (!Objects.equals(rule.getPermissionId(), permissionId)
                    || !Objects.equals(rule.getResourceId(), resourceId)) {
                continue;
            }
            apply(bucket, rule.getScopeType(), rule.getScopes(), binding, userId);
        }
    }

    private List<PlatformRoleDataRule> loadPlatform(RoleBinding binding) {
        if (!binding.isPlatformRole()) {
            return List.of();
        }
        return platformRoleDataRuleService.listByRoleId(binding.getRoleId());
    }

    private void apply(MergedRule bucket,
                       DataScopeTypeEnum scopeType,
                       List<Long> customDeptIds,
                       RoleBinding binding,
                       long userId) {
        if (scopeType == null) {
            return;
        }
        switch (scopeType) {
            case ALL -> bucket.all = true;
            case SELF -> bucket.self = true;
            case DEPT -> bucket.deptIds.addAll(resolveDeptRoots(binding, userId));
            case DEPT_AND_CHILD -> {
                for (Long root : resolveDeptRoots(binding, userId)) {
                    bucket.deptIds.addAll(bizDeptService.getDescendantList(root, true).stream()
                            .map(TenantDept::getId)
                            .toList());
                }
            }
            case CUSTOM -> bucket.deptIds.addAll(CollUtil.emptyIfNull(customDeptIds));
        }
    }

    private List<Long> resolveDeptRoots(RoleBinding binding, long userId) {
        if (binding.getDeptId() != null) {
            return List.of(binding.getDeptId());
        }
        return CollUtil.emptyIfNull(bizUserDeptService.getDeptIds(userId));
    }

    private Instant resolveExpiresAt(Instant generatedAt) {
        Instant cap = generatedAt.plusSeconds(AuthorizationSnapshotConstants.MAX_TTL_SECONDS
                - AuthorizationSnapshotConstants.CLOCK_SKEW_SECONDS);
        Instant nearest = nearestBoundary(generatedAt);
        if (nearest != null && nearest.isBefore(cap)) {
            return nearest;
        }
        return cap;
    }

    private Instant nearestBoundary(Instant generatedAt) {
        Instant nearest = null;
        for (TenantAppConfig config : tenantAppConfigService.list()) {
            nearest = earlierAfter(nearest, generatedAt, toInstant(config.getValidFrom()));
            nearest = earlierAfter(nearest, generatedAt, toInstant(config.getValidUntil()));
        }
        return nearest;
    }

    private static Instant earlierAfter(Instant current, Instant generatedAt, Instant candidate) {
        if (candidate == null || !candidate.isAfter(generatedAt)) {
            return current;
        }
        if (current == null || candidate.isBefore(current)) {
            return candidate;
        }
        return current;
    }

    private static Instant toInstant(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private static List<AuthorizationResourceRuleDTO> toDtos(Map<String, MergedRule> merged) {
        List<AuthorizationResourceRuleDTO> result = new ArrayList<>();
        for (MergedRule rule : merged.values()) {
            AuthorizationResourceRuleDTO dto = new AuthorizationResourceRuleDTO();
            dto.setResourceCode(rule.resourceCode);
            dto.setPermissionCode(rule.permissionCode);
            if (rule.all) {
                dto.setScopeType(DataScopeTypeEnum.ALL);
                dto.setSelf(Boolean.FALSE);
                dto.setDeptIds(new ArrayList<>());
            } else {
                dto.setScopeType(rule.self && rule.deptIds.isEmpty()
                        ? DataScopeTypeEnum.SELF : DataScopeTypeEnum.CUSTOM);
                dto.setSelf(rule.self);
                dto.setDeptIds(new ArrayList<>(rule.deptIds));
            }
            result.add(dto);
        }
        return result;
    }

    private static String key(String resourceCode, String permissionCode) {
        return resourceCode + "\0" + permissionCode;
    }

    private static final class MergedRule {
        private final String resourceCode;
        private final String permissionCode;
        private boolean all;
        private boolean self;
        private final Set<Long> deptIds = new LinkedHashSet<>();

        private MergedRule(String resourceCode, String permissionCode) {
            this.resourceCode = resourceCode;
            this.permissionCode = permissionCode;
        }

        private boolean isVacant() {
            return !all && !self && deptIds.isEmpty();
        }
    }
}
