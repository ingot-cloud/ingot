package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.cloud.pms.api.model.transform.AuthorityTrans;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleAuthorityMapper;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.model.dto.common.RelationDTO;
import com.ingot.framework.core.utils.tree.TreeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        getBaseMapper().insertIgnore(roleId, targetId);
        return true;
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.AUTHORITY_DETAILS, key = "'role-*'")
    public void authorityBindRoles(RelationDTO<Long, Long> params) {
        bindRoles(params, remove, bind,
                "SysRoleAuthorityServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.AUTHORITY_DETAILS, key = "'role-*'")
    public void roleBindAuthorities(RelationDTO<Long, Long> params) {
        bindTargets(params, remove, bind,
                "SysRoleAuthorityServiceImpl.RemoveFailed");
    }

    @Override
    @Cacheable(value = CacheConstants.AUTHORITY_DETAILS, key = "'role-' + #roleId", unless = "#result.isEmpty()")
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
