package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.AppRoleGroup;
import com.ingot.cloud.pms.api.model.dto.role.RoleFilterDTO;
import com.ingot.cloud.pms.api.model.vo.role.RoleGroupItemVO;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVO;
import com.ingot.cloud.pms.mapper.AppRoleMapper;
import com.ingot.cloud.pms.service.domain.AppRoleService;
import com.ingot.framework.commons.model.support.Option;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
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
public class AppRoleServiceImpl extends BaseServiceImpl<AppRoleMapper, AppRole> implements AppRoleService {

    @Override
    public List<AppRole> getRolesOfUser(long userId) {
        return List.of();
    }

    @Override
    public AppRole getRoleByCode(String code) {
        return null;
    }

    @Override
    public List<Option<Long>> options(boolean isAdmin) {
        return List.of();
    }

    @Override
    public List<RolePageItemVO> conditionList(AppRole condition, boolean isAdmin) {
        return List.of();
    }

    @Override
    public List<RoleGroupItemVO> groupRoleList(boolean isAdmin, RoleFilterDTO filter) {
        return List.of();
    }

    @Override
    public void createRole(AppRole params, boolean isAdmin) {

    }

    @Override
    public void removeRoleById(long id, boolean isAdmin) {

    }

    @Override
    public void updateRoleById(AppRole params, boolean isAdmin) {

    }

    @Override
    public void sortGroup(List<Long> list) {

    }

    @Override
    public void createGroup(AppRoleGroup params, boolean isAdmin) {

    }

    @Override
    public void updateGroup(AppRoleGroup params, boolean isAdmin) {

    }

    @Override
    public void deleteGroup(long id, boolean isAdmin) {

    }
}
