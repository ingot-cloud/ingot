package com.ingot.cloud.member.service.biz.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.convert.MemberRoleConvert;
import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.api.model.vo.permission.MemberPermissionTreeNodeVO;
import com.ingot.cloud.member.api.model.vo.role.MemberRoleTreeNodeVO;
import com.ingot.cloud.member.common.BizFilter;
import com.ingot.cloud.member.common.BizUtils;
import com.ingot.cloud.member.service.biz.BizRoleService;
import com.ingot.cloud.member.service.domain.MemberPermissionService;
import com.ingot.cloud.member.service.domain.MemberRolePermissionService;
import com.ingot.cloud.member.service.domain.MemberRoleService;
import com.ingot.cloud.member.service.domain.MemberRoleUserService;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.support.Option;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizRoleServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 11:00.</p>
 */
@Service
@RequiredArgsConstructor
public class BizRoleServiceImpl implements BizRoleService {
    private final MemberRoleService roleService;
    private final MemberRolePermissionService rolePermissionService;
    private final MemberPermissionService permissionService;
    private final MemberRoleUserService roleUserService;

    private final AssertionChecker assertionChecker;

    @Override
    public List<Option<Long>> options(MemberRole condition) {
        return roleService.list().stream()
                .filter(BizFilter.roleFilter(condition))
                .map(role -> Option.of(role.getId(), role.getName()))
                .toList();
    }

    @Override
    public List<MemberRoleTreeNodeVO> conditionTree(MemberRole condition) {
        List<MemberRoleTreeNodeVO> list = roleService.list().stream()
                .filter(BizFilter.roleFilter(condition))
                .map(MemberRoleConvert.INSTANCE::toTreeNode)
                .toList();
        List<MemberRoleTreeNodeVO> tree = TreeUtil.build(list);
        TreeUtil.compensate(tree, list);
        return tree;
    }

    @Override
    public List<MemberPermission> getRolePermissions(long roleId) {
        List<Long> ids = rolePermissionService.getRoleBindPermissionIds(roleId);
        if (CollUtil.isEmpty(ids)) {
            return ListUtil.empty();
        }
        return permissionService.list(Wrappers.<MemberPermission>lambdaQuery()
                        .in(MemberPermission::getId, ids))
                .stream()
                .filter(permission -> permission.getStatus() == CommonStatusEnum.ENABLE)
                .toList();
    }

    @Override
    public List<MemberPermissionTreeNodeVO> getRolePermissionsTree(long roleId) {
        List<MemberPermission> permissions = getRolePermissions(roleId);
        return BizUtils.mapTree(permissions, null);
    }

    @Override
    public void create(MemberRole params) {
        roleService.create(params);
    }

    @Override
    public void update(MemberRole params) {
        roleService.update(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        // 清除关联权限
        rolePermissionService.clearByRoleId(id);
        // 清除关联用户
        roleUserService.clearByRoleId(id);
        // 删除角色
        roleService.delete(id);
    }

    @Override
    public void setPermissions(SetDTO<Long, Long> params) {
        if (CollUtil.isEmpty(params.getSetIds())) {
            return;
        }
        rolePermissionService.roleSetPermissions(params);
    }

    @Override
    public void assignUsers(AssignDTO<Long, Long> params) {
        MemberRole role = roleService.getById(params.getId());
        assertionChecker.checkOperation(role != null, "BizRoleServiceImpl.RoleNonNul");
        assert role != null;
        roleUserService.roleBindUsers(params);
    }

}
