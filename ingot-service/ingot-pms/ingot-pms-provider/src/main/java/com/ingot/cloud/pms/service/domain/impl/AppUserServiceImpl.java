package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.cloud.pms.api.model.status.PmsErrorCode;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.mapper.AppUserMapper;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
public class AppUserServiceImpl extends BaseServiceImpl<AppUserMapper, AppUser> implements AppUserService {
    private final AppRoleService appRoleService;
    private final AppUserTenantService appUserTenantService;
    private final SysTenantService sysTenantService;
    private final AppRoleUserService appRoleUserService;
    private final AppUserSocialService appUserSocialService;

    private final UserConvert userConvert;
    private final PasswordEncoder passwordEncoder;
    private final AssertionChecker assertionChecker;

    @Override
    public IPage<AppUser> conditionPageWithTenant(Page<AppUser> page, AppUser condition, Long tenantId) {
        return getBaseMapper().conditionPageWithTenant(page, condition, tenantId);
    }

    @Override
    public UserInfoDTO getUserInfo(InUser user) {
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
            result.setUser(userConvert.toUserBaseInfo(userInfo));
            result.setRoles(roleCodes);
            result.setAllows(allows);
            return result;
        });
    }

    @Override
    public void createUser(AppUser user) {
        user.setInitPwd(Boolean.TRUE);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(DateUtils.now());
        if (user.getStatus() == null) {
            user.setStatus(UserStatusEnum.ENABLE);
        }

        checkUserUniqueField(user, null);

        assertionChecker.checkOperation(save(user),
                "SysUserServiceImpl.CreateFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserById(long id) {
        // 取消关联角色
        appRoleUserService.removeByUserId(id);

        // 取消关联社交信息
        appUserSocialService.remove(
                Wrappers.<AppUserSocial>lambdaQuery().eq(AppUserSocial::getUserId, id));

        // 取消关联租户
        appUserTenantService.remove(
                Wrappers.<AppUserTenant>lambdaQuery().eq(AppUserTenant::getUserId, id));

        assertionChecker.checkOperation(removeById(id),
                "SysUserServiceImpl.RemoveFailed");
    }

    @Override
    public void updateUser(AppUser user) {
        AppUser current = getById(user.getId());

        if (StrUtil.isNotEmpty(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            // 如果没有传递init pwd，那么设置为false
            if (user.getInitPwd() == null) {
                user.setInitPwd(Boolean.FALSE);
            }
        }

        checkUserUniqueField(user, current);

        user.setUpdatedAt(DateUtils.now());
        assertionChecker.checkOperation(updateById(user),
                "SysUserServiceImpl.UpdateFailed");
    }

    @Override
    public void fixPassword(long id, UserPasswordDTO params) {
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getPassword())
                        && StrUtil.isNotEmpty(params.getNewPassword()),
                "SysUserServiceImpl.IncorrectPassword");

        AppUser current = getById(id);
        assertionChecker.checkOperation(current != null,
                "SysUserServiceImpl.UserNonExist");
        assert current != null;

        assertionChecker.checkOperation(passwordEncoder.matches(params.getPassword(), current.getPassword()),
                "SysUserServiceImpl.IncorrectPassword");
        AppUser user = new AppUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(params.getNewPassword()));
        user.setInitPwd(false);
        assertionChecker.checkOperation(user.updateById(),
                "SysUserServiceImpl.UpdatePasswordFailed");
    }

    @Override
    public void checkUserUniqueField(AppUser update, AppUser current) {
        // 更新字段不为空，并且不等于当前值
        if (StrUtil.isNotEmpty(update.getUsername())
                && (current == null || !StrUtil.equals(update.getUsername(), current.getUsername()))) {
            assertionChecker.checkBiz(count(Wrappers.<AppUser>lambdaQuery()
                            .eq(AppUser::getUsername, update.getUsername())) == 0,
                    PmsErrorCode.ExistUsername.getCode(),
                    "SysUserServiceImpl.UsernameExist");
        }

        if (StrUtil.isNotEmpty(update.getPhone())
                && (current == null || !StrUtil.equals(update.getPhone(), current.getPhone()))) {
            assertionChecker.checkBiz(count(Wrappers.<AppUser>lambdaQuery()
                            .eq(AppUser::getPhone, update.getPhone())) == 0,
                    PmsErrorCode.ExistPhone.getCode(),
                    "SysUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(update.getEmail())
                && (current == null || !StrUtil.equals(update.getEmail(), current.getEmail()))) {
            assertionChecker.checkBiz(count(Wrappers.<AppUser>lambdaQuery()
                            .eq(AppUser::getEmail, update.getEmail())) == 0,
                    PmsErrorCode.ExistEmail.getCode(),
                    "SysUserServiceImpl.EmailExist");
        }
    }
}
