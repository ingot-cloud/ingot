package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.domain.SysRoleMenu;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleMenuMapper;
import com.ingot.cloud.pms.service.domain.SysRoleMenuService;
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
public class SysRoleMenuServiceImpl extends CommonRoleRelationService<SysRoleMenuMapper, SysRoleMenu> implements SysRoleMenuService {

    @Override
    public void menuBindRoles(RelationDto<Long, Long> params) {
        bindRoles(params,
                (roleId, targetId) -> remove(Wrappers.<SysRoleMenu>lambdaQuery()
                        .eq(SysRoleMenu::getRoleId, roleId)
                        .eq(SysRoleMenu::getMenuId, targetId)),
                (roleId, targetId) -> {
                    getBaseMapper().insertIgnore(roleId, targetId);
                    return true;
                }, "SysRoleMenuServiceImpl.RemoveFailed");
    }

    @Override
    public void roleBindMenus(RelationDto<Long, Long> params) {
        bindTargets(params,
                (roleId, targetId) -> remove(Wrappers.<SysRoleMenu>lambdaQuery()
                        .eq(SysRoleMenu::getRoleId, roleId)
                        .eq(SysRoleMenu::getMenuId, targetId)),
                (roleId, targetId) -> {
                    getBaseMapper().insertIgnore(roleId, targetId);
                    return true;
                }, "SysRoleMenuServiceImpl.RemoveFailed");
    }

    @Override
    public IPage<SysMenu> getRoleBindMenus(long roleId, Page<?> page) {
        return getBaseMapper().getRoleBindMenus(page, roleId);
    }
}
