package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.enums.DeptRoleScopeEnum;
import com.ingot.cloud.pms.api.model.transform.RoleTrans;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVo;
import com.ingot.cloud.pms.mapper.SysRoleMapper;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.validation.service.AssertI18nService;
import com.ingot.framework.security.constants.RoleConstants;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
@AllArgsConstructor
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
    private final SysDeptService sysDeptService;
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysRoleDeptService sysRoleDeptService;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysRoleOauthClientService sysRoleOauthClientService;
    private final SysRoleUserService sysRoleUserService;

    private final IdGenerator idGenerator;
    private final AssertI18nService assertI18nService;
    private final RoleTrans roleTrans;

    @Override
    public List<SysRole> getAllRolesOfUser(long userId, long deptId) {
        // 基础角色ID
        Set<Long> baseRoleIds = sysRoleUserService.list(Wrappers.<SysRoleUser>lambdaQuery()
                .eq(SysRoleUser::getUserId, userId))
                .stream().map(SysRoleUser::getRoleId).collect(Collectors.toSet());

        // 获取部门角色ID
        SysDept dept = sysDeptService.getById(deptId);
        Set<Long> deptRoleIds = new HashSet<>();
        deptRoleIds(dept, deptRoleIds);

        // 合并去重
        baseRoleIds.addAll(deptRoleIds);

        return list(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, CommonStatusEnum.ENABLE)
                .in(SysRole::getId, baseRoleIds));
    }

    @Override
    public List<SysRole> getAllRolesOfClients(List<Long> clientIds) {
        Set<Long> roleIdSet = sysRoleOauthClientService.list(Wrappers.<SysRoleOauthClient>lambdaQuery()
                .in(SysRoleOauthClient::getClientId, clientIds))
                .stream()
                .map(SysRoleOauthClient::getRoleId)
                .collect(Collectors.toSet());
        return list(Wrappers.<SysRole>lambdaQuery().in(SysRole::getId, roleIdSet));
    }

    @Override
    public List<SysRole> getRolesOfUser(long userId) {
        Set<Long> baseRoleIds = sysRoleUserService.list(Wrappers.<SysRoleUser>lambdaQuery()
                .eq(SysRoleUser::getUserId, userId))
                .stream().map(SysRoleUser::getRoleId).collect(Collectors.toSet());

        return list(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, CommonStatusEnum.ENABLE)
                .in(SysRole::getId, baseRoleIds));
    }

    @Override
    public IPage<RolePageItemVo> conditionPage(Page<SysRole> page, SysRole condition) {
        IPage<SysRole> temp = page(page, Wrappers.lambdaQuery(condition));
        IPage<RolePageItemVo> result = new Page<>();
        result.setCurrent(temp.getCurrent());
        result.setTotal(temp.getTotal());
        result.setSize(temp.getSize());

        List<RolePageItemVo> records = temp.getRecords()
                .stream().map(item -> {
                    RolePageItemVo v = roleTrans.to(item);
                    v.setCanAction(v.getId() != RoleConstants.ROLE_ADMIN_ID);
                    return v;
                }).collect(Collectors.toList());

        result.setRecords(records);
        return result;
    }

    @Override
    public void createRole(SysRole params) {
        params.setId(idGenerator.nextId());
        params.setStatus(CommonStatusEnum.ENABLE);
        params.setCreatedAt(DateUtils.now());
        assertI18nService.checkOperation(save(params),
                "SysRoleServiceImpl.CreateFailed");
    }

    @Override
    public void removeRoleById(long id) {
        assertI18nService.checkOperation(id != RoleConstants.ROLE_ADMIN_ID,
                "SysRoleServiceImpl.SuperAdminRemoveFailed");

        // 是否关联权限
        assertI18nService.checkOperation(sysRoleAuthorityService.count(
                Wrappers.<SysRoleAuthority>lambdaQuery()
                        .eq(SysRoleAuthority::getRoleId, id)) == 0,
                "SysRoleServiceImpl.RemoveFailedExistRelationInfo");
        // 是否关联部门
        assertI18nService.checkOperation(sysRoleDeptService.count(
                Wrappers.<SysRoleDept>lambdaQuery()
                        .eq(SysRoleDept::getRoleId, id)) == 0,
                "SysRoleServiceImpl.RemoveFailedExistRelationInfo");
        // 是否关联菜单
        assertI18nService.checkOperation(sysRoleMenuService.count(
                Wrappers.<SysRoleMenu>lambdaQuery()
                        .eq(SysRoleMenu::getRoleId, id)) == 0,
                "SysRoleServiceImpl.RemoveFailedExistRelationInfo");
        // 是否关联客户端
        assertI18nService.checkOperation(sysRoleOauthClientService.count(
                Wrappers.<SysRoleOauthClient>lambdaQuery()
                        .eq(SysRoleOauthClient::getRoleId, id)) == 0,
                "SysRoleServiceImpl.RemoveFailedExistRelationInfo");
        // 是否关联用户
        assertI18nService.checkOperation(sysRoleUserService.count(
                Wrappers.<SysRoleUser>lambdaQuery()
                        .eq(SysRoleUser::getRoleId, id)) == 0,
                "SysRoleServiceImpl.RemoveFailedExistRelationInfo");

        assertI18nService.checkOperation(removeById(id),
                "SysRoleServiceImpl.RemoveFailed");
    }

    @Override
    public void updateRoleById(SysRole params) {
        // 角色编码不可修改
        params.setCode(null);
        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(params),
                "SysRoleServiceImpl.UpdateFailed");
    }

    private void deptRoleIds(SysDept dept, Set<Long> deptRoleIds) {
        DeptRoleScopeEnum scope = dept.getScope();
        switch (scope) {
            // 获取当前部门和子部门的角色ID
            case CURRENT_CHILD:
                // 获取可用的 children
                List<SysDept> children = sysDeptService.list(Wrappers.<SysDept>lambdaQuery()
                        .eq(SysDept::getPid, dept.getId())
                        .eq(SysDept::getStatus, CommonStatusEnum.ENABLE));

                if (!CollUtil.isEmpty(children)) {
                    for (SysDept childDept : children) {
                        deptRoleIds(childDept, deptRoleIds);
                    }
                }

                // 获取当前部门角色ID
            case CURRENT:
                deptRoleIds.addAll(sysRoleDeptService.list(Wrappers.<SysRoleDept>lambdaQuery()
                        .eq(SysRoleDept::getDeptId, dept))
                        .stream().map(SysRoleDept::getRoleId).collect(Collectors.toSet()));
                break;
        }
    }
}
