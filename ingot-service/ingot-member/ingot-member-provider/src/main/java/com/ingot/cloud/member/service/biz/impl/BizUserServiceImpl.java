package com.ingot.cloud.member.service.biz.impl;

import java.util.List;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.member.api.model.convert.MemberUserConvert;
import com.ingot.cloud.member.api.model.domain.*;
import com.ingot.cloud.member.api.model.dto.user.MemberUserBaseInfoDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserCreateByPhoneDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserPasswordDTO;
import com.ingot.cloud.member.api.model.vo.user.MemberUserProfileVO;
import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.cloud.member.service.domain.*;
import com.ingot.framework.commons.model.enums.UserStatusEnum;
import com.ingot.framework.commons.model.security.ResetPwdVO;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.commons.utils.UUIDUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    private final PasswordEncoder passwordEncoder;
    private final AssertionChecker assertionChecker;

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
            user = new MemberUser();
            user.setUsername(params.getPhone());
            user.setPassword(passwordEncoder.encode(UUIDUtil.generateShortUuid()));
            user.setAvatar(params.getAvatar());
            user.setNickname(params.getNickname());
            user.setPhone(params.getPhone());
            user.setStatus(UserStatusEnum.ENABLE);
            user.setInitPwd(Boolean.TRUE);
            user.setCreatedAt(DateUtil.now());
            userService.save(user);
        }

        // join tenant
        userTenantService.joinTenant(user.getId(), TenantContextHolder.get());
        return user;
    }

    @Override
    public ResetPwdVO createUser(MemberUserDTO params) {
        MemberUser user = MemberUserConvert.INSTANCE.toEntity(params);

        // 默认初始化密码
        String initPwd = randomPwd();

        user.setUsername(params.getPhone());
        user.setInitPwd(Boolean.TRUE);
        user.setPassword(initPwd);
        user.setStatus(UserStatusEnum.ENABLE);
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
        MemberUser user = userService.getById(userId);
        assertionChecker.checkOperation(user != null,
                "MemberUserServiceImpl.UserNonExist");
        assert user != null;

        // 重置密码
        String initPwd = randomPwd();
        user.setPassword(passwordEncoder.encode(initPwd));
        user.updateById();

        ResetPwdVO result = new ResetPwdVO();
        result.setRandom(initPwd);
        return result;
    }

    @Override
    public void fixPassword(MemberUserPasswordDTO params) {
        userService.fixPassword(SecurityAuthContext.getUser().getId(), params);
    }

    private String randomPwd() {
        return RandomUtil.randomString(6);
    }
}
