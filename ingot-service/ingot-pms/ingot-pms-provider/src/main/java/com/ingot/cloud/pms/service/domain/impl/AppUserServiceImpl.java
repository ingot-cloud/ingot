package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.api.model.domain.AppUserTenant;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDTO;
import com.ingot.cloud.pms.api.model.transform.UserTrans;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.mapper.AppUserMapper;
import com.ingot.cloud.pms.service.domain.AppRoleService;
import com.ingot.cloud.pms.service.domain.AppUserService;
import com.ingot.cloud.pms.service.domain.AppUserTenantService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl extends BaseServiceImpl<AppUserMapper, AppUser> implements AppUserService {
    private final AppRoleService appRoleService;
    private final AppUserTenantService appUserTenantService;
    private final SysTenantService sysTenantService;
    private final UserTrans userTrans;

    @Override
    public IPage<AppUser> conditionPageWithTenant(Page<AppUser> page, AppUser condition, Long tenantId) {
        return getBaseMapper().conditionPageWithTenant(page, condition, tenantId);
    }

    @Override
    public UserInfoDTO getUserInfo(IngotUser user) {
        // 使用当前用户 tenant 进行操作
        return TenantEnv.applyAs(user.getTenantId(), () -> {
            AppUser userInfo = getById(user.getId());
            if (userInfo == null) {
                OAuth2ErrorUtils.throwInvalidRequest("用户异常");
            }

            List<AppRole> roles = appRoleService.getRolesOfUser(user.getId());
            List<String> roleCodes = roles.stream()
                    .map(AppRole::getCode).collect(Collectors.toList());

            // 获取可以访问的租户列表
            List<AppUserTenant> userTenantList = appUserTenantService.list(
                    Wrappers.<AppUserTenant>lambdaQuery()
                            .eq(AppUserTenant::getUserId, user.getId()));
            List<AllowTenantDTO> allows = BizUtils.getAllows(sysTenantService,
                    userTenantList.stream()
                            .map(AppUserTenant::getTenantId).collect(Collectors.toSet()),
                    (item) -> {
                        // main=true，为当前登录的租户
                        item.setMain(Long.parseLong(item.getId()) == user.getTenantId());
                    });

            UserInfoDTO result = new UserInfoDTO();
            result.setUser(userTrans.toUserBaseInfo(userInfo));
            result.setRoles(roleCodes);
            result.setAllows(allows);
            return result;
        });
    }
}
