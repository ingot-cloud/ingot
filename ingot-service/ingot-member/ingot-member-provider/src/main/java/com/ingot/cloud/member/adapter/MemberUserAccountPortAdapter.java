package com.ingot.cloud.member.adapter;

import java.time.LocalDateTime;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.mapper.MemberUserMapper;
import com.ingot.framework.account.domain.model.UserAccount;
import com.ingot.framework.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Member 用户账号端口适配器
 *
 * @author jymot
 * @since 2026-02-14
 */
@Component
@RequiredArgsConstructor
public class MemberUserAccountPortAdapter implements UserAccountPort {

    private final MemberUserMapper memberUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAccount save(UserAccount account) {
        MemberUser user = toEntity(account);
        memberUserMapper.insert(user);
        account.setId(user.getId());
        return account;
    }

    @Override
    public Optional<UserAccount> findById(Long userId, UserTypeEnum userType) {
        MemberUser user = memberUserMapper.selectById(userId);
        return Optional.ofNullable(user).map(this::toModel);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username, UserTypeEnum userType) {
        LambdaQueryWrapper<MemberUser> query = Wrappers.lambdaQuery();
        query.eq(MemberUser::getUsername, username);
        MemberUser user = memberUserMapper.selectOne(query);
        return Optional.ofNullable(user).map(this::toModel);
    }

    @Override
    public Optional<UserAccount> findByPhone(String phone, UserTypeEnum userType) {
        LambdaQueryWrapper<MemberUser> query = Wrappers.lambdaQuery();
        query.eq(MemberUser::getPhone, phone);
        MemberUser user = memberUserMapper.selectOne(query);
        return Optional.ofNullable(user).map(this::toModel);
    }

    @Override
    public boolean existsByUsername(String username, UserTypeEnum userType) {
        LambdaQueryWrapper<MemberUser> query = Wrappers.lambdaQuery();
        query.eq(MemberUser::getUsername, username);
        return memberUserMapper.selectCount(query) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, UserTypeEnum userType, boolean enabled) {
        LambdaUpdateWrapper<MemberUser> update = Wrappers.lambdaUpdate();
        update.eq(MemberUser::getId, userId)
                .set(MemberUser::getEnabled, enabled);
        memberUserMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLockStatus(Long userId, UserTypeEnum userType, boolean locked) {
        LambdaUpdateWrapper<MemberUser> update = Wrappers.lambdaUpdate();
        update.eq(MemberUser::getId, userId)
                .set(MemberUser::getLocked, locked);
        memberUserMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLogin(Long userId, UserTypeEnum userType,
                                LocalDateTime loginAt, String loginIp) {
        LambdaUpdateWrapper<MemberUser> update = Wrappers.lambdaUpdate();
        update.eq(MemberUser::getId, userId)
                .set(MemberUser::getLastLoginAt, loginAt)
                .set(MemberUser::getLastLoginIp, loginIp);
        memberUserMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, UserTypeEnum userType) {
        memberUserMapper.deleteById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithVersion(UserAccount account, Long expectedVersion) {
        MemberUser user = toEntity(account);
        LambdaUpdateWrapper<MemberUser> update = Wrappers.lambdaUpdate();
        update.eq(MemberUser::getId, account.getId())
                .eq(MemberUser::getVersion, expectedVersion);
        int updated = memberUserMapper.update(user, update);
        return updated > 0;
    }

    private UserAccount toModel(MemberUser entity) {
        return UserAccount.builder()
                .id(entity.getId())
                .userType(UserTypeEnum.APP)
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

    private MemberUser toEntity(UserAccount model) {
        MemberUser entity = new MemberUser();
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
