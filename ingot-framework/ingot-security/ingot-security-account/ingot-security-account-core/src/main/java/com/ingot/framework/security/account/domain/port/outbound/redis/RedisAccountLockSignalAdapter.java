package com.ingot.framework.security.account.domain.port.outbound.redis;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.config.AccountLockSignalProperties;
import com.ingot.framework.security.account.domain.model.AccountLockSignal;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * <p>基于 {@link StringRedisTemplate} 的账号锁定信号适配器：双 Key 写入/删除，读写均 fail-open。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class RedisAccountLockSignalAdapter implements AccountLockSignalPort {

    private static final String PERMANENT_VALUE = "1";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final StringRedisTemplate redisTemplate;
    private final AccountLockSignalProperties properties;

    @Override
    public void writeLocked(AccountLockSignal signal) {
        if (signal == null || signal.userId() == null || signal.userType() == null) {
            return;
        }
        try {
            String userType = signal.userType().getValue();
            String value = signal.lockedUntil() == null
                    ? PERMANENT_VALUE
                    : ISO.format(signal.lockedUntil());
            long ttlSeconds = resolveTtlSeconds(signal.lockedUntil());
            if (ttlSeconds <= 0) {
                log.warn("[AccountLockSignal] TTL 无效，跳过写入 userId={}", signal.userId());
                return;
            }
            String uidKey = RedisKeyConstants.AccountLock.uidKey(userType, signal.userId());
            redisTemplate.opsForValue().set(uidKey, value, ttlSeconds, TimeUnit.SECONDS);
            if (StringUtils.hasText(signal.username())) {
                String nameKey = RedisKeyConstants.AccountLock.nameKey(userType, signal.username());
                redisTemplate.opsForValue().set(nameKey, value, ttlSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception ex) {
            log.warn("[AccountLockSignal] writeLocked fail-open userId={}: {}",
                    signal.userId(), ex.toString());
        }
    }

    @Override
    public void clearLocked(AccountLockSignal signal) {
        if (signal == null || signal.userId() == null || signal.userType() == null) {
            return;
        }
        try {
            String userType = signal.userType().getValue();
            redisTemplate.delete(RedisKeyConstants.AccountLock.uidKey(userType, signal.userId()));
            if (StringUtils.hasText(signal.username())) {
                redisTemplate.delete(RedisKeyConstants.AccountLock.nameKey(userType, signal.username()));
            }
        } catch (Exception ex) {
            log.warn("[AccountLockSignal] clearLocked fail-open userId={}: {}",
                    signal.userId(), ex.toString());
        }
    }

    @Override
    public boolean isLockedByUserId(UserTypeEnum userType, Long userId) {
        if (userType == null || userId == null) {
            return false;
        }
        try {
            Boolean has = redisTemplate.hasKey(
                    RedisKeyConstants.AccountLock.uidKey(userType.getValue(), userId));
            return Boolean.TRUE.equals(has);
        } catch (Exception ex) {
            log.warn("[AccountLockSignal] isLockedByUserId fail-open userId={}: {}",
                    userId, ex.toString());
            return false;
        }
    }

    @Override
    public boolean isLockedByUsername(UserTypeEnum userType, String username) {
        if (userType == null || !StringUtils.hasText(username)) {
            return false;
        }
        try {
            Boolean has = redisTemplate.hasKey(
                    RedisKeyConstants.AccountLock.nameKey(userType.getValue(), username));
            return Boolean.TRUE.equals(has);
        } catch (Exception ex) {
            log.warn("[AccountLockSignal] isLockedByUsername fail-open username={}: {}",
                    username, ex.toString());
            return false;
        }
    }

    private long resolveTtlSeconds(LocalDateTime lockedUntil) {
        if (lockedUntil == null) {
            int days = Math.max(properties.getPermanentLockTtlDays(), 1);
            return Duration.ofDays(days).getSeconds();
        }
        long seconds = Duration.between(LocalDateTime.now(), lockedUntil).getSeconds();
        return Math.max(seconds, 1L);
    }
}
