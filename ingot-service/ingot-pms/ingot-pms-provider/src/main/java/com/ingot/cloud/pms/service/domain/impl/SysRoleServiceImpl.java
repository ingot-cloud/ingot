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
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnums;
import com.ingot.cloud.pms.api.model.transform.RoleTrans;
import com.ingot.cloud.pms.api.model.vo.role.RoleGroupItemVO;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVO;
import com.ingot.cloud.pms.core.BizIdGen;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysRoleUserService sysRoleUserService;
    private final SysRoleGroupService sysRoleGroupService;

    private final AssertionChecker assertI18nService;
    private final RoleTrans roleTrans;
    private final BizIdGen bizIdGen;

    private final Map<String, SysRole> roleCache = new ConcurrentHashMap<>();

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
                        ListUtil.list(false, RoleTypeEnums.Tenant, RoleTypeEnums.Custom))
                .eq(SysRole::getStatus, CommonStatusEnum.ENABLE))
                .stream()
                .map(roleTrans::option).collect(Collectors.toList());
    }

    @Override
    public List<RolePageItemVO> conditionList(SysRole condition, boolean isAdmin) {
        LambdaQueryWrapper<SysRole> query = Wrappers.lambdaQuery(condition)
                .in(!isAdmin, SysRole::getType,
                        ListUtil.list(false, RoleTypeEnums.Tenant, RoleTypeEnums.Custom));
        List<SysRole> temp = list(query);
        List<SysRoleGroup> groups = sysRoleGroupService.list(Wrappers.<SysRoleGroup>lambdaQuery()
                .in(!isAdmin, SysRoleGroup::getType,
                        ListUtil.list(false, RoleTypeEnums.Tenant, RoleTypeEnums.Custom)));
        return temp.stream().map(item -> {
            RolePageItemVO v = roleTrans.to(item);
            v.setGroupName(groups.stream()
                    .filter(group -> Objects.equals(group.getId(), item.getGroupId()))
                    .findFirst()
                    .map(SysRoleGroup::getName)
                    .orElse(null));
            return v;
        }).toList();
    }

    @Override
    public List<RoleGroupItemVO> groupRoleList(boolean isAdmin) {
        List<SysRoleGroup> groups = sysRoleGroupService.list(Wrappers.<SysRoleGroup>lambdaQuery()
                .orderByAsc(ListUtil.list(false, SysRoleGroup::getSort, SysRoleGroup::getId))
                .in(!isAdmin, SysRoleGroup::getType,
                        ListUtil.list(false, RoleTypeEnums.Tenant, RoleTypeEnums.Custom)));

        List<SysRole> roles = list(Wrappers.<SysRole>lambdaQuery()
                .in(!isAdmin, SysRole::getType,
                        ListUtil.list(false, RoleTypeEnums.Tenant, RoleTypeEnums.Custom)));

        return groups.stream()
                .map(item -> {
                    RoleGroupItemVO vo = new RoleGroupItemVO();
                    vo.setIsGroup(Boolean.TRUE);
                    vo.setId(item.getId());
                    vo.setName(item.getName());
                    vo.setType(item.getType());
                    vo.setChildren(roles.stream()
                            .filter(role -> Objects.equals(role.getGroupId(), item.getId()))
                            .map(role -> {
                                RoleGroupItemVO itemVo = new RoleGroupItemVO();
                                itemVo.setIsGroup(Boolean.FALSE);
                                itemVo.setId(role.getId());
                                itemVo.setName(role.getName());
                                itemVo.setType(role.getType());
                                itemVo.setGroupId(item.getId());
                                return itemVo;
                            }).toList());
                    return vo;
                }).toList();
    }

    @Override
    public IPage<RolePageItemVO> conditionPage(Page<SysRole> page,
                                               SysRole condition,
                                               boolean isAdmin) {
        LambdaQueryWrapper<SysRole> query = Wrappers.lambdaQuery(condition)
                .in(!isAdmin, SysRole::getType,
                        ListUtil.list(false, RoleTypeEnums.Tenant, RoleTypeEnums.Custom));
        IPage<SysRole> temp = page(page, query);

        List<SysRoleGroup> groups = sysRoleGroupService.list();
        return PageUtils.map(temp, item -> {
            RolePageItemVO v = roleTrans.to(item);
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
    public void createRole(SysRole params, boolean isAdmin) {
        long count = count(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, params.getCode()));
        assertI18nService.checkOperation(count == 0, "SysRoleServiceImpl.RoleCodeExisted");

        // 非超管只能创建自定义角色
        if (!isAdmin || params.getType() == null) {
            params.setType(RoleTypeEnums.Custom);
        }
        if (!isAdmin || StrUtil.isEmpty(params.getCode())) {
            params.setCode(bizIdGen.genOrgRoleCode());
        }

        params.setStatus(CommonStatusEnum.ENABLE);
        params.setCreatedAt(DateUtils.now());
        assertI18nService.checkOperation(save(params),
                "SysRoleServiceImpl.CreateFailed");
    }

    @Override
    public void removeRoleById(long id, boolean isAdmin) {
        SysRole role = getById(id);
        assertI18nService.checkOperation(role != null,
                "SysRoleServiceImpl.NonExist");

        assertI18nService.checkOperation(!RoleUtils.isAdmin(role.getCode()),
                "SysRoleServiceImpl.SuperAdminRemoveFailed");

        // 非超管只能删除自定义角色
        if (!isAdmin) {
            assertI18nService.checkOperation(role.getType() == RoleTypeEnums.Custom,
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

        roleCache.remove(role.getCode());
    }

    @Override
    public void updateRoleById(SysRole params, boolean isAdmin) {
        SysRole role = getById(params.getId());
        assertI18nService.checkOperation(role != null,
                "SysRoleServiceImpl.NonExist");

        if (!isAdmin) {
            assertI18nService.checkOperation(role.getType() == RoleTypeEnums.Custom,
                    "SysRoleServiceImpl.UpdateFailed");
        }

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
            params.setType(RoleTypeEnums.Custom);
        }
        if (!isAdmin) {
            params.setType(RoleTypeEnums.Custom);
            params.setTenantId(null);
            params.setSort(null);
        }
        sysRoleGroupService.save(params);
    }

    @Override
    public void updateGroup(SysRoleGroup params, boolean isAdmin) {
        SysRoleGroup group = sysRoleGroupService.getById(params.getId());
        assertI18nService.checkOperation(group != null,
                "SysRoleServiceImpl.GroupNotExisted");

        if (!isAdmin) {
            assertI18nService.checkOperation(group.getType() == RoleTypeEnums.Custom,
                    "SysRoleServiceImpl.GroupUpdateFailed");
        }

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
            assertI18nService.checkOperation(group.getType() == RoleTypeEnums.Custom,
                    "SysRoleServiceImpl.GroupOpsError");
        }

        sysRoleGroupService.removeById(id);
    }
}
