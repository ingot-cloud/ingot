package com.ingot.framework.security.account.domain.port.outbound.noop;

import com.ingot.framework.security.account.domain.port.outbound.UserCredentialPort;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * UserCredentialPort 空实现
 * <p>
 * 当不需要独立凭证管理时，使用此空实现作为默认值。
 * 各业务服务（PMS/Member）应提供自己的实现来替换此类。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
public class NoOpUserCredentialPort implements UserCredentialPort {

    @Override
    public String getPasswordHash(Long userId, UserTypeEnum userType) {
        log.warn("[NoOp] UserCredentialPort.getPasswordHash 未实现，返回 null");
        return null;
    }

    @Override
    public boolean updatePassword(Long userId, UserTypeEnum userType,
                                  String newPasswordHash, LocalDateTime changedAt,
                                  Long expectedVersion, boolean mustChangePwd) {
        log.warn("[NoOp] UserCredentialPort.updatePassword 未实现，返回 false");
        return false;
    }

    @Override
    public LocalDateTime getPasswordChangedAt(Long userId, UserTypeEnum userType) {
        log.warn("[NoOp] UserCredentialPort.getPasswordChangedAt 未实现，返回 null");
        return null;
    }
}
