package com.ingot.cloud.pms.service.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.convert.RoleConvert;
import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.AppRoleGroup;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleGroup;
import com.ingot.cloud.pms.api.model.dto.role.RoleFilterDTO;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.vo.role.RoleGroupItemVO;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVO;

/**
 * <p>Description  : RoleService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/24.</p>
 * <p>Time         : 11:44.</p>
 */
public interface RoleService {

    default Function<AppRole, RolePageItemVO> appToRolePageItemMap(RoleConvert roleConvert, List<AppRoleGroup> groups) {
        return item -> {
            RolePageItemVO v = roleConvert.to(item);
            v.setGroupName(groups.stream()
                    .filter(group -> Objects.equals(group.getId(), item.getGroupId()))
                    .findFirst()
                    .map(AppRoleGroup::getName)
                    .orElse(null));
            return v;
        };
    }

    default Function<SysRole, RolePageItemVO> sysToRolePageItemMap(RoleConvert roleConvert, List<SysRoleGroup> groups) {
        return item -> {
            RolePageItemVO v = roleConvert.to(item);
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
                        RoleGroupItemVO target = new RoleGroupItemVO();
                        BeanUtil.copyProperties(role, target);
                        target.setGroupId(item.getId());
                        target.setIsGroup(Boolean.FALSE);
                        return target;
                    }).toList());
            return vo;
        };
    }

    default List<OrgTypeEnum> filterOrgTypeEnums(boolean isAdmin, RoleFilterDTO filter) {
        List<OrgTypeEnum> roleTypeList = new ArrayList<>();
        if (StrUtil.isNotEmpty(filter.getRoleType())) {
            roleTypeList.add(OrgTypeEnum.getEnum(filter.getRoleType()));
        } else {
            if (!isAdmin) {
                roleTypeList = ListUtil.list(false, OrgTypeEnum.Tenant, OrgTypeEnum.Custom);
            }
        }
        return roleTypeList;
    }
}
