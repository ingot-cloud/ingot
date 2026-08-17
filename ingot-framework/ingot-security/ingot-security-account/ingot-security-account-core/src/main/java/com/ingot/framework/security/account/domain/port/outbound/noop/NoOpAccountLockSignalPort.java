package com.ingot.framework.security.account.domain.port.outbound.noop;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.AccountLockSignal;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>{@link AccountLockSignalPort} 空实现：无 Redis 时 fail-open，不阻断锁定落库主路径。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class NoOpAccountLockSignalPort implements AccountLockSignalPort {

    @Override
    public void writeLocked(AccountLockSignal signal) {
        log.debug("[NoOp] AccountLockSignalPort.writeLocked userId={}",
                signal == null ? null : signal.userId());
    }

    @Override
    public void clearLocked(AccountLockSignal signal) {
        log.debug("[NoOp] AccountLockSignalPort.clearLocked userId={}",
                signal == null ? null : signal.userId());
    }

    @Override
    public boolean isLockedByUserId(UserTypeEnum userType, Long userId) {
        return false;
    }

    @Override
    public boolean isLockedByUsername(UserTypeEnum userType, String username) {
        return false;
    }
}
