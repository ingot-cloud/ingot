package com.ingot.framework.security.account.domain.port.outbound.noop;

import java.time.LocalDateTime;
import java.util.Optional;

import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * UserAccountPort 空实现
 * <p>
 * 当不需要用户账号管理时，使用此空实现作为默认值。
 * 各业务服务（PMS/Member）应提供自己的实现来替换此类。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
public class NoOpUserAccountPort implements UserAccountPort {

    @Override
    public UserAccount save(UserAccount account) {
        log.warn("[NoOp] UserAccountPort.save 未实现，请提供具体实现");
        return account;
    }

    @Override
    public Optional<UserAccount> findById(Long userId, UserTypeEnum userType) {
        log.warn("[NoOp] UserAccountPort.findById 未实现");
        return Optional.empty();
    }

    @Override
    public Optional<UserAccount> findByUsername(String username, UserTypeEnum userType) {
        log.warn("[NoOp] UserAccountPort.findByUsername 未实现");
        return Optional.empty();
    }

    @Override
    public Optional<UserAccount> findByPhone(String phone, UserTypeEnum userType) {
        log.warn("[NoOp] UserAccountPort.findByPhone 未实现");
        return Optional.empty();
    }

    @Override
    public boolean existsByUsername(String username, UserTypeEnum userType) {
        log.warn("[NoOp] UserAccountPort.existsByUsername 未实现，返回 false");
        return false;
    }

    @Override
    public void updateStatus(Long userId, UserTypeEnum userType, boolean enabled) {
        log.warn("[NoOp] UserAccountPort.updateStatus 未实现，userId={}, enabled={}", userId, enabled);
    }

    @Override
    public void updateLockStatus(Long userId, UserTypeEnum userType, boolean locked) {
        log.warn("[NoOp] UserAccountPort.updateLockStatus 未实现，userId={}, locked={}", userId, locked);
    }

    @Override
    public void updateLastLogin(Long userId, UserTypeEnum userType, LocalDateTime loginAt, String loginIp) {
        log.warn("[NoOp] UserAccountPort.updateLastLogin 未实现，userId={}", userId);
    }

    @Override
    public boolean updateWithVersion(UserAccount account, Long expectedVersion) {
        log.warn("[NoOp] UserAccountPort.updateWithVersion 未实现，返回 false");
        return false;
    }

    @Override
    public void delete(Long userId, UserTypeEnum userType) {
        log.warn("[NoOp] UserAccountPort.delete 未实现 userId={} userType={}", userId, userType);
    }
}
