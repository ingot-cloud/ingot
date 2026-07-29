package com.ingot.framework.security.account.adapter.port;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.framework.security.account.adapter.entity.AccountLockStateEntity;
import com.ingot.framework.security.account.adapter.mapper.AccountLockStateMapper;
import com.ingot.framework.security.account.domain.model.LockState;
import com.ingot.framework.security.account.domain.model.enums.LockType;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 锁定状态端口默认实现（基于 account_lock_state 表）
 * <p>
 * 由 {@code AccountAdapterAutoConfiguration} 以 {@code @ConditionalOnMissingBean} 方式注册，
 * 不使用 {@code @Component}，避免与业务服务中的自定义实现产生冲突。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultLockStatePortAdapter implements LockStatePort {

    private final AccountLockStateMapper lockStateMapper;

    @Override
    public Optional<LockState> findByUser(Long userId, UserTypeEnum userType) {
        AccountLockStateEntity entity = lockStateMapper.selectOne(
                buildUserQuery(userId, userType));
        return Optional.ofNullable(entity).map(this::toModel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LockState initialize(Long userId, UserTypeEnum userType) {
        AccountLockStateEntity entity = new AccountLockStateEntity();
        entity.setUserId(userId);
        entity.setUserType(userType != null ? userType.getValue() : null);
        entity.setLocked(false);
        entity.setFailedLoginCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        lockStateMapper.insert(entity);
        return toModel(entity);
    }

    /**
     * 原子递增失败计数并返回递增后的新值。
     * <p>
     * 使用 {@code INSERT ... ON DUPLICATE KEY UPDATE} 在 DB 层原子完成 upsert，
     * 避免并发场景下多个请求同时读到旧值、各自加 1、写回同一结果的竞态问题。
     * 事务内在 upsert 后立即读取，MySQL REPEATABLE READ 保证能读到本事务刚写入的值。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int incrementFailCount(Long userId, UserTypeEnum userType) {
        String userTypeValue = userType != null ? userType.getValue() : null;
        lockStateMapper.upsertIncrementFailCount(userId, userTypeValue, LocalDateTime.now());

        AccountLockStateEntity entity = lockStateMapper.selectOne(buildUserQuery(userId, userType));
        return entity != null ? entity.getFailedLoginCount() : 1;
    }

    /**
     * 直接 UPDATE 将失败计数清零，无需先 SELECT。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetFailCount(Long userId, UserTypeEnum userType) {
        String userTypeValue = userType != null ? userType.getValue() : null;
        lockStateMapper.resetFailCountDirect(userId, userTypeValue, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLockStatus(Long userId, UserTypeEnum userType,
                                  boolean locked, LocalDateTime lockedUntil,
                                  String reasonCode, LockType lockType,
                                  Long operatorId, String operatorName) {
        AccountLockStateEntity entity = lockStateMapper.selectOne(
                buildUserQuery(userId, userType));
        if (entity == null) {
            entity = new AccountLockStateEntity();
            entity.setUserId(userId);
            entity.setUserType(userType != null ? userType.getValue() : null);
        }

        entity.setLocked(locked);
        entity.setLockType(lockType != null ? lockType.getCode() : null);
        entity.setLockReasonCode(reasonCode);
        entity.setLockedAt(locked ? LocalDateTime.now() : null);
        entity.setLockedUntil(lockedUntil);
        entity.setOperatorId(operatorId);
        entity.setOperatorName(operatorName);

        if (entity.getId() == null) {
            lockStateMapper.insert(entity);
        } else {
            lockStateMapper.updateById(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUser(Long userId, UserTypeEnum userType) {
        lockStateMapper.delete(buildUserQuery(userId, userType));
    }

    @Override
    public List<LockState> findExpiredLocksByPage(LocalDateTime now, Long afterId, int pageSize) {
        return lockStateMapper.findExpiredLocksByPage(now, afterId, pageSize).stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    /**
     * 构建 (userId, userType) 精确查询条件。
     * userType 为 null 时仅按 userId 查询（兼容过渡期数据）。
     */
    private LambdaQueryWrapper<AccountLockStateEntity> buildUserQuery(Long userId, UserTypeEnum userType) {
        LambdaQueryWrapper<AccountLockStateEntity> query = Wrappers.lambdaQuery();
        query.eq(AccountLockStateEntity::getUserId, userId);
        if (userType != null) {
            query.eq(AccountLockStateEntity::getUserType, userType.getValue());
        }
        return query;
    }

    private LockState toModel(AccountLockStateEntity entity) {
        return LockState.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .userType(UserTypeEnum.getEnum(entity.getUserType()))
                .locked(entity.getLocked())
                .lockType(entity.getLockType() != null ? LockType.valueOf(entity.getLockType()) : null)
                .lockReasonCode(entity.getLockReasonCode())
                .lockReasonDetail(entity.getLockReasonDetail())
                .lockedAt(entity.getLockedAt())
                .lockedUntil(entity.getLockedUntil())
                .operatorId(entity.getOperatorId())
                .operatorName(entity.getOperatorName())
                .failedLoginCount(entity.getFailedLoginCount())
                .lastFailedAt(entity.getLastFailedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
