package com.ingot.cloud.member.service.biz.impl;

import java.util.List;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.member.api.model.convert.MemberUserConvert;
import com.ingot.cloud.member.api.model.domain.*;
import com.ingot.cloud.member.api.model.dto.user.MemberAccountLockDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserBaseInfoDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserCreateByPhoneDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserPasswordDTO;
import com.ingot.cloud.member.api.model.vo.user.MemberUserProfileVO;
import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.cloud.member.service.domain.*;
import com.ingot.framework.account.domain.model.UserAccount;
import com.ingot.framework.account.domain.model.enums.EventSource;
import com.ingot.framework.account.domain.model.enums.LockReason;
import com.ingot.framework.account.domain.port.inbound.ChangePasswordUseCase;
import com.ingot.framework.account.domain.port.inbound.LockAccountUseCase;
import com.ingot.framework.account.domain.port.inbound.ManageAccountStatusUseCase;
import com.ingot.framework.account.domain.port.inbound.RegisterUserUseCase;
import com.ingot.framework.account.domain.port.inbound.UnlockAccountUseCase;
import com.ingot.framework.commons.model.security.ResetPwdVO;
import org.springframework.lang.Nullable;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.commons.utils.UUIDUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizUserServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 13:06.</p>
 */
@Service
@RequiredArgsConstructor
public class BizUserServiceImpl implements BizUserService {
    private final MemberUserService userService;
    private final MemberUserTenantService userTenantService;
    private final MemberUserSocialService userSocialService;

    private final MemberRoleService roleService;
    private final MemberRoleUserService roleUserService;

    private final RegisterUserUseCase registerUserUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final ManageAccountStatusUseCase manageAccountStatusUseCase;
    private final LockAccountUseCase lockAccountUseCase;
    private final UnlockAccountUseCase unlockAccountUseCase;
    private final AssertionChecker assertionChecker;
    private final InitialPasswordService initialPasswordService;

    @Override
    public IPage<MemberUser> conditionPage(Page<MemberUser> page, MemberUserDTO condition) {
        return userService.page(page, Wrappers.<MemberUser>lambdaQuery()
                .like(StrUtil.isNotEmpty(condition.getNickname()), MemberUser::getNickname, condition.getNickname())
                .like(StrUtil.isNotEmpty(condition.getPhone()), MemberUser::getPhone, condition.getPhone()));
    }

    @Override
    public List<MemberRole> getUserRoles(long userId) {
        List<Long> roleIds = roleUserService.getUserRoles(userId)
                .stream().map(MemberRoleUser::getRoleId).toList();
        // 内置角色以及绑定角色
        return roleService.list().stream()
                .filter(item -> BooleanUtil.isTrue(item.getBuiltIn()) || roleIds.stream()
                        .anyMatch(id -> item.getId().equals(id)))
                .toList();
    }

    @Override
    public void setUserRoles(long userId, List<Long> roleIds) {
        List<Long> filterIds = roleService.list(Wrappers.<MemberRole>lambdaQuery()
                        .in(MemberRole::getId, roleIds))
                .stream().filter(role -> BooleanUtil.isFalse(role.getBuiltIn()))
                .map(MemberRole::getId)
                .toList();
        roleUserService.setRoles(userId, filterIds);
    }

    @Override
    public MemberUserProfileVO getUserProfile(long id) {
        MemberUser user = userService.getById(id);
        assertionChecker.checkOperation(user != null,
                "MemberUserServiceImpl.UserNonExist");
        assert user != null;

        MemberUserProfileVO profile = MemberUserConvert.INSTANCE.toProfileVO(user);
        profile.setRoles(getUserRoles(id));
        return profile;
    }

