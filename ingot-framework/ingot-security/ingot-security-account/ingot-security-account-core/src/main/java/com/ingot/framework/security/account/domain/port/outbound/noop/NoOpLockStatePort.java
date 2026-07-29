package com.ingot.framework.security.account.domain.port.outbound.noop;

import com.ingot.framework.security.account.domain.model.LockState;
import com.ingot.framework.security.account.domain.model.enums.LockType;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * LockStatePort 空实现
 * <p>
 * 当不需要独立的锁定状态表时，使用此空实现作为默认值。
 * 默认的 {@link com.ingot.framework.security.account.adapter.port.DefaultLockStatePortAdapter}
 * 提供基于 account_lock_state 表的完整实现；
 * 此 NoOp 实现适用于只需要 sys_user.locked 冗余字段而无需独立锁定表的场景。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
public class NoOpLockStatePort implements LockStatePort {

    @Override
    public Optional<LockState> findByUser(Long userId, UserTypeEnum userType) {
        log.debug("[NoOp] LockStatePort.findByUser userId={}", userId);
        return Optional.empty();
    }

    @Override
    public LockState initialize(Long userId, UserTypeEnum userType) {
        log.debug("[NoOp] LockStatePort.initialize userId={}", userId);
        return LockState.builder().userId(userId).locked(false).failedLoginCount(0).build();
    }

    @Override
    public int incrementFailCount(Long userId, UserTypeEnum userType) {
        log.debug("[NoOp] LockStatePort.incrementFailCount userId={}", userId);
        return 0;
    }

    @Override
    public void resetFailCount(Long userId, UserTypeEnum userType) {
        log.debug("[NoOp] LockStatePort.resetFailCount userId={}", userId);
    }

    @Override
    public void updateLockStatus(Long userId, UserTypeEnum userType,
                                  boolean locked, LocalDateTime lockedUntil,
                                  String reasonCode, LockType lockType,
                                  Long operatorId, String operatorName) {
        log.debug("[NoOp] LockStatePort.updateLockStatus userId={}, locked={}", userId, locked);
    }

    @Override
    public void deleteByUser(Long userId, UserTypeEnum userType) {
        log.debug("[NoOp] LockStatePort.deleteByUser userId={}", userId);
    }

    @Override
    public List<LockState> findExpiredLocksByPage(LocalDateTime now, Long afterId, int pageSize) {
        return Collections.emptyList();
    }
}
