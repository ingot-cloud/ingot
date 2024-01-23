package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.AppRoleUser;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.dto.user.AppUserCreateDTO;
import com.ingot.cloud.pms.api.model.status.PmsErrorCode;
import com.ingot.cloud.pms.service.biz.BizAppUserService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.UUIDUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.common.constants.RoleConstants;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizAppUserServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/17.</p>
 * <p>Time         : 14:22.</p>
 */
@Service
@RequiredArgsConstructor
public class BizAppUserServiceImpl implements BizAppUserService {
    private final AppUserService appUserService;
    private final AppRoleService appRoleService;
    private final AppRoleUserService appRoleUserService;
    private final AppUserTenantService appUserTenantService;
    private final SysTenantService sysTenantService;

    private final AssertionChecker assertI18nService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public IPage<AppUser> page(Page<AppUser> page, AppUser filter) {
        return appUserService.page(page, Wrappers.lambdaQuery(filter));
    }

    @Override
    public IPage<AppUser> pageTenant(Page<AppUser> page, AppUser filter) {
        return appUserService.conditionPageWithTenant(page, filter, TenantContextHolder.get());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppUser createIfPhoneNotUsed(AppUserCreateDTO params) {
        AppUser user = appUserService.getOne(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getPhone, params.getPhone()));
        if (user == null) {
            user = new AppUser();
            user.setUsername(params.getPhone());
            user.setPassword(passwordEncoder.encode(UUIDUtils.generateShortUuid()));
            user.setAvatar(params.getAvatar());
            user.setNickname(params.getNickname());
            user.setPhone(params.getPhone());
            user.setStatus(UserStatusEnum.ENABLE);
            user.setInitPwd(Boolean.TRUE);
            user.setCreatedAt(DateUtils.now());
            appUserService.save(user);
        }

        // bind role
        AppRole role = appRoleService.getRoleByCode(RoleConstants.ROLE_USER_CODE);
        long roleCount = appRoleUserService.count(Wrappers.<AppRoleUser>lambdaQuery()
                .eq(AppRoleUser::getUserId, user.getId())
                .eq(AppRoleUser::getRoleId, role.getId()));
        if (roleCount == 0) {
            AppRoleUser roleUser = new AppRoleUser();
            roleUser.setRoleId(role.getId());
            roleUser.setUserId(user.getId());
            appRoleUserService.save(roleUser);
        }

        // join tenant
        SysTenant tenant = sysTenantService.getById(TenantContextHolder.get());
        appUserTenantService.joinTenant(user.getId(), tenant);
        return user;
    }

    @Override
    public void updateUser(AppUser params) {
        AppUser current = appUserService.getById(params.getId());

        if (StrUtil.isNotEmpty(params.getPassword())) {
            params.setPassword(passwordEncoder.encode(params.getPassword()));
            params.setInitPwd(false);
        }

        checkUserUniqueField(params, current);

        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(appUserService.updateById(params),
                "SysUserServiceImpl.UpdateFailed");
    }

    private void checkUserUniqueField(AppUser update, AppUser current) {
        // 更新字段不为空，并且不等于当前值
        if (StrUtil.isNotEmpty(update.getUsername())
                && (current == null || !StrUtil.equals(update.getUsername(), current.getUsername()))) {
            assertI18nService.checkBiz(appUserService.count(Wrappers.<AppUser>lambdaQuery()
                            .eq(AppUser::getUsername, update.getUsername())) == 0,
                    PmsErrorCode.ExistUsername.getCode(),
                    "SysUserServiceImpl.UsernameExist");
        }

        if (StrUtil.isNotEmpty(update.getPhone())
                && (current == null || !StrUtil.equals(update.getPhone(), current.getPhone()))) {
            assertI18nService.checkBiz(appUserService.count(Wrappers.<AppUser>lambdaQuery()
                            .eq(AppUser::getPhone, update.getPhone())) == 0,
                    PmsErrorCode.ExistPhone.getCode(),
                    "SysUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(update.getEmail())
                && (current == null || !StrUtil.equals(update.getEmail(), current.getEmail()))) {
            assertI18nService.checkBiz(appUserService.count(Wrappers.<AppUser>lambdaQuery()
                            .eq(AppUser::getEmail, update.getEmail())) == 0,
                    PmsErrorCode.ExistEmail.getCode(),
                    "SysUserServiceImpl.EmailExist");
        }
    }
}
