package com.ingot.cloud.pms.service.biz.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.MetaPermission;
import com.ingot.framework.commons.model.enums.PermissionTypeEnum;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.cloud.pms.api.model.vo.permission.PermissionTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.service.biz.BizMetaPermissionService;
import com.ingot.cloud.pms.service.domain.MetaAppService;
import com.ingot.cloud.pms.service.domain.MetaPermissionService;
import com.ingot.cloud.pms.service.domain.MetaRolePermissionService;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : BizMetaAuthorityServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/12.</p>
 * <p>Time         : 15:00.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizMetaPermissionServiceImpl implements BizMetaPermissionService {
    private final MetaPermissionService authorityService;
    private final MetaRolePermissionService roleAuthorityService;
    private final MetaAppService appService;

    private final AuthorityConvert authorityConvert;
    private final AssertionChecker assertionChecker;

    @Override
    public List<PermissionTreeNodeVO> treeList(MetaPermission filter) {
        List<PermissionTreeNodeVO> nodeList = authorityService.list()
                .stream()
                .filter(BizFilter.authorityFilter(filter))
                .sorted(Comparator.comparing(PermissionType::getOrgType)
                        .thenComparing(PermissionType::getId))
                .map(authorityConvert::toTreeNode).collect(Collectors.toList());

        List<PermissionTreeNodeVO> tree = TreeUtil.build(nodeList);
        TreeUtil.compensate(tree, nodeList);
        return tree;
    }

    @Override
    public void createNonMenuPermission(MetaPermission authority) {
        assertionChecker.checkOperation(authority.getType() != PermissionTypeEnum.MENU,
                "BizMetaAuthorityServiceImpl.CantCreateMenuAuthority");

        if (authority.getPid() != null) {
            MetaPermission parent = authorityService.getById(authority.getPid());
            assertionChecker.checkOperation(parent != null, "BizMetaAuthorityServiceImpl.ParentNotExist");
            assert parent != null;
            authority.setType(parent.getType());
            authority.setOrgType(parent.getOrgType());
        }

        authorityService.create(authority, true);
    }

    @Override
    public void updateNonMenuPermission(MetaPermission authority) {
        MetaPermission current = authorityService.getById(authority.getId());
        assertionChecker.checkOperation(current != null, "BizMetaAuthorityServiceImpl.NotExist");
        assert current != null;
        assertionChecker.checkOperation(current.getType() != PermissionTypeEnum.MENU,
                "BizMetaAuthorityServiceImpl.CantUpdateMenuAuthority");

        authorityService.update(authority);
    }

    @Override
    public void deleteNonMenuPermission(long id) {
        MetaPermission current = authorityService.getById(id);
        assertionChecker.checkOperation(current != null, "BizMetaAuthorityServiceImpl.NotExist");
        assert current != null;
        assertionChecker.checkOperation(current.getType() != PermissionTypeEnum.MENU,
                "BizMetaAuthorityServiceImpl.CantDeleteMenuAuthority");

        // 判断是否为应用，如果是应用那么不可删除
        assertionChecker.checkOperation(appService.count(Wrappers.<MetaApp>lambdaQuery()
                        .eq(MetaApp::getMenuId, id)) == 0,
                "BizMetaAuthorityServiceImpl.IsApplication");

        // 清空角色权限关联
        roleAuthorityService.clearByPermissionId(id);
        // 删除权限
        authorityService.delete(id);
    }
}
