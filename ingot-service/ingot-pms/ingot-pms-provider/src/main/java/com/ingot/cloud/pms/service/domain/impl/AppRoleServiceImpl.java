package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.RoleConvert;
import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.AppRoleGroup;
import com.ingot.cloud.pms.api.model.domain.AppRoleUser;
import com.ingot.cloud.pms.api.model.dto.role.RoleFilterDTO;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.vo.role.RoleGroupItemVO;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVO;
import com.ingot.cloud.pms.core.BizIdGen;
import com.ingot.cloud.pms.mapper.AppRoleMapper;
import com.ingot.cloud.pms.service.domain.AppRoleGroupService;
import com.ingot.cloud.pms.service.domain.AppRoleService;
import com.ingot.cloud.pms.service.domain.AppRoleUserService;
import com.ingot.cloud.pms.service.domain.RoleService;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.support.Option;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
@Service
@RequiredArgsConstructor
public class AppRoleServiceImpl extends BaseServiceImpl<AppRoleMapper, AppRole> implements AppRoleService, RoleService {
    private final AppRoleUserService appRoleUserService;
    private final AppRoleGroupService appRoleGroupService;
    private final RoleConvert roleConvert;
    private final AssertionChecker assertionChecker;
    private final BizIdGen bizIdGen;
    private final Map<String, AppRole> roleCache = new ConcurrentHashMap<>();

    @Override
    public List<AppRole> getRolesOfUser(long userId) {
        // 基础角色ID
        Set<Long> baseRoleIds = appRoleUserService.list(Wrappers.<AppRoleUser>lambdaQuery()
                        .eq(AppRoleUser::getUserId, userId))
                .stream().map(AppRoleUser::getRoleId).collect(Collectors.toSet());

        if (CollUtil.isEmpty(baseRoleIds)) {
            return CollUtil.newArrayList();
        }

        return list(Wrappers.<AppRole>lambdaQuery()
                .eq(AppRole::getStatus, CommonStatusEnum.ENABLE)
                .in(AppRole::getId, baseRoleIds));
    }

    @Override
    public AppRole getRoleByCode(String code) {
        AppRole role = roleCache.get(TenantContextHolder.get() + code);
        if (role == null) {
            role = getOne(Wrappers.<AppRole>lambdaQuery().eq(AppRole::getCode, code));
            if (role != null) {
                roleCache.put(TenantContextHolder.get() + code, role);
            }
        }
        return role;
    }

    @Override
    public List<Option<Long>> options(boolean isAdmin) {
        return list(Wrappers.<AppRole>lambdaQuery()
                .in(!isAdmin, AppRole::getType,
                        ListUtil.list(false, OrgTypeEnum.Tenant, OrgTypeEnum.Custom))
                .eq(AppRole::getStatus, CommonStatusEnum.ENABLE))
                .stream()
                .map(roleConvert::option).collect(Collectors.toList());
    }

    @Override
    public List<RolePageItemVO> conditionList(AppRole condition, boolean isAdmin) {
        LambdaQueryWrapper<AppRole> query = Wrappers.lambdaQuery(condition)
                .in(!isAdmin, AppRole::getType,
                        ListUtil.list(false, OrgTypeEnum.Tenant, OrgTypeEnum.Custom));
        List<AppRole> temp = list(query);
        List<AppRoleGroup> groups = appRoleGroupService.list(Wrappers.<AppRoleGroup>lambdaQuery()
                .in(!isAdmin, AppRoleGroup::getType,
                        ListUtil.list(false, OrgTypeEnum.Tenant, OrgTypeEnum.Custom)));
        return temp.stream().map(appToRolePageItemMap(roleConvert, groups)).toList();
    }

    @Override
    public List<RoleGroupItemVO> groupRoleList(boolean isAdmin, RoleFilterDTO filter) {
        List<OrgTypeEnum> roleTypeList = filterOrgTypeEnums(isAdmin, filter);

        List<AppRoleGroup> groups = appRoleGroupService.list(Wrappers.<AppRoleGroup>lambdaQuery()
                .orderByAsc(ListUtil.list(false, AppRoleGroup::getSort, AppRoleGroup::getId))
                .in(CollUtil.isNotEmpty(roleTypeList), AppRoleGroup::getType, roleTypeList));

        List<AppRole> roles = list(Wrappers.<AppRole>lambdaQuery()
                .like(StrUtil.isNotEmpty(filter.getRoleName()), AppRole::getName, filter.getRoleName())
                .in(CollUtil.isNotEmpty(roleTypeList), AppRole::getType, roleTypeList));

        return groups.stream().map(appToRoleGroupItemVOMap(roles)).toList();
    }

