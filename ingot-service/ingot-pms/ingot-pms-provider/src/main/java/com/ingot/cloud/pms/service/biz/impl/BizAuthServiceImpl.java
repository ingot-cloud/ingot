package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDTO;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.core.BizPermissionUtils;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizAuthService;
import com.ingot.cloud.pms.service.biz.BizMetaMenuService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : BizAuthServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 09:13.</p>
 */
@Service
@RequiredArgsConstructor
public class BizAuthServiceImpl implements BizAuthService {
    private final SysUserService userService;
    private final SysTenantService tenantService;
    private final SysUserTenantService userTenantService;

    private final BizRoleService bizRoleService;
    private final BizAppService bizAppService;
    private final BizMetaMenuService bizMetaMenuService;

    private final UserConvert userConvert;

    @Override
    public UserInfoDTO getUserInfo(InUser user) {
        // 使用当前用户 tenant 进行操作
        return TenantEnv.applyAs(user.getTenantId(), () -> {
            Long userId = user.getId();

            SysUser userInfo = userService.getById(userId);
            if (userInfo == null) {
                OAuth2ErrorUtils.throwInvalidRequest("用户异常");
            }

            // 获取可以访问的租户列表
            List<SysUserTenant> userTenantList = userTenantService.getUserOrgs(userId);
            List<TenantMainDTO> allows = BizUtils.getTenants(tenantService,
                    userTenantList.stream()
                            .map(SysUserTenant::getTenantId).collect(Collectors.toSet()),
                    (item) -> {
                        // main=true，为当前登录的租户
                        item.setMain(Long.parseLong(item.getId()) == user.getTenantId());
                    });

            UserInfoDTO result = new UserInfoDTO();
            result.setUser(userConvert.toUserBaseInfo(userInfo));
            result.setRoles(user.getRoleCodeList());
            result.setAllows(allows);
            return result;
        });
    }

    @Override
    public List<MenuTreeNodeVO> getUserMenus(InUser user) {
        List<String> roleCodeList = user.getRoleCodeList();
        List<RoleType> roles = bizRoleService.getRolesByCodes(roleCodeList);
        List<PermissionType> authorities = bizRoleService.getRolesPermissionsAndChildren(roles);

        // 过滤禁用App
        List<PermissionType> finallyAuthorities = BizPermissionUtils.filterOrgLockAuthority(
                authorities, bizAppService);
        return bizMetaMenuService.getMenuByPermissions(finallyAuthorities);
    }
}
