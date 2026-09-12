package com.ingot.cloud.pms.service.biz.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ingot.cloud.pms.api.model.domain.PlatformMenu;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.core.BizMenuUtils;
import com.ingot.cloud.pms.service.biz.BizPlatformMenuService;
import com.ingot.cloud.pms.service.domain.PlatformMenuPermissionService;
import com.ingot.cloud.pms.service.domain.PlatformMenuService;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>平台菜单树装配与菜单行持久化；不再托管权限生命周期，可见性由 {@link PlatformMenuPermissionService} 维护。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class BizPlatformMenuServiceImpl implements BizPlatformMenuService {
    private final PlatformMenuService menuService;
    private final PlatformMenuPermissionService menuPermissionService;

    @Override
    public List<MenuTreeNodeVO> getMenuByPermissions(List<? extends PermissionType> authorities) {
        List<MenuTreeNodeVO> nodeList = BizMenuUtils.filterMenus(attachPermissionIds(menuService.nodeList()), authorities);
        return TreeUtil.build(nodeList)
                .stream()
                .sorted(Comparator.comparing(MenuTreeNodeVO::getSort))
                .toList();
    }

    @Override
    public List<MenuTreeNodeVO> treeList(PlatformMenu filter) {
        List<MenuTreeNodeVO> allNode = attachPermissionIds(menuService.nodeList()).stream()
                .filter(BizFilter.menuFilter(filter))
                .sorted(Comparator.comparing(MenuTreeNodeVO::getOrgType)
                        .thenComparing(MenuTreeNodeVO::getSort))
                .collect(Collectors.toList());

        return TreeUtil.build(allNode);
    }

    @Override
    public void create(PlatformMenu params) {
        menuService.create(params);
    }

    @Override
    public void update(PlatformMenu menu) {
        menuService.update(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        menuPermissionService.clearByMenuId(id);
        menuService.delete(id);
    }

    private List<MenuTreeNodeVO> attachPermissionIds(List<MenuTreeNodeVO> nodes) {
        Map<Long, List<Long>> permissionIds = menuPermissionService.mapPermissionIds();
        for (MenuTreeNodeVO node : nodes) {
            node.setPermissionIds(permissionIds.getOrDefault(node.getId(), List.of()));
        }
        return nodes;
    }
}