    @Override
    public void createRole(AppRole params, boolean isAdmin) {
        long count = count(Wrappers.<AppRole>lambdaQuery()
                .eq(AppRole::getCode, params.getCode()));
        assertionChecker.checkOperation(count == 0, "SysRoleServiceImpl.RoleCodeExisted");

        // 非超管只能创建自定义角色
        if (!isAdmin || params.getType() == null) {
            params.setType(OrgTypeEnum.Custom);
        }
        if (!isAdmin || StrUtil.isEmpty(params.getCode())) {
            params.setCode(bizIdGen.genOrgAppRoleCode());
        }

        params.setStatus(CommonStatusEnum.ENABLE);
        params.setCreatedAt(DateUtil.now());
        assertionChecker.checkOperation(save(params),
                "SysRoleServiceImpl.CreateFailed");
    }

    @Override
    public void removeRoleById(long id, boolean isAdmin) {
        AppRole role = getById(id);
        assertionChecker.checkOperation(role != null,
                "SysRoleServiceImpl.NonExist");

        // 非超管只能删除自定义角色
        if (!isAdmin) {
            assertionChecker.checkOperation(role.getType() == OrgTypeEnum.Custom,
                    "SysRoleServiceImpl.DefaultRemoveFailed");
        }

        // 是否关联用户
        assertionChecker.checkOperation(appRoleUserService.count(
                        Wrappers.<AppRoleUser>lambdaQuery()
                                .eq(AppRoleUser::getRoleId, id)) == 0,
                "SysRoleServiceImpl.RemoveFailedExistRelationInfo");

        assertionChecker.checkOperation(removeById(id),
                "SysRoleServiceImpl.RemoveFailed");

        roleCache.remove(TenantContextHolder.get() + role.getCode());
    }

    @Override
    public void updateRoleById(AppRole params, boolean isAdmin) {
        AppRole role = getById(params.getId());
        assertionChecker.checkOperation(role != null,
                "SysRoleServiceImpl.NonExist");

        if (!isAdmin) {
            assertionChecker.checkOperation(role.getType() == OrgTypeEnum.Custom,
                    "SysRoleServiceImpl.UpdateFailed");
        }

        // 角色编码不可修改
        params.setCode(null);
        // 角色类型不能修改
        params.setType(null);
        params.setUpdatedAt(DateUtil.now());
        assertionChecker.checkOperation(updateById(params),
                "SysRoleServiceImpl.UpdateFailed");

        roleCache.remove(TenantContextHolder.get() + role.getCode());
    }

    @Override
    public void sortGroup(List<Long> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }

        AtomicInteger index = new AtomicInteger(0);
        List<AppRoleGroup> groups = list.stream().map(id -> {
            AppRoleGroup group = new AppRoleGroup();
            group.setId(id);
            group.setSort(index.getAndIncrement());
            return group;
        }).toList();

        appRoleGroupService.updateBatchById(groups);
    }

    @Override
    public void createGroup(AppRoleGroup params, boolean isAdmin) {
        if (params.getType() == null) {
            params.setType(OrgTypeEnum.Custom);
        }
        if (!isAdmin) {
            params.setType(OrgTypeEnum.Custom);
            params.setTenantId(null);
            params.setSort(null);
        }
        long count = appRoleGroupService.count(
                Wrappers.<AppRoleGroup>lambdaQuery()
                        .eq(AppRoleGroup::getName, params.getName()));
        assertionChecker.checkOperation(count == 0, "SysRoleServiceImpl.GroupNameExist");
        appRoleGroupService.save(params);
    }

    @Override
    public void updateGroup(AppRoleGroup params, boolean isAdmin) {
        AppRoleGroup group = appRoleGroupService.getById(params.getId());
        assertionChecker.checkOperation(group != null,
                "SysRoleServiceImpl.GroupNotExisted");

        if (!isAdmin) {
            assertionChecker.checkOperation(group.getType() == OrgTypeEnum.Custom,
                    "SysRoleServiceImpl.GroupUpdateFailed");
        }

        params.setType(null);

        appRoleGroupService.updateById(params);
    }

    @Override
    public void deleteGroup(long id, boolean isAdmin) {
        AppRoleGroup group = appRoleGroupService.getById(id);
        assertionChecker.checkOperation(group != null,
                "SysRoleServiceImpl.GroupNotExisted");

        long count = count(Wrappers.<AppRole>lambdaQuery().eq(AppRole::getGroupId, id));
        assertionChecker.checkOperation(count == 0,
                "SysRoleServiceImpl.GroupHasChild");

        if (!isAdmin) {
            assertionChecker.checkOperation(group.getType() == OrgTypeEnum.Custom,
                    "SysRoleServiceImpl.GroupOpsError");
        }

        appRoleGroupService.removeById(id);
    }
}
