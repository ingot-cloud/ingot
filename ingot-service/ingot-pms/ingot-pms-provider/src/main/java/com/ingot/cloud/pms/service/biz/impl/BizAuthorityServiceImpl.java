package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.ingot.cloud.pms.api.model.domain.MetaAuthority;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.service.biz.BizAuthorityService;
import com.ingot.cloud.pms.service.domain.MetaAuthorityService;
import com.ingot.cloud.pms.service.domain.MetaRoleAuthorityService;
import com.ingot.cloud.pms.service.domain.TenantRoleAuthorityPrivateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : BizAuthorityServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 13:20.</p>
 */
@Service
@RequiredArgsConstructor
public class BizAuthorityServiceImpl implements BizAuthorityService {
    private final MetaAuthorityService metaAuthorityService;
    private final MetaRoleAuthorityService metaRoleAuthorityService;
    private final TenantRoleAuthorityPrivateService tenantRoleAuthorityPrivateService;

    @Override
    public List<AuthorityType> getAuthoritiesByRoleIds(List<RoleType> roles) {
        List<MetaAuthority> all = metaAuthorityService.list();
        return roles.stream()
                .flatMap(role -> {
                    OrgTypeEnum orgType = role.getOrgType();
                    if (orgType == OrgTypeEnum.Platform) {
                        return metaRoleAuthorityService.getRoleBindAuthorityIds(role.getId()).stream()
                                .map(id -> all.stream()
                                        .filter(item -> item.getId().equals(id))
                                        .findFirst()
                                        .orElse(null))
                                .filter(Objects::nonNull);
                    }
                    return tenantRoleAuthorityPrivateService.getRoleBindAuthorityIds(role.getId()).stream()
                            .map(id -> all.stream()
                                    .filter(item -> item.getId().equals(id))
                                    .findFirst()
                                    .orElse(null))
                            .filter(Objects::nonNull);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthorityType> getAuthoritiesAndChildrenByRoleIds(List<RoleType> roles) {
        List<MetaAuthority> all = metaAuthorityService.list();
        CopyOnWriteArrayList<AuthorityType> authorities = new CopyOnWriteArrayList<>(getAuthoritiesByRoleIds(roles));
        authorities.forEach(item -> fillChildren(authorities, all, item));
        return authorities;
    }

    /**
     * 填充子权限
     */
    private void fillChildren(List<AuthorityType> result, List<? extends AuthorityType> all, AuthorityType parent) {
        all.stream()
                .filter(item -> item.getPid() != null && item.getPid().equals(parent.getId()))
                .forEach(item -> {
                    if (result.stream().noneMatch(a -> Objects.equals(a.getId(), item.getId()))) {
                        result.add(item);
                    }
                    fillChildren(result, all, item);
                });
    }
}
