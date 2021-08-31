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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Slf4j
@Service
@AllArgsConstructor
public class SysRoleMenuServiceImpl extends CommonRoleRelationService<SysRoleMenuMapper, SysRoleMenu> implements SysRoleMenuService {
    private final MenuTrans menuTrans;

    private final Do remove = (roleId, targetId) -> remove(Wrappers.<SysRoleMenu>lambdaQuery()
            .eq(SysRoleMenu::getRoleId, roleId)
            .eq(SysRoleMenu::getMenuId, targetId));
    private final Do bind = (roleId, targetId) -> {
        getBaseMapper().insertIgnore(roleId, targetId);
        return true;
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void menuBindRoles(RelationDto<Long, Long> params) {
        bindRoles(params, remove, bind,
                "SysRoleMenuServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindMenus(RelationDto<Long, Long> params) {
        bindTargets(params, remove, bind,
                "SysRoleMenuServiceImpl.RemoveFailed");
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
