package com.ingot.cloud.member.service.domain.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.api.model.enums.MemberErrorCode;
import com.ingot.cloud.member.mapper.MemberUserMapper;
import com.ingot.cloud.member.service.domain.MemberUserService;
import com.ingot.framework.account.domain.model.UserAccount;
import com.ingot.framework.account.domain.model.enums.EventSource;
import com.ingot.framework.account.domain.port.inbound.DeleteAccountUseCase;
import com.ingot.framework.account.domain.port.inbound.RegisterUserUseCase;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.RequiredArgsConstructor;
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
    private final AssertionChecker assertionChecker;

    private final RegisterUserUseCase registerUserUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;

    @Override
    public void create(MemberUser user) {
        assertionChecker.checkOperation(StrUtil.isNotEmpty(user.getUsername()),
                "MemberUserServiceImpl.UsernameNonNull");

        checkUserUniqueField(user, null);

        // 管理员/后台创建：跳过密码强度校验，强制用户首次登录修改密码
        UserAccount account = registerUserUseCase.register(RegisterUserUseCase.RegisterUserCommand.builder()
                .creationSource(RegisterUserUseCase.CreationSource.ADMIN_CREATE)
                .username(user.getUsername())
                .password(user.getPassword())
                .userType(UserTypeEnum.APP)
                .phone(user.getPhone())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .mustChangePwd(Boolean.TRUE)
                .createdBy(safeOperatorId())
                .eventSource(EventSource.MEMBER)
                .build());

        user.setId(account.getId());
    }

    @Override
    public void update(MemberUser user) {
        MemberUser current = getById(user.getId());

        // 密码更新统一走 ChangePasswordUseCase，禁止在通用 update 中设置密码
        user.setPassword(null);

        checkUserUniqueField(user, current);

        user.setUpdatedAt(DateUtil.now());
        assertionChecker.checkOperation(updateById(user),
                "MemberUserServiceImpl.UpdateFailed");
    }

    @Override
    public void delete(long id) {
        InUser operator = safeOperator();
        deleteAccountUseCase.deleteAccount(DeleteAccountUseCase.DeleteAccountCommand.builder()
                .userId(id)
                .userType(UserTypeEnum.APP)
                .source(EventSource.MEMBER)
                .operatorId(operator == null ? null : operator.getId())
                .operatorName(operator == null ? null : operator.getUsername())
                .build());
    }

    private void checkUserUniqueField(MemberUser update, MemberUser current) {
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

    /**
     * 部分 C 端场景（比如 H5 自助注册）上下文中不存在登录态 InUser，这里做降级处理。
     */
    private InUser safeOperator() {
        try {
            return SecurityAuthContext.getUser();
        } catch (Exception e) {
            return null;
        }
    }

    private Long safeOperatorId() {
        InUser operator = safeOperator();
        return operator == null ? null : operator.getId();
    }
}
