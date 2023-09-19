package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.enums.DeptRoleScopeEnum;
import com.ingot.cloud.pms.api.model.transform.RoleTrans;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVO;
import com.ingot.cloud.pms.mapper.SysRoleMapper;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.model.support.Option;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.PageUtils;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
import com.ingot.framework.security.common.utils.RoleUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
@RequiredArgsConstructor
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
    private final SysDeptService sysDeptService;
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysRoleDeptService sysRoleDeptService;
    private final SysRoleUserService sysRoleUserService;

    private final AssertionChecker assertI18nService;
    private final RoleTrans roleTrans;

    private final Map<String, SysRole> roleCache = new ConcurrentHashMap<>();


    @Override
    public List<SysRole> getAllRolesOfUser(long userId, long deptId) {
        // 基础角色ID
        Set<Long> baseRoleIds = sysRoleUserService.list(Wrappers.<SysRoleUser>lambdaQuery()
                        .eq(SysRoleUser::getUserId, userId))
                .stream().map(SysRoleUser::getRoleId).collect(Collectors.toSet());

        Set<Long> deptRoleIds = new HashSet<>();
        // 获取部门角色ID
        if (deptId > 0) {
            SysDept dept = sysDeptService.getById(deptId);
            deptRoleIds(dept, deptRoleIds);
        }

        // 合并去重
        baseRoleIds.addAll(deptRoleIds);

        return list(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, CommonStatusEnum.ENABLE)
                .in(SysRole::getId, baseRoleIds));
    }

    @Override
    public List<SysRole> getRolesOfDept(long deptId) {
        Set<Long> deptRoleIds = new HashSet<>();
        SysDept dept = sysDeptService.getById(deptId);
        deptRoleIds(dept, deptRoleIds);
        if (CollUtil.isEmpty(deptRoleIds)) {
            return CollUtil.empty(List.class);
        }
        return list(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, CommonStatusEnum.ENABLE)
                .in(SysRole::getId, deptRoleIds));
    }

    @Override
    public List<SysRole> getRolesOfUser(long userId) {
        Set<Long> baseRoleIds = sysRoleUserService.list(Wrappers.<SysRoleUser>lambdaQuery()
                        .eq(SysRoleUser::getUserId, userId))
                .stream().map(SysRoleUser::getRoleId).collect(Collectors.toSet());

        if (CollUtil.isEmpty(baseRoleIds)) {
            return CollUtil.newArrayList();
        }

        return list(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, CommonStatusEnum.ENABLE)
                .in(SysRole::getId, baseRoleIds));
    }

    @Override
    public List<Option<Long>> options() {
        return list(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, CommonStatusEnum.ENABLE))
                .stream()
                .map(roleTrans::option).collect(Collectors.toList());
    }

    @Override
    public IPage<RolePageItemVO> conditionPage(Page<SysRole> page, SysRole condition) {
        IPage<SysRole> temp = page(page, Wrappers.lambdaQuery(condition));
        return PageUtils.map(temp, item -> {
            RolePageItemVO v = roleTrans.to(item);
            // admin角色不可编辑
            v.setCanAction(!RoleUtils.isAdmin(v.getCode()));
            return v;
        });
    }

    @Override
    public SysRole getRoleByCode(String code) {
        SysRole role = roleCache.get(code);
        if (role == null) {
            role = getOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, code));
            if (role != null) {
                roleCache.put(code, role);
            }
        }

        return role;
    }

    @Override
    public void createRole(SysRole params) {
        long count = count(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, params.getCode()));
        assertI18nService.checkOperation(count == 0, "SysRoleServiceImpl.RoleCodeExisted");

        params.setStatus(CommonStatusEnum.ENABLE);
        params.setCreatedAt(DateUtils.now());
        assertI18nService.checkOperation(save(params),
                "SysRoleServiceImpl.CreateFailed");
    }

    @Override
    public void removeRoleById(long id) {
        SysRole role = getById(id);
        assertI18nService.checkOperation(role != null,
                "SysRoleServiceImpl.NonExist");

        assertI18nService.checkOperation(!RoleUtils.isAdmin(role.getCode()),
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
        // 是否关联用户
        assertI18nService.checkOperation(sysRoleUserService.count(
                        Wrappers.<SysRoleUser>lambdaQuery()
                                .eq(SysRoleUser::getRoleId, id)) == 0,
                "SysRoleServiceImpl.RemoveFailedExistRelationInfo");

        assertI18nService.checkOperation(removeById(id),
                "SysRoleServiceImpl.RemoveFailed");

        roleCache.remove(role.getCode());
    }

    @Override
    public void updateRoleById(SysRole params) {
        SysRole role = getById(params.getId());
        assertI18nService.checkOperation(role != null,
                "SysRoleServiceImpl.NonExist");

        if (params.getStatus() == CommonStatusEnum.LOCK) {
            assertI18nService.checkOperation(!RoleUtils.isAdmin(role.getCode()),
                    "SysRoleServiceImpl.DisableAdminFailed");
        }

        // 角色编码不可修改
        params.setCode(null);
        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(params),
                "SysRoleServiceImpl.UpdateFailed");

        roleCache.remove(role.getCode());
    }

    private void deptRoleIds(SysDept dept, Set<Long> deptRoleIds) {
        if (dept == null) {
            return;
        }
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
