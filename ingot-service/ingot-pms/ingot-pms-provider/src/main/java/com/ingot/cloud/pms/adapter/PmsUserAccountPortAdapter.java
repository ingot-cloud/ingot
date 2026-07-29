package com.ingot.cloud.pms.adapter;

import java.time.LocalDateTime;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.mapper.SysUserMapper;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PMS 用户账号端口适配器
 *
 * @author jymot
 * @since 2026-02-13
 */
@Component
@RequiredArgsConstructor
public class PmsUserAccountPortAdapter implements UserAccountPort {

    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAccount save(UserAccount account) {
        SysUser sysUser = toEntity(account);
        sysUserMapper.insert(sysUser);
        account.setId(sysUser.getId());
        return account;
    }

    @Override
    public Optional<UserAccount> findById(Long userId, UserTypeEnum userType) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        return Optional.ofNullable(sysUser).map(this::toModel);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username, UserTypeEnum userType) {
        LambdaQueryWrapper<SysUser> query = Wrappers.lambdaQuery();
        query.eq(SysUser::getUsername, username);
        SysUser sysUser = sysUserMapper.selectOne(query);
        return Optional.ofNullable(sysUser).map(this::toModel);
    }

    @Override
    public Optional<UserAccount> findByPhone(String phone, UserTypeEnum userType) {
        LambdaQueryWrapper<SysUser> query = Wrappers.lambdaQuery();
        query.eq(SysUser::getPhone, phone);
        SysUser sysUser = sysUserMapper.selectOne(query);
        return Optional.ofNullable(sysUser).map(this::toModel);
    }

    @Override
    public boolean existsByUsername(String username, UserTypeEnum userType) {
        LambdaQueryWrapper<SysUser> query = Wrappers.lambdaQuery();
        query.eq(SysUser::getUsername, username);
        return sysUserMapper.selectCount(query) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, UserTypeEnum userType, boolean enabled) {
        LambdaUpdateWrapper<SysUser> update = Wrappers.lambdaUpdate();
        update.eq(SysUser::getId, userId)
              .set(SysUser::getEnabled, enabled);
        sysUserMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLockStatus(Long userId, UserTypeEnum userType, boolean locked) {
        LambdaUpdateWrapper<SysUser> update = Wrappers.lambdaUpdate();
        update.eq(SysUser::getId, userId)
              .set(SysUser::getLocked, locked);
        sysUserMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLogin(Long userId, UserTypeEnum userType, 
                                LocalDateTime loginAt, String loginIp) {
        LambdaUpdateWrapper<SysUser> update = Wrappers.lambdaUpdate();
        update.eq(SysUser::getId, userId)
              .set(SysUser::getLastLoginAt, loginAt)
              .set(SysUser::getLastLoginIp, loginIp);
        sysUserMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, UserTypeEnum userType) {
        sysUserMapper.deleteById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithVersion(UserAccount account, Long expectedVersion) {
        SysUser sysUser = toEntity(account);
        LambdaUpdateWrapper<SysUser> update = Wrappers.lambdaUpdate();
        update.eq(SysUser::getId, account.getId())
              .eq(SysUser::getVersion, expectedVersion);
        int updated = sysUserMapper.update(sysUser, update);
        return updated > 0;
    }

    // ========== 转换方法 ==========

    private UserAccount toModel(SysUser entity) {
        return UserAccount.builder()
                .id(entity.getId())
                .userType(UserTypeEnum.ADMIN)
                .username(entity.getUsername())
                .password(entity.getPassword())
                .nickname(entity.getNickname())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .avatar(entity.getAvatar())
                .mustChangePwd(entity.getMustChangePwd())
                .passwordChangedAt(entity.getPasswordChangedAt())
                .enabled(entity.getEnabled())
                .locked(entity.getLocked())
                .lastLoginAt(entity.getLastLoginAt())
                .lastLoginIp(entity.getLastLoginIp())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private SysUser toEntity(UserAccount model) {
        SysUser entity = new SysUser();
        entity.setId(model.getId());
        entity.setUsername(model.getUsername());
        entity.setPassword(model.getPassword());
        entity.setNickname(model.getNickname());
        entity.setPhone(model.getPhone());
        entity.setEmail(model.getEmail());
        entity.setAvatar(model.getAvatar());
        entity.setMustChangePwd(model.getMustChangePwd());
        entity.setPasswordChangedAt(model.getPasswordChangedAt());
        entity.setEnabled(model.getEnabled());
        entity.setLocked(model.getLocked());
        entity.setLastLoginAt(model.getLastLoginAt());
        entity.setLastLoginIp(model.getLastLoginIp());
        entity.setVersion(model.getVersion());
        entity.setCreatedAt(model.getCreatedAt());
        entity.setUpdatedAt(model.getUpdatedAt());
        entity.setDeletedAt(model.getDeletedAt());
        return entity;
    }
}
