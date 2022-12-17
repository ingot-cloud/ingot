package com.ingot.cloud.pms.service.domain.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.domain.SysRoleMenu;
import com.ingot.cloud.pms.api.model.transform.MenuTrans;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.api.utils.TreeUtils;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleMenuMapper;
import com.ingot.cloud.pms.service.domain.SysRoleMenuService;
import com.ingot.framework.core.constants.IDConstants;
import com.ingot.framework.core.model.dto.common.RelationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@RequiredArgsConstructor
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
    public void menuBindRoles(RelationDTO<Long, Long> params) {
        bindRoles(params, remove, bind,
                "SysRoleMenuServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindMenus(RelationDTO<Long, Long> params) {
        bindTargets(params, remove, bind,
                "SysRoleMenuServiceImpl.RemoveFailed");
    }

    @Override
    public List<MenuTreeNodeVO> getRoleMenus(long roleId,
                                             boolean isBind,
                                             SysMenu condition) {
        List<SysMenu> all = getBaseMapper().getRoleMenus(roleId, isBind, condition);
        List<MenuTreeNodeVO> allNode = all.stream()
                .sorted(Comparator.comparingInt(SysMenu::getSort))
                .map(menuTrans::to).collect(Collectors.toList());

        List<MenuTreeNodeVO> tree = TreeUtils.build(allNode, IDConstants.ROOT_TREE_ID);

        // 如果是获取绑定列表，需要检测树中是否包含所有绑定的节点，如果存在未添加的，则直接添加到列表中
        // 由于可能存在角色只绑定了某一个子节点并未绑定该节点的父，那么在构建树的时候，
        // 该子节点将会被忽略，所以需要进行补偿。
        // allNode是默认是通过创建时间升序排列的，这样可以保证父节点永远在其子节点的前面，避免重复添加
        if (isBind) {
            allNode.forEach(item -> {
                        if (!TreeUtils.contains(tree, item)) {
                            tree.add(item);
                        }
                    });
        }

        return tree;
    }
}
