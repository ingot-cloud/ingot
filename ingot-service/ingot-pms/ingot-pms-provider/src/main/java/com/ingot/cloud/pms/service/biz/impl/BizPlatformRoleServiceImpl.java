package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.convert.RoleConvert;
import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.domain.PlatformRole;
import com.ingot.cloud.pms.api.model.vo.permission.PermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.role.RoleTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.core.BizPermissionUtils;
import com.ingot.cloud.pms.service.biz.BizPlatformRoleService;
import com.ingot.cloud.pms.service.domain.PlatformPermissionService;
import com.ingot.cloud.pms.service.domain.PlatformRolePermissionService;
import com.ingot.cloud.pms.service.domain.PlatformRoleService;
import com.ingot.cloud.pms.service.domain.TenantRoleUserPrivateService;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.commons.model.support.Option;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizPlatformRoleServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Service
@RequiredArgsConstructor
public class BizPlatformRoleServiceImpl implements BizPlatformRoleService {
    private final PlatformRoleService roleService;
    private final PlatformRolePermissionService roleAuthorityService;
    private final PlatformPermissionService authorityService;
    private final TenantRoleUserPrivateService roleUserPrivateService;

    private final RoleConvert roleConvert;
    private final AuthorityConvert authorityConvert;

    @Override
    public List<Option<Long>> options(PlatformRole condition) {
        return roleService.list().stream()
                .filter(BizFilter.roleFilter(condition))
                .map(role -> Option.of(role.getId(), role.getName()))
                .toList();
    }

    @Override
    public List<RoleTreeNodeVO> conditionTree(PlatformRole condition) {
        List<RoleTreeNodeVO> list = roleService.list().stream()
                .filter(BizFilter.roleFilter(condition))
                .map(role -> BizUtils.convert(role, roleConvert))
                .toList();
        List<RoleTreeNodeVO> tree = TreeUtil.build(list);
        TreeUtil.compensate(tree, list);
        return tree;
    }

    @Override
    public List<PlatformPermission> getRolePermissions(long roleId) {
        List<Long> ids = roleAuthorityService.getRoleBindPermissionIds(roleId);
        if (CollUtil.isEmpty(ids)) {
            return ListUtil.empty();
        }
        return authorityService.list(Wrappers.<PlatformPermission>lambdaQuery()
                .in(PlatformPermission::getId, ids));
    }

    @Override
    public List<PermissionTreeNodeVO> getRolePermissionsTree(long roleId) {
        List<PlatformPermission> authorities = getRolePermissions(roleId);
        return BizPermissionUtils.mapTree(authorities, authorityConvert, null);
    }

    @Override
    public void create(PlatformRole params) {
        roleService.create(params);
    }

    @Override
    public void update(PlatformRole params) {
        roleService.update(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        // 清除关联权限
        roleAuthorityService.clearByRoleId(id);
        // 清除关联用户
        roleUserPrivateService.clearByRoleId(id);
        // 删除角色
        roleService.delete(id);
    }

    @Override
    public void setPermissions(SetDTO<Long, Long> params) {
        roleAuthorityService.roleSetPermissions(params);
    }
}
