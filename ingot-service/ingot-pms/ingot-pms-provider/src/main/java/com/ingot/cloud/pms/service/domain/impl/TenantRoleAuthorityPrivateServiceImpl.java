package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.TenantRoleAuthorityPrivate;
import com.ingot.cloud.pms.api.model.dto.common.BizBindDTO;
import com.ingot.cloud.pms.mapper.TenantRoleAuthorityPrivateMapper;
import com.ingot.cloud.pms.service.domain.TenantRoleAuthorityPrivateService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Service
public class TenantRoleAuthorityPrivateServiceImpl extends BaseServiceImpl<TenantRoleAuthorityPrivateMapper, TenantRoleAuthorityPrivate> implements TenantRoleAuthorityPrivateService {

    @Override
    @CacheEvict(
            value = CacheConstants.TENANT_ROLE_AUTHORITIES,
            key = "'role-' + #params.id"
    )
    @Transactional(rollbackFor = Exception.class)
    public void roleBindAuthorities(BizBindDTO params) {
        Long roleId = params.getId();
        List<Long> bindIds = params.getBindIds();
        boolean metaFlag = params.isMetaFlag();

        // 清空当前权限
        remove(Wrappers.<TenantRoleAuthorityPrivate>lambdaQuery()
                .eq(TenantRoleAuthorityPrivate::getRoleId, roleId));

        List<TenantRoleAuthorityPrivate> bindList = CollUtil.emptyIfNull(bindIds).stream()
                .map(authorityId -> {
                    TenantRoleAuthorityPrivate bind = new TenantRoleAuthorityPrivate();
                    bind.setRoleId(roleId);
                    bind.setAuthorityId(authorityId);
                    bind.setMetaRole(metaFlag);
                    return bind;
                }).toList();
        if (CollUtil.isNotEmpty(bindList)) {
            saveBatch(bindList);
        }
    }

    @Override
    @Cacheable(
            value = CacheConstants.META_AUTHORITIES,
            key = "'role-' + #id",
            unless = "#result.isEmpty()"
    )
    public List<Long> getRoleBindAuthorityIds(long id) {
        return CollUtil.emptyIfNull(
                list(Wrappers.<TenantRoleAuthorityPrivate>lambdaQuery()
                        .eq(TenantRoleAuthorityPrivate::getRoleId, id))
                        .stream()
                        .map(TenantRoleAuthorityPrivate::getAuthorityId)
                        .toList()
        );
    }

    @Override
    @CacheEvict(
            value = CacheConstants.TENANT_ROLE_AUTHORITIES,
            allEntries = true
    )
    public void clearByAuthorityId(long authorityId) {
        remove(Wrappers.<TenantRoleAuthorityPrivate>lambdaQuery()
                .eq(TenantRoleAuthorityPrivate::getAuthorityId, authorityId));
    }

    @Override
    @CacheEvict(
            value = CacheConstants.TENANT_ROLE_AUTHORITIES,
            allEntries = true
    )
    public void clearByRoleId(long roleId) {
        remove(Wrappers.<TenantRoleAuthorityPrivate>lambdaQuery()
                .eq(TenantRoleAuthorityPrivate::getRoleId, roleId));
    }
}
