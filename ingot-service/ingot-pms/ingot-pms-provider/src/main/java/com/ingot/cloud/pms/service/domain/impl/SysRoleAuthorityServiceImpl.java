package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.cloud.pms.api.model.dto.authority.AuthorityFilterDTO;
import com.ingot.cloud.pms.api.model.transform.AuthorityTrans;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.core.AuthorityUtils;
import com.ingot.cloud.pms.mapper.SysRoleAuthorityMapper;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.data.redis.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleAuthorityServiceImpl extends CommonRoleRelationService<SysRoleAuthorityMapper, SysRoleAuthority, Long> implements SysRoleAuthorityService {
    private final AuthorityTrans authorityTrans;
    private final RedisTemplate<String, Object> redisTemplate;

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
    public void authorityBindRoles(RelationDTO<Long, Long> params) {
        // 清空所有权限角色绑定缓存
        RedisUtils.deleteKeys(redisTemplate,
                ListUtil.list(false,
                        CacheConstants.AUTHORITY_DETAILS + "*"
                ));
        bindRoles(params, remove, bind,
                "SysRoleAuthorityServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindAuthorities(RelationDTO<Long, Long> params) {
        List<Long> bindIds = params.getBindIds();
        Long roleId = params.getId();

        // 如果没有绑定权限，那么不处理
        if (CollUtil.isEmpty(bindIds)) {
            return;
        }

        // 清空指定权限角色绑定缓存
        RedisUtils.deleteKeys(redisTemplate,
                ListUtil.list(false,
                        CacheConstants.AUTHORITY_DETAILS + "::" + CacheKey.getAuthorityRoleKey(roleId)
                ));
        // 清空当前权限
        remove(Wrappers.<SysRoleAuthority>lambdaQuery()
                .eq(SysRoleAuthority::getRoleId, roleId));

        List<SysRoleAuthority> bindList = CollUtil.emptyIfNull(bindIds).stream()
                .map(authorityId -> {
                    SysRoleAuthority sysRoleAuthority = new SysRoleAuthority();
                    sysRoleAuthority.setRoleId(roleId);
                    sysRoleAuthority.setAuthorityId(authorityId);
                    return sysRoleAuthority;
                }).toList();
        saveBatch(bindList);
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
                                                        AuthorityFilterDTO condition) {
        List<SysAuthority> authorities = SpringContextHolder.getBean(SysRoleAuthorityService.class)
                .getAuthoritiesByRole(roleId);

        return AuthorityUtils.mapTree(authorities, condition, authorityTrans);
    }
}
