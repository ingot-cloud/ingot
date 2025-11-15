package com.ingot.cloud.pms.service.biz.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.MetaAuthority;
import com.ingot.cloud.pms.api.model.domain.MetaMenu;
import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.core.MenuUtils;
import com.ingot.cloud.pms.service.biz.BizMetaMenuService;
import com.ingot.cloud.pms.service.domain.MetaAppService;
import com.ingot.cloud.pms.service.domain.MetaAuthorityService;
import com.ingot.cloud.pms.service.domain.MetaMenuService;
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
    private final MetaAuthorityService authorityService;
    private final MetaAppService appService;

    private final AssertionChecker assertionChecker;

    @Override
    public List<MenuTreeNodeVO> getMenuByAuthorities(List<? extends AuthorityType> authorities) {
        List<MenuTreeNodeVO> allNodeList = menuService.nodeList();
        List<MenuTreeNodeVO> nodeList = MenuUtils.filterMenus(allNodeList, authorities);
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
        Long authorityId = createAuthority(params);
        params.setAuthorityId(authorityId);
        menuService.create(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MetaMenu menu) {
        MetaMenu current = menuService.getById(menu.getId());

        MetaAuthority authority = new MetaAuthority();
        authority.setId(current.getAuthorityId());
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
        authorityService.delete(current.getAuthorityId());

        menuService.delete(id);
    }

    private Long createAuthority(MetaMenu menu) {
        // 创建权限
        MetaAuthority authority = new MetaAuthority();
        if (menu.getPid() != null && menu.getPid() > 0) {
            MetaMenu parent = menuService.getById(menu.getPid());
            authority.setPid(parent != null ? parent.getAuthorityId() : null);
        }

        authority.setName(menu.getName());
        authority.setCode(MenuUtils.getMenuAuthorityCode(menu));
        authority.setStatus(menu.getStatus());
        authority.setOrgType(menu.getOrgType());
        return authorityService.createAndReturnId(authority, false);
    }
}
