package com.ingot.framework.security.credential.data.service;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.framework.security.credential.data.mapper.PasswordExpirationMapper;
import com.ingot.framework.security.credential.model.domain.PasswordExpiration;
import com.ingot.framework.security.credential.service.PasswordExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 密码过期服务实现（通用，基于 MyBatis-Plus）
 *
 * @author jymot
 * @since 2026-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordExpirationServiceImpl implements PasswordExpirationService {

    private final PasswordExpirationMapper mapper;

    @Override
    public PasswordExpiration getByUserId(Long userId) {
        log.debug("获取密码过期信息 - userId: {}", userId);

        return mapper.selectOne(
                Wrappers.<PasswordExpiration>lambdaQuery()
                        .eq(PasswordExpiration::getUserId, userId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initExpiration(Long userId, int maxDays, int graceLogins) {
        log.info("初始化密码过期信息 - userId: {}, maxDays: {}", userId, maxDays);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = maxDays > 0 ? now.plusDays(maxDays) : null;

        PasswordExpiration expiration = new PasswordExpiration();
        expiration.setUserId(userId);
        expiration.setLastChangedAt(now);
        expiration.setExpiresAt(expiresAt);
        expiration.setGraceLoginRemaining(graceLogins);
        expiration.setCreatedAt(now);
        expiration.setUpdatedAt(now);

        mapper.insert(expiration);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastChanged(Long userId, int maxDays) {
        log.info("更新密码修改时间 - userId: {}, maxDays: {}", userId, maxDays);

        PasswordExpiration expiration = getByUserId(userId);
        LocalDateTime now = LocalDateTime.now();

        if (expiration == null) {
            // 不存在则初始化
            initExpiration(userId, maxDays, 0);
        } else {
            // 更新现有记录
            expiration.setLastChangedAt(now);
            expiration.setExpiresAt(maxDays > 0 ? now.plusDays(maxDays) : null);
            expiration.setGraceLoginRemaining(0);
            expiration.setUpdatedAt(now);
            mapper.updateById(expiration);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int decrementGraceLogin(Long userId) {
        log.info("减少宽限登录次数 - userId: {}", userId);

        PasswordExpiration expiration = getByUserId(userId);
        if (expiration == null) {
            log.warn("密码过期信息不存在 - userId: {}", userId);
            return 0;
        }

        int remaining = Math.max(0, expiration.getGraceLoginRemaining() - 1);
        expiration.setGraceLoginRemaining(remaining);
        expiration.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(expiration);

        log.debug("剩余宽限登录次数 - userId: {}, remaining: {}", userId, remaining);
        return remaining;
    }

    @Override
    public boolean isExpired(Long userId) {
        log.debug("检查密码是否过期 - userId: {}", userId);

        PasswordExpiration expiration = getByUserId(userId);
        if (expiration == null || expiration.getExpiresAt() == null) {
            return false;
        }

        boolean expired = LocalDateTime.now().isAfter(expiration.getExpiresAt());
        log.debug("密码过期检查结果 - userId: {}, expired: {}", userId, expired);
        return expired;
    }

    @Override
    public boolean needsWarning(Long userId, int warningDaysBefore) {
        log.debug("检查是否需要提醒 - userId: {}, warningDaysBefore: {}", userId, warningDaysBefore);

        PasswordExpiration expiration = getByUserId(userId);
        if (expiration == null || expiration.getExpiresAt() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningStartAt = expiration.getExpiresAt().minusDays(warningDaysBefore);

        // 已经过了警告开始时间，但还没过期
        boolean needsWarning = now.isAfter(warningStartAt) && now.isBefore(expiration.getExpiresAt());

        // 检查是否已经在最近时间内警告过（避免频繁提醒）
        if (needsWarning && expiration.getNextWarningAt() != null) {
            needsWarning = now.isAfter(expiration.getNextWarningAt());
        }

        log.debug("提醒检查结果 - userId: {}, needsWarning: {}", userId, needsWarning);
        return needsWarning;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNextWarning(Long userId) {
        log.debug("更新下次提醒时间 - userId: {}", userId);

        PasswordExpiration expiration = getByUserId(userId);
        if (expiration != null) {
            // 24小时后再次提醒
            expiration.setNextWarningAt(LocalDateTime.now().plusHours(24));
            expiration.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(expiration);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUserId(Long userId) {
        log.info("删除密码过期信息 - userId: {}", userId);

        mapper.delete(
                Wrappers.<PasswordExpiration>lambdaQuery()
                        .eq(PasswordExpiration::getUserId, userId)
        );
    }
}
