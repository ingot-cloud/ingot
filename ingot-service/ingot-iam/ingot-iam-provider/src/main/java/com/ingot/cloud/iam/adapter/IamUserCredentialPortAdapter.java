package com.ingot.cloud.iam.adapter;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.api.model.domain.SysUser;
import com.ingot.cloud.iam.mapper.SysUserMapper;
import com.ingot.framework.security.account.domain.port.outbound.UserCredentialPort;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>将凭证端口适配到当前持久化实现，更新受版本条件约束。</p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Component
@RequiredArgsConstructor
public class IamUserCredentialPortAdapter implements UserCredentialPort {

    private final SysUserMapper sysUserMapper;

    /** {@inheritDoc} */
    @Override
    public String getPasswordHash(Long userId, UserTypeEnum userType) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        return sysUser != null ? sysUser.getPassword() : null;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePassword(Long userId, UserTypeEnum userType,
                                  String newPasswordHash, LocalDateTime changedAt,
                                  Long expectedVersion, boolean mustChangePwd) {
        LambdaUpdateWrapper<SysUser> update = Wrappers.lambdaUpdate();
        update.eq(SysUser::getId, userId)
                .eq(SysUser::getVersion, expectedVersion)
                .set(SysUser::getPassword, newPasswordHash)
                .set(SysUser::getPasswordChangedAt, changedAt)
                .set(SysUser::getMustChangePwd, mustChangePwd);

        int updated = sysUserMapper.update(null, update);
        return updated > 0;
    }

    /** {@inheritDoc} */
    @Override
    public LocalDateTime getPasswordChangedAt(Long userId, UserTypeEnum userType) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        return sysUser != null ? sysUser.getPasswordChangedAt() : null;
    }
}
