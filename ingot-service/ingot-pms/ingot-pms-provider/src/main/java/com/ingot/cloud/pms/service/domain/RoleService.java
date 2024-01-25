package com.ingot.cloud.pms.service.domain;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.AppRoleGroup;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleGroup;
import com.ingot.cloud.pms.api.model.dto.role.RoleFilterDTO;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnums;
import com.ingot.cloud.pms.api.model.transform.RoleTrans;
import com.ingot.cloud.pms.api.model.vo.role.RoleGroupItemVO;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * <p>Description  : RoleService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/24.</p>
 * <p>Time         : 11:44.</p>
 */
public interface RoleService {

    default Function<AppRole, RolePageItemVO> appToRolePageItemMap(RoleTrans roleTrans, List<AppRoleGroup> groups) {
        return item -> {
            RolePageItemVO v = roleTrans.to(item);
            v.setGroupName(groups.stream()
                    .filter(group -> Objects.equals(group.getId(), item.getGroupId()))
                    .findFirst()
                    .map(AppRoleGroup::getName)
                    .orElse(null));
            return v;
        };
    }

    default Function<SysRole, RolePageItemVO> sysToRolePageItemMap(RoleTrans roleTrans, List<SysRoleGroup> groups) {
        return item -> {
            RolePageItemVO v = roleTrans.to(item);
            v.setGroupName(groups.stream()
                    .filter(group -> Objects.equals(group.getId(), item.getGroupId()))
                    .findFirst()
                    .map(SysRoleGroup::getName)
                    .orElse(null));
            return v;
        };
    }

    default Function<AppRoleGroup, RoleGroupItemVO> appToRoleGroupItemVOMap(List<AppRole> roles) {
        return item -> {
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
                        itemVo.setStatus(role.getStatus());
                        itemVo.setCode(role.getCode());
                        return itemVo;
                    }).toList());
            return vo;
        };
    }

    default Function<SysRoleGroup, RoleGroupItemVO> sysToRoleGroupItemVOMap(List<SysRole> roles) {
        return item -> {
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
                        itemVo.setStatus(role.getStatus());
                        itemVo.setCode(role.getCode());
                        return itemVo;
                    }).toList());
            return vo;
        };
    }

    default List<OrgTypeEnums> filterOrgTypeEnums(boolean isAdmin, RoleFilterDTO filter) {
        List<OrgTypeEnums> roleTypeList = new ArrayList<>();
        if (StrUtil.isNotEmpty(filter.getRoleType())) {
            roleTypeList.add(OrgTypeEnums.getEnum(filter.getRoleType()));
        } else {
            if (!isAdmin) {
                roleTypeList = ListUtil.list(false, OrgTypeEnums.Tenant, OrgTypeEnums.Custom);
            }
        }
        return roleTypeList;
    }
}
