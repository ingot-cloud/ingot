package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.cloud.pms.api.model.transform.AuthorityTrans;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleAuthorityMapper;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.utils.tree.TreeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
@RequiredArgsConstructor
public class SysRoleAuthorityServiceImpl extends CommonRoleRelationService<SysRoleAuthorityMapper, SysRoleAuthority, Long> implements SysRoleAuthorityService {
    private final AuthorityTrans authorityTrans;

    private final Do<Long> remove = (roleId, targetId) -> remove(Wrappers.<SysRoleAuthority>lambdaQuery()
            .eq(SysRoleAuthority::getRoleId, roleId)
            .eq(SysRoleAuthority::getAuthorityId, targetId));
    private final Do<Long> bind = (roleId, targetId) -> {
        SysRoleAuthority params = new SysRoleAuthority();
        params.setRoleId(roleId);
        params.setAuthorityId(targetId);
        params.insert();
        return true;
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.AUTHORITY_DETAILS, key = CacheKey.AuthorityRoleAllKey)
    public void authorityBindRoles(RelationDTO<Long, Long> params) {
        bindRoles(params, remove, bind,
                "SysRoleAuthorityServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.AUTHORITY_DETAILS, key = CacheKey.AuthorityRoleAllKey)
    public void roleBindAuthorities(RelationDTO<Long, Long> params) {
        bindTargets(params, remove, bind,
                "SysRoleAuthorityServiceImpl.RemoveFailed");
    }

    @Override
    public void clearRole(List<Long> roleIds) {
        int size = CollUtil.size(roleIds);
        if (size == 0) {
            return;
        }
        remove(Wrappers.<SysRoleAuthority>lambdaQuery()
                .eq(size == 1, SysRoleAuthority::getRoleId, roleIds.get(0))
                .in(size > 1, SysRoleAuthority::getRoleId, roleIds));
    }

    @Override
    public void clearRoleWithAuthorities(List<Long> roleIds, List<Long> authorityIds) {
        int roleSize = CollUtil.size(roleIds);
        int authoritySize = CollUtil.size(authorityIds);
        if (roleSize == 0 || authoritySize == 0) {
            return;
        }

        remove(Wrappers.<SysRoleAuthority>lambdaQuery()
                .eq(authoritySize == 1, SysRoleAuthority::getAuthorityId, authorityIds.get(0))
                .in(authoritySize > 1, SysRoleAuthority::getAuthorityId, authorityIds)
                .eq(roleSize == 1, SysRoleAuthority::getRoleId, roleIds.get(0))
                .in(roleSize > 1, SysRoleAuthority::getRoleId, roleIds));

    }

    @Override
    @Cacheable(value = CacheConstants.AUTHORITY_DETAILS, key = CacheKey.AuthorityRoleKey, unless = "#result.isEmpty()")
    public List<SysAuthority> getAuthoritiesByRole(long roleId) {
        return CollUtil.emptyIfNull(baseMapper.getAuthoritiesByRole(roleId));
    }

    @Override
    public List<AuthorityTreeNodeVO> getRoleAuthorities(long roleId,
                                                        SysAuthority condition) {
        List<SysAuthority> authorities = SpringContextHolder.getBean(SysRoleAuthorityService.class)
                .getAuthoritiesByRole(roleId);
        List<AuthorityTreeNodeVO> nodeList = authorities.stream()
                .filter(BizFilter.authorityFilter(condition))
                .map(authorityTrans::to).collect(Collectors.toList());

        List<AuthorityTreeNodeVO> tree = TreeUtils.build(nodeList);
        TreeUtils.compensate(tree, nodeList);
        return tree;
    }
}
