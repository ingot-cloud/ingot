package com.ingot.cloud.pms.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.enums.DeptRoleScopeEnum;
import com.ingot.cloud.pms.mapper.SysRoleMapper;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleDept;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.service.SysDeptService;
import com.ingot.cloud.pms.service.SysRoleDeptService;
import com.ingot.cloud.pms.service.SysRoleService;
import com.ingot.cloud.pms.service.SysRoleUserService;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
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
    private final SysRoleUserService sysRoleUserService;
    private final SysRoleDeptService sysRoleDeptService;

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

    private void deptRoleIds(SysDept dept, Set<Long> deptRoleIds) {
        DeptRoleScopeEnum scope = dept.getScope();
        // 获取当前部门角色ID
        if (scope == DeptRoleScopeEnum.CURRENT) {
            deptRoleIds.addAll(sysRoleDeptService.list(Wrappers.<SysRoleDept>lambdaQuery()
                    .eq(SysRoleDept::getDeptId, dept))
                    .stream().map(SysRoleDept::getRoleId).collect(Collectors.toSet()));
            return;
        }
        // 获取直接子部门的角色ID
        if (scope == DeptRoleScopeEnum.CURRENT_CHILD) {
            List<SysDept> children = sysDeptService.list(Wrappers.<SysDept>lambdaQuery()
                    .eq(SysDept::getPid, dept.getId()));
            if (CollUtil.isEmpty(children)) {
                return;
            }

            for (SysDept childDept : children) {
                deptRoleIds(childDept, deptRoleIds);
            }
        }
    }
}
