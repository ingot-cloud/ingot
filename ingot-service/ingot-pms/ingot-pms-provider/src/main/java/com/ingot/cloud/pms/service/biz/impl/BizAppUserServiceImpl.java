package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.biz.UserOrgEditDTO;
import com.ingot.cloud.pms.api.model.dto.user.AppUserCreateDTO;
import com.ingot.cloud.pms.api.model.dto.user.OrgUserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.cloud.pms.api.model.transform.UserTrans;
import com.ingot.cloud.pms.api.model.vo.biz.ResetPwdVO;
import com.ingot.cloud.pms.api.model.vo.biz.UserOrgInfoVO;
import com.ingot.cloud.pms.api.model.vo.user.OrgUserProfileVO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;
import com.ingot.cloud.pms.service.biz.BizAppRoleService;
import com.ingot.cloud.pms.service.biz.BizAppUserService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.UUIDUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.common.constants.RoleConstants;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.tenant.TenantContextHolder;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final BizAppRoleService bizAppRoleService;
    private final SysTenantService sysTenantService;

    private final AssertionChecker assertionChecker;
    private final PasswordEncoder passwordEncoder;
    private final UserTrans userTrans;

    @Override
    public IPage<AppUser> page(Page<AppUser> page, AppUser filter) {
        return appUserService.page(page, Wrappers.lambdaQuery(filter));
    }

    @Override
    public IPage<AppUser> pageTenant(Page<AppUser> page, AppUser filter) {
        return appUserService.conditionPageWithTenant(page, filter, TenantContextHolder.get());
    }

    @Override
    public UserProfileVO getUserProfile(long id) {
        AppUser user = appUserService.getById(id);
        assertionChecker.checkOperation(user != null,
                "SysUserServiceImpl.UserNonExist");
        assert user != null;

        UserProfileVO profile = userTrans.toUserProfile(user);
        List<AppUserTenant> userTenantList = appUserTenantService.getUserOrgs(user.getId());
        profile.setOrgList(userTenantList);

        return profile;
    }

    @Override
    public void updateUserBaseInfo(long id, UserBaseInfoDTO params) {
        AppUser current = appUserService.getById(id);
        assertionChecker.checkOperation(current != null,
                "SysUserServiceImpl.UserNonExist");
        assert current != null;

        AppUser user = userTrans.toAppUser(params);
        if (StrUtil.isNotEmpty(user.getPhone())
                && !StrUtil.equals(user.getPhone(), current.getPhone())) {
            assertionChecker.checkOperation(appUserService.count(Wrappers.<AppUser>lambdaQuery()
                            .eq(AppUser::getPhone, user.getPhone())) == 0,
                    "SysUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(user.getEmail())
                && !StrUtil.equals(user.getEmail(), current.getEmail())) {
            assertionChecker.checkOperation(appUserService.count(Wrappers.<AppUser>lambdaQuery()
                            .eq(AppUser::getEmail, user.getEmail())) == 0,
                    "SysUserServiceImpl.EmailExist");
        }

        user.setUpdatedAt(DateUtils.now());
        assertionChecker.checkOperation(appUserService.updateById(user),
                "SysUserServiceImpl.UpdateFailed");
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
    public ResetPwdVO createUser(AppUserCreateDTO params) {
        AppUser user = userTrans.to(params);

        // 默认初始化密码
        String initPwd = RandomUtil.randomString(6);

        user.setInitPwd(Boolean.TRUE);
        user.setPassword(initPwd);
        user.setStatus(UserStatusEnum.ENABLE);
        appUserService.createUser(user);

        ResetPwdVO result = new ResetPwdVO();
        result.setRandom(initPwd);
        return result;
    }

    @Override
    public void updateUser(AppUser params) {
        AppUser current = appUserService.getById(params.getId());

        if (StrUtil.isNotEmpty(params.getPassword())) {
            params.setPassword(passwordEncoder.encode(params.getPassword()));
            params.setInitPwd(false);
        }

        appUserService.checkUserUniqueField(params, current);

        params.setUpdatedAt(DateUtils.now());
        assertionChecker.checkOperation(appUserService.updateById(params),
                "SysUserServiceImpl.UpdateFailed");
    }

    @Override
    public void deleteUser(long id) {
        appUserService.removeUserById(id);
    }

    @Override
    public ResetPwdVO resetPwd(long userId) {
        AppUser user = appUserService.getById(userId);
        assertionChecker.checkOperation(user != null,
                "SysUserServiceImpl.UserNonExist");
        assert user != null;

        // 重置密码
        String initPwd = RandomUtil.randomString(6);
        user.setPassword(passwordEncoder.encode(initPwd));
        user.updateById();

        ResetPwdVO result = new ResetPwdVO();
        result.setRandom(initPwd);
        return result;
    }

    @Override
    public void userOrgEdit(UserOrgEditDTO params) {
        TenantEnv.runAs(params.getOrgId(), () -> {
            long userId = params.getId();
            SysTenant tenant = sysTenantService.getById(params.getOrgId());

            appUserTenantService.joinTenant(userId, tenant);
            bizAppRoleService.setOrgUserRoles(userId, params.getRoleIds());
        });
    }

    @Override
    public void userOrgLeave(UserOrgEditDTO params) {
        TenantEnv.runAs(params.getOrgId(), () -> {
            long userId = params.getId();
            appUserTenantService.leaveTenant(userId);
            // 取消关联角色
            appRoleUserService.remove(Wrappers.<AppRoleUser>lambdaQuery()
                    .eq(AppRoleUser::getUserId, userId));
        });
    }

    @Override
    public List<UserOrgInfoVO> userOrgInfo(long userId) {
        List<AppUserTenant> list = CollUtil.emptyIfNull(appUserTenantService.getUserOrgs(userId));
        return list.stream().map(org ->
                TenantEnv.applyAs(org.getTenantId(), () -> {
                    UserOrgInfoVO item = new UserOrgInfoVO();
                    item.setOrgId(org.getTenantId());

                    List<Long> roleIds = CollUtil.emptyIfNull(appRoleService.getRolesOfUser(userId))
                            .stream().map(AppRole::getId).toList();
                    item.setRoleIds(roleIds);
                    return item;
                })).toList();
    }

    @Override
    public OrgUserProfileVO getOrgUserProfile(long id) {
        AppUser user = appUserService.getById(id);
        assertionChecker.checkOperation(user != null,
                "SysUserServiceImpl.UserNonExist");
        assert user != null;

        OrgUserProfileVO profile = userTrans.toOrgUserProfile(user);
        return profile;
    }

    @Override
    public void orgCreateUser(OrgUserDTO params) {
        // 手机号，部门，昵称不能为空
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getPhone()), "SysUser.phone");
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getNickname()), "SysUser.nickname");
        assertionChecker.checkOperation(CollUtil.isNotEmpty(params.getDeptIds()), "SysUser.deptId");

        // 如果已经存在注册用户，那么直接关联新组织信息
        AppUser user = appUserService.getOne(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getPhone, params.getPhone()));
        if (user == null) {
            // 密码默认为手机号
            user = userTrans.toAppUser(params);
            user.setUsername(params.getPhone());
            user.setPassword(params.getPhone());
            user.setInitPwd(Boolean.TRUE);
            appUserService.createUser(user);
        }

        // 加入租户
        appUserTenantService.joinTenant(user.getId(), sysTenantService.getById(TenantContextHolder.get()));
    }

    @Override
    public void orgUpdateUser(OrgUserDTO params) {
        assertionChecker.checkOperation(params.getId() != null, "Common.IDNonNull");

        AppUser user = userTrans.toAppUser(params);
        // 更新用户
        appUserService.updateUser(user);
    }

    @Override
    public void orgDeleteUser(long id) {
        long userId = SecurityAuthContext.getUser().getId();
        assertionChecker.checkOperation(userId != id, "BizUserServiceImpl.RemoveSelfFailed");

        // 取消关联组织
        appUserTenantService.leaveTenant(id);
        // 取消关联角色
        appRoleUserService.remove(Wrappers.<AppRoleUser>lambdaQuery()
                .eq(AppRoleUser::getUserId, id));
    }

    @Override
    public void orgPasswordInit(UserPasswordDTO params) {
        long id = SecurityAuthContext.getUser().getId();
        AppUser current = appUserService.getById(id);
        assertionChecker.checkOperation(current != null,
                "SysUserServiceImpl.UserNonExist");
        if (!BooleanUtil.isTrue(current.getInitPwd())) {
            return;
        }

        AppUser user = new AppUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(params.getNewPassword()));
        user.setInitPwd(false);
        assertionChecker.checkOperation(user.updateById(),
                "SysUserServiceImpl.UpdatePasswordFailed");
    }

    @Override
    public void fixPassword(UserPasswordDTO params) {
        appUserService.fixPassword(SecurityAuthContext.getUser().getId(), params);
    }

}