    @Override
    public void updateUserBaseInfo(long id, MemberUserBaseInfoDTO params) {
        MemberUser current = userService.getById(id);
        assertionChecker.checkOperation(current != null,
                "MemberUserServiceImpl.UserNonExist");
        assert current != null;

        MemberUser user = MemberUserConvert.INSTANCE.toEntity(params);
        if (StrUtil.isNotEmpty(user.getPhone())
                && !StrUtil.equals(user.getPhone(), current.getPhone())) {
            assertionChecker.checkOperation(userService.count(Wrappers.<MemberUser>lambdaQuery()
                            .eq(MemberUser::getPhone, user.getPhone())) == 0,
                    "MemberUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(user.getEmail())
                && !StrUtil.equals(user.getEmail(), current.getEmail())) {
            assertionChecker.checkOperation(userService.count(Wrappers.<MemberUser>lambdaQuery()
                            .eq(MemberUser::getEmail, user.getEmail())) == 0,
                    "MemberUserServiceImpl.EmailExist");
        }

        user.setUpdatedAt(DateUtil.now());
        assertionChecker.checkOperation(userService.updateById(user),
                "MemberUserServiceImpl.UpdateFailed");
    }

    @Override
    public MemberUser createIfPhoneNotUsed(MemberUserCreateByPhoneDTO params) {
        MemberUser user = userService.getOne(Wrappers.<MemberUser>lambdaQuery()
                .eq(MemberUser::getPhone, params.getPhone()));
        if (user == null) {
            // 首次以手机号登录 / 注册：走账号域 RegisterUserUseCase，
            // 密码使用短 UUID 占位，强制用户首次登录修改密码。
            UserAccount account = registerUserUseCase.register(RegisterUserUseCase.RegisterUserCommand.builder()
                    .creationSource(RegisterUserUseCase.CreationSource.ADMIN_CREATE)
                    .username(params.getPhone())
                    .password(UUIDUtil.generateShortUuid())
                    .userType(UserTypeEnum.APP)
                    .phone(params.getPhone())
                    .nickname(params.getNickname())
                    .avatar(params.getAvatar())
                    .mustChangePwd(Boolean.TRUE)
                    .eventSource(EventSource.MEMBER)
                    .build());
            user = userService.getById(account.getId());
        }

        // join tenant
        userTenantService.joinTenant(user.getId(), TenantContextHolder.get());
        return user;
    }

    @Override
    public ResetPwdVO createUser(MemberUserDTO params) {
        MemberUser user = MemberUserConvert.INSTANCE.toEntity(params);

        // 默认初始化密码（按初始密码策略生成）
        String initPwd = initialPasswordService.generate();

        user.setUsername(params.getPhone());
        user.setPassword(initPwd);
        userService.create(user);

        ResetPwdVO result = new ResetPwdVO();
        result.setRandom(initPwd);
        result.setId(user.getId());
        return result;
    }

    @Override
    public void updateUser(MemberUserDTO params) {
        MemberUser user = MemberUserConvert.INSTANCE.toEntity(params);
        userService.update(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(long id) {
        // 取消关联角色
        roleUserService.clearByUserId(id);

        // 取消关联社交信息
        userSocialService.remove(
                Wrappers.<MemberUserSocial>lambdaQuery().eq(MemberUserSocial::getUserId, id));

        // 取消关联租户
        userTenantService.remove(
                Wrappers.<MemberUserTenant>lambdaQuery().eq(MemberUserTenant::getUserId, id));

        userService.delete(id);
    }

    @Override
    public ResetPwdVO resetPwd(long userId) {
        InUser operator = SecurityAuthContext.getUser();
        String randomPwd = initialPasswordService.generate();

        // 管理员重置密码：走账号域 ChangePasswordUseCase，自动更新 mustChangePwd/密码历史/过期记录
        changePasswordUseCase.resetPassword(ChangePasswordUseCase.ResetPasswordCommand.builder()
                .userId(userId)
                .userType(UserTypeEnum.APP)
                .newPassword(randomPwd)
                .operatorId(operator.getId())
                .operatorName(operator.getUsername())
                .source(EventSource.MEMBER)
                .build());

        ResetPwdVO result = new ResetPwdVO();
        result.setRandom(randomPwd);
        return result;
    }

    @Override
    public void fixPassword(MemberUserPasswordDTO params) {
        long id = SecurityAuthContext.getUser().getId();
        changePasswordUseCase.changePassword(ChangePasswordUseCase.ChangePasswordCommand.builder()
                .userId(id)
                .userType(UserTypeEnum.APP)
                .oldPassword(params.getPassword())
                .newPassword(params.getNewPassword())
                .confirmPassword(params.getNewPassword())
                .build());
    }

    @Override
    public void enableAccount(long userId, @Nullable String reason) {
        InUser operator = SecurityAuthContext.getUser();
        manageAccountStatusUseCase.enableAccount(ManageAccountStatusUseCase.StatusCommand.builder()
                .userId(userId)
                .userType(UserTypeEnum.APP)
                .targetStatus(Boolean.TRUE)
                .reason(reason)
                .operatorId(operator.getId())
                .operatorName(operator.getUsername())
                .source(EventSource.MEMBER)
                .build());
    }

    @Override
    public void disableAccount(long userId, @Nullable String reason) {
        InUser operator = SecurityAuthContext.getUser();
        manageAccountStatusUseCase.disableAccount(ManageAccountStatusUseCase.StatusCommand.builder()
                .userId(userId)
                .userType(UserTypeEnum.APP)
                .targetStatus(Boolean.FALSE)
                .reason(reason)
                .operatorId(operator.getId())
                .operatorName(operator.getUsername())
                .source(EventSource.MEMBER)
                .build());
    }

    @Override
    public void lockAccount(long userId, MemberAccountLockDTO params) {
        InUser operator = SecurityAuthContext.getUser();
        lockAccountUseCase.lockManually(LockAccountUseCase.LockCommand.builder()
                .userId(userId)
                .userType(UserTypeEnum.APP)
                .reason(LockReason.MANUAL_LOCK)
                .reasonDetail(params.getReasonDetail())
                .lockedUntil(params.getLockedUntil())
                .operatorId(operator.getId())
                .operatorName(operator.getUsername())
                .source(EventSource.MEMBER)
                .build());
    }

    @Override
    public void unlockAccount(long userId, @Nullable String reason) {
        InUser operator = SecurityAuthContext.getUser();
        unlockAccountUseCase.unlockManually(UnlockAccountUseCase.UnlockCommand.builder()
                .userId(userId)
                .userType(UserTypeEnum.APP)
                .reason(reason)
                .operatorId(operator.getId())
                .operatorName(operator.getUsername())
                .source(EventSource.MEMBER)
                .build());
    }
}
