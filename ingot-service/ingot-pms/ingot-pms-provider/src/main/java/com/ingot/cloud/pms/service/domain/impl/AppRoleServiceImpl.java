package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.AppRoleUser;
import com.ingot.cloud.pms.mapper.AppRoleMapper;
import com.ingot.cloud.pms.service.domain.AppRoleService;
import com.ingot.cloud.pms.service.domain.AppRoleUserService;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final AppRoleUserService appRoleUserService;

    @Override
    public List<AppRole> getAllRolesOfUser(long userId, long deptId) {
        // 基础角色ID
        Set<Long> baseRoleIds = appRoleUserService.list(Wrappers.<AppRoleUser>lambdaQuery()
                        .eq(AppRoleUser::getUserId, userId))
                .stream().map(AppRoleUser::getRoleId).collect(Collectors.toSet());

        return list(Wrappers.<AppRole>lambdaQuery()
                .eq(AppRole::getStatus, CommonStatusEnum.ENABLE)
                .in(AppRole::getId, baseRoleIds));
    }
}
