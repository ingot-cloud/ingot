package com.ingot.framework.security.account.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>账号锁定 Redis 信号配置，绑定 {@code ingot.security.account.signal}。</p>
 *
 * <p>TTL 必须全局同一份，配在 Nacos {@code in-security-policy.yml}，不要在各服务 yml 复制。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "ingot.security.account.signal")
public class AccountLockSignalProperties {

    /**
     * 永久锁定时 uid/name key 的 TTL（天），默认 30。
     * <p>临时锁 TTL 仍对齐 {@code lockedUntil}，本项只作用于永久锁。</p>
     */
    private int permanentLockTtlDays = 30;
}
