package com.ingot.cloud.pms.service.biz.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.cloud.pms.api.model.vo.permission.PermissionTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.service.biz.BizPlatformPermissionService;
import com.ingot.cloud.pms.service.domain.PlatformAppService;
import com.ingot.cloud.pms.service.domain.PlatformPermissionService;
import com.ingot.cloud.pms.service.domain.PlatformRolePermissionService;
import com.ingot.framework.commons.model.enums.PermissionTypeEnum;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : BizPlatformAuthorityServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/12.</p>
 * <p>Time         : 15:00.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizPlatformPermissionServiceImpl implements BizPlatformPermissionService {
    private final PlatformPermissionService authorityService;
    private final PlatformRolePermissionService roleAuthorityService;
    private final PlatformAppService appService;

    private final AuthorityConvert authorityConvert;
    private final AssertionChecker assertionChecker;

    @Override
    public List<PermissionTreeNodeVO> treeList(PlatformPermission filter) {
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
    public void createNonMenuPermission(PlatformPermission authority) {
        assertionChecker.checkOperation(authority.getType() != PermissionTypeEnum.MENU,
                "BizPlatformAuthorityServiceImpl.CantCreateMenuAuthority");

        if (authority.getPid() != null) {
            PlatformPermission parent = authorityService.getById(authority.getPid());
            assertionChecker.checkOperation(parent != null, "BizPlatformAuthorityServiceImpl.ParentNotExist");
            assert parent != null;
            authority.setType(parent.getType());
            authority.setOrgType(parent.getOrgType());
        }

        authorityService.create(authority, true);
    }

    @Override
    public void updateNonMenuPermission(PlatformPermission authority) {
        PlatformPermission current = authorityService.getById(authority.getId());
        assertionChecker.checkOperation(current != null, "BizPlatformAuthorityServiceImpl.NotExist");
        assert current != null;
        assertionChecker.checkOperation(current.getType() != PermissionTypeEnum.MENU,
                "BizPlatformAuthorityServiceImpl.CantUpdateMenuAuthority");

        authorityService.update(authority);
    }

    @Override
    public void deleteNonMenuPermission(long id) {
        PlatformPermission current = authorityService.getById(id);
        assertionChecker.checkOperation(current != null, "BizPlatformAuthorityServiceImpl.NotExist");
        assert current != null;
        assertionChecker.checkOperation(current.getType() != PermissionTypeEnum.MENU,
                "BizPlatformAuthorityServiceImpl.CantDeleteMenuAuthority");

        // 判断是否为应用，如果是应用那么不可删除
        assertionChecker.checkOperation(appService.count(Wrappers.<PlatformApp>lambdaQuery()
                        .eq(PlatformApp::getMenuId, id)) == 0,
                "BizPlatformAuthorityServiceImpl.IsApplication");

        // 清空角色权限关联
        roleAuthorityService.clearByPermissionId(id);
        // 删除权限
        authorityService.delete(id);
    }
}
