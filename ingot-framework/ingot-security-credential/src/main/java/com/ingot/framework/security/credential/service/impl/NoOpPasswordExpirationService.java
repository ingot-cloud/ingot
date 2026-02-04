package com.ingot.framework.security.credential.service.impl;

import com.ingot.framework.security.credential.model.domain.PasswordExpiration;
import com.ingot.framework.security.credential.service.PasswordExpirationService;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认密码过期服务实现（空实现）
 * <p>密码永不过期，适用于不需要过期检查的场景</p>
 *
 * @author jymot
 * @since 2026-01-24
 */
@Slf4j
public class NoOpPasswordExpirationService implements PasswordExpirationService {

    @Override
    public PasswordExpiration getByUserId(Long userId) {
        log.debug("使用默认空实现，返回null");
        return null;
    }

    @Override
    public void initExpiration(Long userId, int maxDays, int graceLogins) {
        log.debug("使用默认空实现，不初始化过期信息");
        // 空实现，不初始化
    }

    @Override
    public void updateLastChanged(Long userId, int maxDays) {
        log.debug("使用默认空实现，不更新过期时间");
        // 空实现，不更新
    }

    @Override
    public int decrementGraceLogin(Long userId) {
        log.debug("使用默认空实现，返回0");
        return 0;
    }

    @Override
    public boolean isExpired(Long userId) {
        log.debug("使用默认空实现，密码未过期");
        return false; // 永不过期
    }

    @Override
    public boolean needsWarning(Long userId, int warningDaysBefore) {
        log.debug("使用默认空实现，无需警告");
        return false; // 无需警告
    }

    @Override
    public void updateNextWarning(Long userId) {
        log.debug("使用默认空实现，不更新警告时间");
        // 空实现，不更新
    }

    @Override
    public void deleteByUserId(Long userId) {
        log.debug("使用默认空实现，无需删除");
        // 空实现，无需删除
    }
}
