package com.ingot.cloud.pms.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.enums.DeptRoleScopeEnum;
import com.ingot.cloud.pms.api.model.transform.RoleTrans;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVo;
import com.ingot.cloud.pms.mapper.SysRoleMapper;
import com.ingot.cloud.pms.service.*;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.base.exception.IllegalOperationException;
import com.ingot.framework.base.utils.DateUtils;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.AssertionUtils;
import com.ingot.framework.core.validation.service.I18nService;
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
    private final SysRoleOauthClientService sysRoleOauthClientService;

    private final IdGenerator idGenerator;
    private final I18nService i18nService;
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
    public IPage<RolePageItemVo> conditionPage(Page<SysRole> page, SysRole condition) {
        IPage<SysRole> temp = page(page, Wrappers.lambdaQuery(condition));
        IPage<RolePageItemVo> result = new Page<>();
        result.setCurrent(temp.getCurrent());
        result.setTotal(temp.getTotal());
        result.setSize(temp.getSize());

        List<RolePageItemVo> records = temp.getRecords()
                .stream().map(roleTrans::to).collect(Collectors.toList());

        result.setRecords(records);
        return result;
    }

    @Override
    public void createRole(SysRole params) {
        params.setId(idGenerator.nextId());
        AssertionUtils.checkOperation(save(params),
                i18nService.getMessage("SysRoleServiceImpl.CreateFailed"));
    }

    @Override
    public void removeRoleById(long id) {
        AssertionUtils.checkOperation(removeById(id),
                i18nService.getMessage("SysRoleServiceImpl.DeleteFailed"));
    }

    @Override
    public void updateRoleById(SysRole params) {
        SysRole lock = getById(params.getId());
        if (lock == null) {
            throw new IllegalOperationException(
                    i18nService.getMessage("SysRoleServiceImpl.NonExist"));
        }

        params.setVersion(lock.getVersion());
        params.setUpdatedAt(DateUtils.now());
        AssertionUtils.checkOperation(updateById(params),
                i18nService.getMessage("SysRoleServiceImpl.UpdateFailed"));
    }

    private void deptRoleIds(SysDept dept, Set<Long> deptRoleIds) {
        DeptRoleScopeEnum scope = dept.getScope();
        switch (scope) {
            // 获取当前部门和子部门的角色ID
            case CURRENT_CHILD:
                List<SysDept> children = sysDeptService.list(Wrappers.<SysDept>lambdaQuery()
                        .eq(SysDept::getPid, dept.getId()));
                if (CollUtil.isEmpty(children)) {
                    return;
                }

                for (SysDept childDept : children) {
                    deptRoleIds(childDept, deptRoleIds);
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
