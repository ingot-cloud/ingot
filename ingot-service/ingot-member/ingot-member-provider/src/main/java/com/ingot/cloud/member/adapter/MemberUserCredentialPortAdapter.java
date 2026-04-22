package com.ingot.cloud.member.adapter;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.mapper.MemberUserMapper;
import com.ingot.framework.account.domain.port.outbound.UserCredentialPort;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Member 用户凭证端口适配器
 *
 * @author jymot
 * @since 2026-02-14
 */
@Component
@RequiredArgsConstructor
public class MemberUserCredentialPortAdapter implements UserCredentialPort {

    private final MemberUserMapper memberUserMapper;

    @Override
    public String getPasswordHash(Long userId, UserTypeEnum userType) {
        MemberUser user = memberUserMapper.selectById(userId);
        return user != null ? user.getPassword() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePassword(Long userId, UserTypeEnum userType,
                                  String newPasswordHash, LocalDateTime changedAt,
                                  Long expectedVersion, boolean mustChangePwd) {
        LambdaUpdateWrapper<MemberUser> update = Wrappers.lambdaUpdate();
        update.eq(MemberUser::getId, userId)
                .eq(MemberUser::getVersion, expectedVersion)
                .set(MemberUser::getPassword, newPasswordHash)
                .set(MemberUser::getPasswordChangedAt, changedAt)
                .set(MemberUser::getMustChangePwd, mustChangePwd);

        int updated = memberUserMapper.update(null, update);
        return updated > 0;
    }

    @Override
    public LocalDateTime getPasswordChangedAt(Long userId, UserTypeEnum userType) {
        MemberUser user = memberUserMapper.selectById(userId);
        return user != null ? user.getPasswordChangedAt() : null;
    }
}
