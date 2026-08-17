package com.ingot.framework.security.account.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>账号锁定 Redis 信号配置。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "ingot.security.account-lock-signal")
public class AccountLockSignalProperties {

    /**
     * 永久锁定时 uid/name key 的 TTL（天），默认 30。
     */
    private int permanentLockTtlDays = 30;
}
