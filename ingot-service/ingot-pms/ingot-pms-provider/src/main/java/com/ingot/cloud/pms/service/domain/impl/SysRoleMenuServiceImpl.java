package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.domain.SysRoleMenu;
import com.ingot.cloud.pms.api.model.transform.MenuTrans;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNode;
import com.ingot.cloud.pms.api.utils.TreeUtils;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleMenuMapper;
import com.ingot.cloud.pms.service.domain.SysRoleMenuService;
import com.ingot.framework.core.model.dto.common.RelationDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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
@AllArgsConstructor
public class SysRoleMenuServiceImpl extends CommonRoleRelationService<SysRoleMenuMapper, SysRoleMenu> implements SysRoleMenuService {
    private final MenuTrans menuTrans;

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
    public List<MenuTreeNode> getRoleMenus(long roleId,
                                           boolean isBind,
                                           SysMenu condition) {
        List<SysMenu> all = getBaseMapper().getRoleMenus(roleId, isBind, condition);
        List<MenuTreeNode> allNode = all.stream()
                .sorted(Comparator.comparingInt(SysMenu::getSort))
                .map(menuTrans::to).collect(Collectors.toList());

        return TreeUtils.build(allNode, 0);
    }
}
