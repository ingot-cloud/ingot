package com.ingot.cloud.member.service.biz.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.ingot.cloud.member.api.model.convert.MemberPermissionConvert;
import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.api.model.vo.permission.MemberPermissionTreeNodeVO;
import com.ingot.cloud.member.common.BizFilter;
import com.ingot.cloud.member.service.biz.BizPermissionService;
import com.ingot.cloud.member.service.domain.MemberPermissionService;
import com.ingot.cloud.member.service.domain.MemberRolePermissionService;
import com.ingot.framework.commons.model.enums.PermissionTypeEnum;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : BizPermissionServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 11:14.</p>
 */
@Service
@RequiredArgsConstructor
public class BizPermissionServiceImpl implements BizPermissionService {
    private final MemberPermissionService permissionService;
    private final MemberRolePermissionService rolePermissionService;

    private final AssertionChecker assertionChecker;

    @Override
    public List<MemberPermissionTreeNodeVO> treeList(MemberPermission filter) {
        List<MemberPermissionTreeNodeVO> nodeList = permissionService.list()
                .stream()
                .filter(BizFilter.authorityFilter(filter))
                .sorted(Comparator.comparing(MemberPermission::getId))
                .map(MemberPermissionConvert.INSTANCE::toTreeNode)
                .collect(Collectors.toList());

        List<MemberPermissionTreeNodeVO> tree = TreeUtil.build(nodeList);
        TreeUtil.compensate(tree, nodeList);
        return tree;
    }

    @Override
    public void createNonMenuPermission(MemberPermission authority) {
        assertionChecker.checkOperation(authority.getType() != PermissionTypeEnum.MENU,
                "BizPermissionServiceImpl.CantCreateMenuAuthority");

        if (authority.getPid() != null) {
            MemberPermission parent = permissionService.getById(authority.getPid());
            assertionChecker.checkOperation(parent != null, "BizPermissionServiceImpl.ParentNotExist");
            assert parent != null;
            authority.setType(parent.getType());
        }

        permissionService.create(authority, true);
    }

    @Override
    public void updateNonMenuPermission(MemberPermission authority) {
        MemberPermission current = permissionService.getById(authority.getId());
        assertionChecker.checkOperation(current != null, "BizPermissionServiceImpl.NotExist");
        assert current != null;
        assertionChecker.checkOperation(current.getType() != PermissionTypeEnum.MENU,
                "BizPermissionServiceImpl.CantUpdateMenuAuthority");

        permissionService.update(authority);
    }

    @Override
    public void deleteNonMenuPermission(long id) {
        MemberPermission current = permissionService.getById(id);
        assertionChecker.checkOperation(current != null, "BizPermissionServiceImpl.NotExist");
        assert current != null;
        assertionChecker.checkOperation(current.getType() != PermissionTypeEnum.MENU,
                "BizPermissionServiceImpl.CantDeleteMenuAuthority");

        // 清空角色权限关联
        rolePermissionService.clearByPermissionId(id);
        // 删除权限
        permissionService.delete(id);
    }
}
