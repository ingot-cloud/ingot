package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.MetaRoleAuthority;
import com.ingot.cloud.pms.mapper.MetaRoleAuthorityMapper;
import com.ingot.cloud.pms.service.domain.MetaRoleAuthorityService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Service
public class MetaRoleAuthorityServiceImpl extends BaseServiceImpl<MetaRoleAuthorityMapper, MetaRoleAuthority> implements MetaRoleAuthorityService {

    @Override
    @CacheEvict(
            value = CacheConstants.META_ROLE_AUTHORITIES,
            key = "'role-' + #params.id"
    )
    @Transactional(rollbackFor = Exception.class)
    public void roleSetAuthorities(SetDTO<Long, Long> params) {
        Long roleId = params.getId();
        List<Long> bindIds = params.getSetIds();

        // 清空当前权限
        remove(Wrappers.<MetaRoleAuthority>lambdaQuery()
                .eq(MetaRoleAuthority::getRoleId, roleId));

        if (CollUtil.isNotEmpty(bindIds)) {
            List<MetaRoleAuthority> bindList = getBindList(roleId, bindIds);
            saveBatch(bindList);
        }
    }

    @Override
    @CacheEvict(
            value = CacheConstants.META_ROLE_AUTHORITIES,
            key = "'role-' + #params.id"
    )
    @Transactional(rollbackFor = Exception.class)
    public void roleAssignAuthorities(AssignDTO<Long, Long> params) {
        Long roleId = params.getId();
        List<Long> bindIds = params.getAssignIds();
        List<Long> unbindIds = params.getUnassignIds();

        if (CollUtil.isNotEmpty(unbindIds)) {
            remove(Wrappers.<MetaRoleAuthority>lambdaQuery()
                    .eq(MetaRoleAuthority::getRoleId, roleId)
                    .in(MetaRoleAuthority::getAuthorityId, unbindIds));
        }

        if (CollUtil.isNotEmpty(bindIds)) {
            List<MetaRoleAuthority> bindList = getBindList(roleId, bindIds);
            saveBatch(bindList);
        }
    }

    private List<MetaRoleAuthority> getBindList(long roleId, List<Long> bindIds) {
        return CollUtil.emptyIfNull(bindIds).stream()
                .map(authorityId -> {
                    MetaRoleAuthority bind = new MetaRoleAuthority();
                    bind.setRoleId(roleId);
                    bind.setAuthorityId(authorityId);
                    return bind;
                }).toList();
    }

    @Override
    @Cacheable(
            value = CacheConstants.META_ROLE_AUTHORITIES,
            key = "'role-' + #id",
            unless = "#result.isEmpty()"
    )
    public List<Long> getRoleBindAuthorityIds(long id) {
        return CollUtil.emptyIfNull(
                list(Wrappers.<MetaRoleAuthority>lambdaQuery()
                        .eq(MetaRoleAuthority::getRoleId, id))
                        .stream()
                        .map(MetaRoleAuthority::getAuthorityId)
                        .toList()
        );
    }

    @Override
    @CacheEvict(
            value = CacheConstants.META_ROLE_AUTHORITIES,
            allEntries = true
    )
    public void clearByAuthorityId(long authorityId) {
        remove(Wrappers.<MetaRoleAuthority>lambdaQuery()
                .eq(MetaRoleAuthority::getAuthorityId, authorityId));
    }

    @Override
    @CacheEvict(
            value = CacheConstants.META_ROLE_AUTHORITIES,
            allEntries = true
    )
    public void clearByRoleId(long roleId) {
        remove(Wrappers.<MetaRoleAuthority>lambdaQuery()
                .eq(MetaRoleAuthority::getRoleId, roleId));
    }


}
