package com.ingot.framework.security.account.domain.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.commons.model.security.PolicySourceMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

/**
 * <p>账号域配置属性，绑定 {@code ingot.security.account}。</p>
 *
 * <p>{@code mode=local} 时 {@link #lockout} 直接生效；{@code mode=remote} 时从安全中心拉取，
 * {@link #lockout} 仅作为远端与 LKG 均不可用时的 Nacos 地板。</p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Data
@ConfigurationProperties(prefix = "ingot.security.account")
public class AccountDomainProperties {

    /**
     * 策略来源：{@link PolicySourceMode#LOCAL} 纯 Nacos；
     * {@link PolicySourceMode#REMOTE} 走安全中心分层缓存链。
     * <p>代码缺省为 {@link PolicySourceMode#LOCAL}。</p>
     */
    private PolicySourceMode mode = PolicySourceMode.LOCAL;

    /**
     * remote 模式的降级参数与分层缓存调参。
     */
    private PolicyConfig policy = new PolicyConfig();

    /**
     * 锁定策略。{@code mode=local} 时直接生效；{@code mode=remote} 时作 Nacos 地板。
     */
    private LockoutPolicy lockout = new LockoutPolicy();

    /**
     * <p>remote 模式的降级与缓存调参。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Data
    public static class PolicyConfig {
        /**
         * 远端不可用时的末级回落。
         */
        private Fallback fallback = new Fallback();

        /**
         * remote 模式的分层缓存参数；local 模式不使用。
         */
        private Cache cache = new Cache();
    }

    /**
     * <p>账号锁定策略 remote 模式的 L1/L2 调参。</p>
     *
     * <p>配置键归属本模块（{@code ingot.security.account.policy.cache.*}），由装配侧映射为
     * 框架的 {@code LayeredCacheSettings}。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Data
    public static class Cache {
        /**
         * 是否启用 L1 进程内缓存。关闭后每次读取都会穿透到 L2 或远端。
         */
        private boolean l1Enabled = true;

        /**
         * L1 存活时间，同时是失效广播丢失时的最长 stale 窗口。
         * <p>无单位数值按分钟解析。</p>
         */
        @DurationUnit(ChronoUnit.MINUTES)
        private Duration l1Ttl = Duration.ofMinutes(5);

        /**
         * L1 最大条目数；锁定策略为单 key，取小值即可。
         */
        private long l1MaximumSize = 16;

        /**
         * 是否启用 L2 Redis 共享缓存。Redis 不可用时本项自动失效。
         */
        private boolean l2Enabled = true;

        /**
         * L2 存活时间。无单位数值按分钟解析。
         */
        @DurationUnit(ChronoUnit.MINUTES)
        private Duration l2Ttl = Duration.ofMinutes(30);

        /**
         * L2 热缓存 Redis key，默认 {@code in:sec:account:policy:snapshot}。
         */
        private String l2RedisKey = RedisKeyConstants.AccountLockoutPolicy.SNAPSHOT;
    }

    /**
     * <p>远端不可用且无 LKG 时的末级回落开关。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Data
    public static class Fallback {
        /**
         * 远端不可用且无 LKG 时，是否回落 {@link LockoutPolicy} 地板。
         * <p>关闭后该场景向上抛出远程不可用异常，而不是静默使用本地地板。</p>
         */
        private boolean localFloorEnabled = true;
    }

    /**
     * <p>Nacos 锁定策略绑定对象，与领域 {@code LockoutPolicy} 值对象分离。</p>
     *
     * @author jymot
     * @since 2026-02-13
     */
    @Data
    public static class LockoutPolicy {
        /**
         * 是否启用自动锁定。
         */
        private boolean enabled = true;

        /**
         * 失败次数阈值。
         */
        private int maxAttempts = 5;

        /**
         * 锁定时长（分钟），0=永久锁定。
         */
        private int lockDurationMinutes = 30;

        /**
         * 失败计数窗口期（分钟）。
         */
        private int attemptWindowMinutes = 15;

        /**
         * 从第几次失败开始给出"还剩几次将锁定"的详细提示，
         * 之前的失败一律返回通用"账号或密码错误"，避免暴露用户存在性。
         * <p>
         * 例：{@code maxAttempts=5}，{@code hintAfterAttempts=3} 时：
         * </p>
         * <ul>
         *   <li>第 1/2 次：通用提示</li>
         *   <li>第 3 次：剩余 2 次</li>
         *   <li>第 4 次：剩余 1 次</li>
         *   <li>第 5 次：触发自动锁定，下次登录命中锁定分支</li>
         * </ul>
         */
        private int hintAfterAttempts = 3;
    }
}
