package com.ingot.framework.security.account.domain.service;

import java.time.LocalDateTime;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.AccountLockSignal;
import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.account.domain.model.LockState;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.model.enums.LockReason;
import com.ingot.framework.security.account.domain.model.enums.LockType;
import com.ingot.framework.security.account.domain.port.inbound.LockAccountUseCase;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.security.account.domain.support.AfterCommitActions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 锁定账号用例实现
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LockAccountUseCaseService implements LockAccountUseCase {

    private final UserAccountPort userAccountPort;
    private final LockStatePort lockStatePort;
    private final SecurityEventPort securityEventPort;
    private final AccountLockSignalPort accountLockSignalPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockManually(LockCommand command) {
        log.info("管理员 {} 手动锁定用户 {}", command.getOperatorId(), command.getUserId());

        if (alreadyLocked(command.getUserId(), command.getUserType())) {
            log.info("用户 {} 已处于锁定状态，跳过重复手动锁定", command.getUserId());
            return;
        }

        // 1. 更新锁定状态（account_lock_state 表）
        lockStatePort.updateLockStatus(
                command.getUserId(),
                command.getUserType(),
                true,
                command.getLockedUntil(),
                command.getReason().getCode(),
                LockType.MANUAL,
                command.getOperatorId(),
                command.getOperatorName()
        );

        // 2. 同步更新用户表 locked 字段
        userAccountPort.updateLockStatus(command.getUserId(), command.getUserType(), true);

        // 3. 发布账号锁定事件
        AccountSecurityEvent event = AccountSecurityEvent.accountLocked(
                command.getUserId(),
                command.getUserType(),
                command.getReason().getCode(),
                command.getReasonDetail(),
                command.getSource(),
                command.getOperatorId(),
                command.getOperatorName()
        );
        securityEventPort.publishEvent(event);

        scheduleWriteLockSignal(command.getUserId(), command.getUserType(), command.getLockedUntil());

        log.info("用户 {} 已被锁定", command.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockAutomatically(Long userId, UserTypeEnum userType,
                                   LockReason reason, Integer durationMinutes) {
        log.warn("系统自动锁定用户 {}，原因: {}", userId, reason.getDescription());

        if (alreadyLocked(userId, userType)) {
            log.info("用户 {} 已处于锁定状态，跳过重复自动锁定", userId);
            return;
        }

        // 计算锁定到期时间
        LocalDateTime lockedUntil = null;
        if (durationMinutes != null && durationMinutes > 0) {
            lockedUntil = LocalDateTime.now().plusMinutes(durationMinutes);
        }

        // 1. 更新锁定状态
        lockStatePort.updateLockStatus(
                userId,
                userType,
                true,
                lockedUntil,
                reason.getCode(),
                LockType.AUTO,
                null,
                null
        );

        // 2. 同步更新用户表 locked 字段
        userAccountPort.updateLockStatus(userId, userType, true);

        // 3. 发布账号锁定事件
        AccountSecurityEvent event = AccountSecurityEvent.accountLocked(
                userId,
                userType,
                reason.getCode(),
                reason.getDescription(),
                EventSource.SYSTEM,
                null,
                null
        );
        securityEventPort.publishEvent(event);

        scheduleWriteLockSignal(userId, userType, lockedUntil);

        log.info("用户 {} 已被自动锁定，到期时间: {}", userId, lockedUntil);
    }

    private void scheduleWriteLockSignal(Long userId, UserTypeEnum userType, LocalDateTime lockedUntil) {
        String username = userAccountPort.findById(userId, userType)
                .map(account -> account.getUsername())
                .orElse(null);
        AccountLockSignal signal = new AccountLockSignal(userId, userType, username, lockedUntil);
        AfterCommitActions.run(() -> accountLockSignalPort.writeLocked(signal));
    }

    private boolean alreadyLocked(Long userId, UserTypeEnum userType) {
        return lockStatePort.findByUser(userId, userType)
                .map(LockState::isLocked)
                .orElse(false);
    }
}
