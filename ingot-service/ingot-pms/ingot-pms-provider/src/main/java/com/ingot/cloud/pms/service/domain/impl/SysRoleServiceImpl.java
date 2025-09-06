package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRoleGroup;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.api.model.dto.role.RoleFilterDTO;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.convert.RoleConvert;
import com.ingot.cloud.pms.api.model.vo.role.RoleGroupItemVO;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVO;
import com.ingot.cloud.pms.core.BizIdGen;
import com.ingot.cloud.pms.mapper.SysRoleMapper;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.support.Option;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.utils.PageUtils;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.data.redis.service.RedisCacheService;
import com.ingot.framework.commons.utils.RoleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleMapper, SysRole> implements SysRoleService, RoleService {
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysRoleUserService sysRoleUserService;
    private final SysRoleGroupService sysRoleGroupService;
    private final RedisCacheService redisCacheService;

    private final AssertionChecker assertI18nService;
    private final RoleConvert roleConvert;
    private final BizIdGen bizIdGen;

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
    public List<Option<Long>> options(boolean isAdmin) {
        return list(Wrappers.<SysRole>lambdaQuery()
                .in(!isAdmin, SysRole::getType,
                        ListUtil.list(false, OrgTypeEnum.Tenant, OrgTypeEnum.Custom))
                .eq(SysRole::getStatus, CommonStatusEnum.ENABLE))
                .stream()
                .map(roleConvert::option).collect(Collectors.toList());
    }

    @Override
    public List<RolePageItemVO> conditionList(SysRole condition, boolean isAdmin) {
        LambdaQueryWrapper<SysRole> query = Wrappers.lambdaQuery(condition)
                .in(!isAdmin, SysRole::getType,
                        ListUtil.list(false, OrgTypeEnum.Tenant, OrgTypeEnum.Custom));
        List<SysRole> temp = list(query);
        List<SysRoleGroup> groups = sysRoleGroupService.list(Wrappers.<SysRoleGroup>lambdaQuery()
                .in(!isAdmin, SysRoleGroup::getType,
                        ListUtil.list(false, OrgTypeEnum.Tenant, OrgTypeEnum.Custom)));
        return temp.stream().map(sysToRolePageItemMap(roleConvert, groups)).toList();
    }

    @Override
    public List<RoleGroupItemVO> groupRoleList(boolean isAdmin, RoleFilterDTO filter) {
        List<OrgTypeEnum> roleTypeList = filterOrgTypeEnums(isAdmin, filter);

        List<SysRoleGroup> groups = sysRoleGroupService.list(Wrappers.<SysRoleGroup>lambdaQuery()
                .orderByAsc(ListUtil.list(false, SysRoleGroup::getSort, SysRoleGroup::getId))
                .in(CollUtil.isNotEmpty(roleTypeList), SysRoleGroup::getType, roleTypeList));

        List<SysRole> roles = CollUtil.emptyIfNull(list(Wrappers.<SysRole>lambdaQuery()
                        .like(StrUtil.isNotEmpty(filter.getRoleName()), SysRole::getName, filter.getRoleName())
                        .in(CollUtil.isNotEmpty(roleTypeList), SysRole::getType, roleTypeList)))
                .stream()
                .filter(item -> isAdmin || item.getStatus() == CommonStatusEnum.ENABLE)
                .collect(Collectors.toList());

        return groups.stream().map(sysToRoleGroupItemVOMap(roles))
                .sorted(Comparator.comparing(RoleGroupItemVO::getType))
                .toList();
    }

    @Override
    public IPage<RolePageItemVO> conditionPage(Page<SysRole> page,
                                               SysRole condition,
                                               boolean isAdmin) {
        LambdaQueryWrapper<SysRole> query = Wrappers.<SysRole>lambdaQuery()
                .like(StrUtil.isNotEmpty(condition.getName()), SysRole::getName, condition.getName())
                .like(StrUtil.isNotEmpty(condition.getCode()), SysRole::getCode, condition.getCode())
                .in(!isAdmin, SysRole::getType,
                        ListUtil.list(false, OrgTypeEnum.Tenant, OrgTypeEnum.Custom));
        IPage<SysRole> temp = page(page, query);

        List<SysRoleGroup> groups = sysRoleGroupService.list();
        return PageUtils.map(temp, item -> {
            RolePageItemVO v = roleConvert.to(item);
            v.setGroupName(groups.stream()
                    .filter(group -> Objects.equals(group.getId(), item.getGroupId()))
                    .findFirst()
                    .map(SysRoleGroup::getName)
                    .orElse(null));
            return v;
        });
    }

    @Override
    public SysRole getRoleByCode(String code) {
        SysRole role = redisCacheService.get(code);
        if (role == null) {
            role = getOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, code));
            if (role != null) {
                redisCacheService.cache(code, role);
            }
        }

        return role;
    }

    @Override
    public List<SysRole> getRoleListByCodes(List<String> codes) {
        List<SysRole> result = new ArrayList<>();
        List<String> notCachedCodes = new ArrayList<>();
        for (String code : codes) {
            SysRole role = redisCacheService.get(code);
            if (role != null) {
                result.add(role);
            } else {
                notCachedCodes.add(code);
            }
        }

        if (CollUtil.isNotEmpty(notCachedCodes)) {
            if (CollUtil.size(notCachedCodes) == 1) {
                result.add(getOne(Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getCode, notCachedCodes.get(0))));
            } else {
                List<SysRole> roles = list(Wrappers.<SysRole>lambdaQuery()
                        .in(SysRole::getCode, notCachedCodes));
                result.addAll(roles);
            }
        }

        return result;
    }

    @Override
    public void createRole(SysRole params, boolean isAdmin) {
        long count = count(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, params.getCode()));
        assertI18nService.checkOperation(count == 0, "SysRoleServiceImpl.RoleCodeExisted");

        // 非超管只能创建自定义角色
        if (!isAdmin || params.getType() == null) {
            params.setType(OrgTypeEnum.Custom);
        }
        if (!isAdmin || StrUtil.isEmpty(params.getCode())) {
            params.setCode(bizIdGen.genOrgSysRoleCode());
        }

        params.setStatus(CommonStatusEnum.ENABLE);
        params.setCreatedAt(DateUtil.now());
        assertI18nService.checkOperation(save(params),
                "SysRoleServiceImpl.CreateFailed");
    }

    @Override
    public void removeRoleById(long id, boolean isAdmin) {
        SysRole role = getById(id);
        assertI18nService.checkOperation(role != null,
                "SysRoleServiceImpl.NonExist");

        assertI18nService.checkOperation(!RoleUtil.isAdmin(role.getCode()),
                "SysRoleServiceImpl.SuperAdminRemoveFailed");

        // 非超管只能删除自定义角色
        if (!isAdmin) {
            assertI18nService.checkOperation(role.getType() == OrgTypeEnum.Custom,
                    "SysRoleServiceImpl.DefaultRemoveFailed");
        }

        // 是否关联权限
        assertI18nService.checkOperation(sysRoleAuthorityService.count(
                        Wrappers.<SysRoleAuthority>lambdaQuery()
                                .eq(SysRoleAuthority::getRoleId, id)) == 0,
                "SysRoleServiceImpl.RemoveFailedExistRelationInfo");

        // 是否关联用户
        assertI18nService.checkOperation(sysRoleUserService.count(
                        Wrappers.<SysRoleUser>lambdaQuery()
                                .eq(SysRoleUser::getRoleId, id)) == 0,
                "SysRoleServiceImpl.RemoveFailedExistRelationInfo");

        assertI18nService.checkOperation(removeById(id),
                "SysRoleServiceImpl.RemoveFailed");

        redisCacheService.delete(role.getCode());
    }

    @Override
    public void updateRoleById(SysRole params, boolean isAdmin) {
        SysRole role = getById(params.getId());
        assertI18nService.checkOperation(role != null,
                "SysRoleServiceImpl.NonExist");

        if (!isAdmin) {
            assertI18nService.checkOperation(role.getType() == OrgTypeEnum.Custom,
                    "SysRoleServiceImpl.UpdateFailed");
        }

        if (params.getStatus() == CommonStatusEnum.LOCK) {
            assertI18nService.checkOperation(!RoleUtil.isAdmin(role.getCode()),
                    "SysRoleServiceImpl.DisableAdminFailed");
        }

        // 角色编码不可修改
        params.setCode(null);
        // 角色类型不能修改
        params.setType(null);
        params.setUpdatedAt(DateUtil.now());
        assertI18nService.checkOperation(updateById(params),
                "SysRoleServiceImpl.UpdateFailed");

        redisCacheService.delete(role.getCode());
    }

    @Override
    public void sortGroup(List<Long> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }

        AtomicInteger index = new AtomicInteger(0);
        List<SysRoleGroup> groups = list.stream().map(id -> {
            SysRoleGroup group = new SysRoleGroup();
            group.setId(id);
            group.setSort(index.getAndIncrement());
            return group;
        }).toList();

        sysRoleGroupService.updateBatchById(groups);
    }

    @Override
    public void createGroup(SysRoleGroup params, boolean isAdmin) {
        if (params.getType() == null) {
            params.setType(OrgTypeEnum.Custom);
        }
        if (!isAdmin) {
            params.setType(OrgTypeEnum.Custom);
            params.setTenantId(null);
            params.setSort(null);
        }
        long count = sysRoleGroupService.count(
                Wrappers.<SysRoleGroup>lambdaQuery()
                        .eq(SysRoleGroup::getName, params.getName()));
        assertI18nService.checkOperation(count == 0, "SysRoleServiceImpl.GroupNameExist");
        sysRoleGroupService.save(params);
    }

    @Override
    public void updateGroup(SysRoleGroup params, boolean isAdmin) {
        SysRoleGroup group = sysRoleGroupService.getById(params.getId());
        assertI18nService.checkOperation(group != null,
                "SysRoleServiceImpl.GroupNotExisted");

        if (!isAdmin) {
            assertI18nService.checkOperation(group.getType() == OrgTypeEnum.Custom,
                    "SysRoleServiceImpl.GroupUpdateFailed");
        }

        params.setType(null);

        sysRoleGroupService.updateById(params);
    }

    @Override
    public void deleteGroup(long id, boolean isAdmin) {
        SysRoleGroup group = sysRoleGroupService.getById(id);
        assertI18nService.checkOperation(group != null,
                "SysRoleServiceImpl.GroupNotExisted");

        long count = count(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getGroupId, id));
        assertI18nService.checkOperation(count == 0,
                "SysRoleServiceImpl.GroupHasChild");

        if (!isAdmin) {
            assertI18nService.checkOperation(group.getType() == OrgTypeEnum.Custom,
                    "SysRoleServiceImpl.GroupOpsError");
        }

        sysRoleGroupService.removeById(id);
    }
}
