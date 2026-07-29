package com.ingot.cloud.pms.adapter;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.mapper.SysUserMapper;
import com.ingot.framework.security.account.domain.port.outbound.UserCredentialPort;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PMS 用户凭证端口适配器
 *
 * @author jymot
 * @since 2026-02-13
 */
@Component
@RequiredArgsConstructor
public class PmsUserCredentialPortAdapter implements UserCredentialPort {

    private final SysUserMapper sysUserMapper;

    @Override
    public String getPasswordHash(Long userId, UserTypeEnum userType) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        return sysUser != null ? sysUser.getPassword() : null;
    }

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

    @Override
    public LocalDateTime getPasswordChangedAt(Long userId, UserTypeEnum userType) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        return sysUser != null ? sysUser.getPasswordChangedAt() : null;
    }
}
