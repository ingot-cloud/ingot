package com.ingot.cloud.iam.adapter;

import java.time.LocalDateTime;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.api.model.domain.SysUser;
import com.ingot.cloud.iam.identity.AccountCredentialRepository;
import com.ingot.cloud.iam.mapper.SysUserMapper;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.ingot.cloud.iam.persistence.AccountWriteRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>优先把账号安全端口接到新模型，未命中或目标表不可用时回退 SysUser。</p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IamUserAccountPortAdapter implements UserAccountPort {
    private final SysUserMapper sysUserMapper;
    private final AccountCredentialRepository accounts;
    private final AccountWriteRepository accountWrites;

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAccount save(UserAccount account) {
        SysUser sysUser = toEntity(account);
        sysUserMapper.insert(sysUser);
        account.setId(sysUser.getId());
        return account;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<UserAccount> findById(Long userId, UserTypeEnum userType) {
        Optional<UserAccount> modern = modern(() -> accounts.findById(userId).map(this::toModel));
        if (modern.isPresent()) {
            return modern;
        }
        return Optional.ofNullable(sysUserMapper.selectById(userId)).map(this::toModel);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<UserAccount> findByUsername(String username, UserTypeEnum userType) {
        Optional<UserAccount> modern = modern(() -> accounts.findByLogin(username)
                .filter(account -> username.equals(account.username()))
                .map(this::toModel));
        if (modern.isPresent()) {
            return modern;
        }
        LambdaQueryWrapper<SysUser> query = Wrappers.lambdaQuery();
        query.eq(SysUser::getUsername, username);
        return Optional.ofNullable(sysUserMapper.selectOne(query)).map(this::toModel);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<UserAccount> findByPhone(String phone, UserTypeEnum userType) {
        Optional<UserAccount> modern = modern(() -> accounts.findByLogin(phone)
                .filter(account -> phone.equals(account.phone()))
                .map(this::toModel));
        if (modern.isPresent()) {
            return modern;
        }
        LambdaQueryWrapper<SysUser> query = Wrappers.lambdaQuery();
        query.eq(SysUser::getPhone, phone);
        return Optional.ofNullable(sysUserMapper.selectOne(query)).map(this::toModel);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByUsername(String username, UserTypeEnum userType) {
        return findByUsername(username, userType).isPresent();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, UserTypeEnum userType, boolean enabled) {
        if (updateModern(() -> accountWrites.updateStatus(userId, enabled))) {
            return;
        }
        LambdaUpdateWrapper<SysUser> update = Wrappers.lambdaUpdate();
        update.eq(SysUser::getId, userId).set(SysUser::getEnabled, enabled);
        sysUserMapper.update(null, update);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLockStatus(Long userId, UserTypeEnum userType, boolean locked) {
        if (modernAccount(userId)) {
            upsertLock(userId, locked);
            return;
        }
        LambdaUpdateWrapper<SysUser> update = Wrappers.lambdaUpdate();
        update.eq(SysUser::getId, userId).set(SysUser::getLocked, locked);
        sysUserMapper.update(null, update);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLogin(Long userId, UserTypeEnum userType,
                                LocalDateTime loginAt, String loginIp) {
        if (updateModern(() -> accountWrites.updateLastLogin(userId, loginAt))) {
            return;
        }
        LambdaUpdateWrapper<SysUser> update = Wrappers.lambdaUpdate();
        update.eq(SysUser::getId, userId)
              .set(SysUser::getLastLoginAt, loginAt)
              .set(SysUser::getLastLoginIp, loginIp);
        sysUserMapper.update(null, update);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, UserTypeEnum userType) {
        if (updateModern(() -> accountWrites.delete(userId))) {
            return;
        }
        sysUserMapper.deleteById(userId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithVersion(UserAccount account, Long expectedVersion) {
        if (modernAccount(account.getId())) {
            return updateModern(() -> accountWrites.updateWithVersion(account, expectedVersion));
        }
        SysUser sysUser = toEntity(account);
        LambdaUpdateWrapper<SysUser> update = Wrappers.lambdaUpdate();
        update.eq(SysUser::getId, account.getId()).eq(SysUser::getVersion, expectedVersion);
        return sysUserMapper.update(sysUser, update) > 0;
    }

    private boolean modernAccount(Long userId) {
        return modern(() -> accounts.findById(userId)).isPresent();
    }

    private boolean updateModern(java.util.function.IntSupplier update) {
        try {
            return update.getAsInt() > 0;
        } catch (DataAccessException exception) {
            log.debug("新模型账号表不可用，回退旧用户表", exception);
            return false;
        }
    }

    private void upsertLock(Long userId, boolean locked) {
        try {
            accountWrites.upsertLock(userId, locked);
        } catch (DataAccessException exception) {
            log.debug("账号锁定表不可用，跳过新模型锁定写入", exception);
        }
    }

    private <T> Optional<T> modern(java.util.function.Supplier<Optional<T>> query) {
        try {
            return query.get();
        } catch (DataAccessException exception) {
            log.debug("新模型账号查询失败，回退旧用户表", exception);
            return Optional.empty();
        }
    }

    private UserAccount toModel(AccountCredentialRepository.AccountCredentials account) {
        return UserAccount.builder()
                .id(account.id())
                .userType(UserTypeEnum.ADMIN)
                .username(account.username())
                .password(account.passwordHash())
                .nickname(account.username())
                .phone(account.phone())
                .email(account.email())
                .mustChangePwd(account.mustChangePassword())
                .passwordChangedAt(account.passwordChangedAt())
                .enabled(account.enabled())
                .locked(account.locked())
                .lastLoginAt(account.lastLoginAt())
                .version(account.version())
                .createdAt(account.createdAt())
                .updatedAt(account.updatedAt())
                .build();
    }

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
