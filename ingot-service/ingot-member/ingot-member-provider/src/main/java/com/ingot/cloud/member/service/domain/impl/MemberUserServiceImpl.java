package com.ingot.cloud.member.service.domain.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.api.model.dto.user.MemberUserPasswordDTO;
import com.ingot.cloud.member.api.model.enums.MemberErrorCode;
import com.ingot.cloud.member.mapper.MemberUserMapper;
import com.ingot.cloud.member.service.domain.MemberUserService;
import com.ingot.framework.commons.model.enums.UserStatusEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
@Service
@RequiredArgsConstructor
public class MemberUserServiceImpl extends BaseServiceImpl<MemberUserMapper, MemberUser> implements MemberUserService {
    private final PasswordEncoder passwordEncoder;
    private final AssertionChecker assertionChecker;

    @Override
    public void create(MemberUser user) {
        user.setInitPwd(Boolean.TRUE);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(DateUtil.now());
        if (user.getStatus() == null) {
            user.setStatus(UserStatusEnum.ENABLE);
        }

        checkUserUniqueField(user, null);

        assertionChecker.checkOperation(save(user),
                "MemberUserServiceImpl.CreateFailed");
    }

    @Override
    public void update(MemberUser user) {
        MemberUser current = getById(user.getId());

        if (StrUtil.isNotEmpty(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            // 如果没有传递init pwd，那么设置为false
            if (user.getInitPwd() == null) {
                user.setInitPwd(Boolean.FALSE);
            }
        }

        checkUserUniqueField(user, current);

        user.setUpdatedAt(DateUtil.now());
        assertionChecker.checkOperation(updateById(user),
                "MemberUserServiceImpl.UpdateFailed");
    }

    @Override
    public void delete(long id) {
        assertionChecker.checkOperation(removeById(id),
                "MemberUserServiceImpl.RemoveFailed");
    }

    private void checkUserUniqueField(MemberUser update, MemberUser current) {
        // 更新字段不为空，并且不等于当前值
        if (StrUtil.isNotEmpty(update.getUsername())
                && (current == null || !StrUtil.equals(update.getUsername(), current.getUsername()))) {
            assertionChecker.checkBiz(count(Wrappers.<MemberUser>lambdaQuery()
                            .eq(MemberUser::getUsername, update.getUsername())) == 0,
                    MemberErrorCode.ExistUsername.getCode(),
                    "MemberUserServiceImpl.UsernameExist");
        }

        if (StrUtil.isNotEmpty(update.getPhone())
                && (current == null || !StrUtil.equals(update.getPhone(), current.getPhone()))) {
            assertionChecker.checkBiz(count(Wrappers.<MemberUser>lambdaQuery()
                            .eq(MemberUser::getPhone, update.getPhone())) == 0,
                    MemberErrorCode.ExistPhone.getCode(),
                    "MemberUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(update.getEmail())
                && (current == null || !StrUtil.equals(update.getEmail(), current.getEmail()))) {
            assertionChecker.checkBiz(count(Wrappers.<MemberUser>lambdaQuery()
                            .eq(MemberUser::getEmail, update.getEmail())) == 0,
                    MemberErrorCode.ExistEmail.getCode(),
                    "MemberUserServiceImpl.EmailExist");
        }
    }

    @Override
    public void fixPassword(long id, MemberUserPasswordDTO params) {
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getPassword())
                        && StrUtil.isNotEmpty(params.getNewPassword()),
                "MemberUserServiceImpl.IncorrectPassword");

        MemberUser current = getById(id);
        assertionChecker.checkOperation(current != null,
                "MemberUserServiceImpl.UserNonExist");
        assert current != null;

        assertionChecker.checkOperation(passwordEncoder.matches(params.getPassword(), current.getPassword()),
                "MemberUserServiceImpl.IncorrectPassword");
        MemberUser user = new MemberUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(params.getNewPassword()));
        user.setInitPwd(false);
        assertionChecker.checkOperation(user.updateById(),
                "MemberUserServiceImpl.UpdatePasswordFailed");
    }
}
