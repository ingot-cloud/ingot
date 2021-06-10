package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleAuthorityMapper;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.framework.core.model.dto.common.RelationDto;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
public class SysRoleAuthorityServiceImpl extends CommonRoleRelationService<SysRoleAuthorityMapper, SysRoleAuthority> implements SysRoleAuthorityService {

    @Override
    public void authorityBindRoles(RelationDto<Long, Long> params) {
        bindRoles(params,
                (roleId, targetId) -> remove(Wrappers.<SysRoleAuthority>lambdaQuery()
                        .eq(SysRoleAuthority::getRoleId, roleId)
                        .eq(SysRoleAuthority::getAuthorityId, targetId)),
                (roleId, targetId) -> {
                    getBaseMapper().insertIgnore(roleId, targetId);
                    return true;
                }, "SysRoleAuthorityServiceImpl.RemoveFailed");
    }

    @Override
    public void roleBindAuthorities(RelationDto<Long, Long> params) {
        bindTargets(params,
                (roleId, targetId) -> remove(Wrappers.<SysRoleAuthority>lambdaQuery()
                        .eq(SysRoleAuthority::getRoleId, roleId)
                        .eq(SysRoleAuthority::getAuthorityId, targetId)),
                (roleId, targetId) -> {
                    getBaseMapper().insertIgnore(roleId, targetId);
                    return true;
                }, "SysRoleAuthorityServiceImpl.RemoveFailed");
    }
}
