package com.ingot.cloud.pms.service.biz.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.MetaPermission;
import com.ingot.cloud.pms.api.model.domain.MetaMenu;
import com.ingot.cloud.pms.api.model.enums.PermissionTypeEnum;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.core.BizMenuUtils;
import com.ingot.cloud.pms.service.biz.BizMetaMenuService;
import com.ingot.cloud.pms.service.domain.MetaAppService;
import com.ingot.cloud.pms.service.domain.MetaPermissionService;
import com.ingot.cloud.pms.service.domain.MetaMenuService;
import com.ingot.cloud.pms.service.domain.MetaRolePermissionService;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizMetaMenuServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/13.</p>
 * <p>Time         : 10:59.</p>
 */
@Service
@RequiredArgsConstructor
public class BizMetaMenuServiceImpl implements BizMetaMenuService {
    private final MetaMenuService menuService;
    private final MetaPermissionService authorityService;
    private final MetaAppService appService;
    private final MetaRolePermissionService roleAuthorityService;

    private final AssertionChecker assertionChecker;

    @Override
    public List<MenuTreeNodeVO> getMenuByPermissions(List<? extends PermissionType> authorities) {
        List<MenuTreeNodeVO> allNodeList = menuService.nodeList();
        List<MenuTreeNodeVO> nodeList = BizMenuUtils.filterMenus(allNodeList, authorities);
        return TreeUtil.build(nodeList)
                .stream()
                .sorted(Comparator.comparing(MenuTreeNodeVO::getSort))
                .toList();
    }

    @Override
    public List<MenuTreeNodeVO> treeList(MetaMenu filter) {
        List<MenuTreeNodeVO> allNode = menuService.nodeList().stream()
                .filter(BizFilter.menuFilter(filter))
                .sorted(Comparator.comparing(MenuTreeNodeVO::getOrgType)
                        .thenComparing(MenuTreeNodeVO::getSort))
                .collect(Collectors.toList());

        return TreeUtil.build(allNode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(MetaMenu params) {
        Long permissionId = createAuthority(params);
        params.setPermissionId(permissionId);
        menuService.create(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MetaMenu menu) {
        MetaMenu current = menuService.getById(menu.getId());

        MetaPermission authority = new MetaPermission();
        authority.setId(current.getPermissionId());
        authority.setName(menu.getName());
        authority.setStatus(menu.getStatus());
        authority.setOrgType(menu.getOrgType());
        authorityService.update(authority);

        menuService.update(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        MetaMenu current = menuService.getById(id);
        // 判断是否为应用，如果是应用那么不可删除
        assertionChecker.checkOperation(appService.count(Wrappers.<MetaApp>lambdaQuery()
                        .eq(MetaApp::getMenuId, id)) == 0,
                "BizMetaMenuServiceImpl.IsApplication");

        // 删除权限
        authorityService.delete(current.getPermissionId());
        // 清空角色权限关联
        roleAuthorityService.clearByPermissionId(current.getPermissionId());

        menuService.delete(id);
    }

    private Long createAuthority(MetaMenu menu) {
        // 创建权限
        MetaPermission authority = new MetaPermission();
        authority.setName(menu.getName());
        authority.setCode(BizMenuUtils.getMenuAuthorityCode(menu));
        authority.setStatus(menu.getStatus());
        authority.setType(PermissionTypeEnum.MENU);
        authority.setOrgType(menu.getOrgType());

        if (menu.getPid() != null && menu.getPid() > 0) {
            MetaMenu parent = menuService.getById(menu.getPid());
            if (parent != null) {
                authority.setPid(parent.getPermissionId());
                authority.setOrgType(parent.getOrgType());
            }
        }
        return authorityService.createAndReturnId(authority, false);
    }
}
