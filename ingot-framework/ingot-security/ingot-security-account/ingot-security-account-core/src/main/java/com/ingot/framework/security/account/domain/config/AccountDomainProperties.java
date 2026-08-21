package com.ingot.framework.security.account.domain.config;

import com.ingot.framework.commons.model.security.PolicySourceMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 账号域配置属性
 * <p>前缀统一为 {@code ingot.security.account}，与 {@code ingot.security.credential} 命名对齐。</p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Data
@ConfigurationProperties(prefix = "ingot.security.account")
public class AccountDomainProperties {

    /**
     * 策略来源：{@link PolicySourceMode#LOCAL} 纯 Nacos；
     * {@link PolicySourceMode#REMOTE} 安全中心弹性阶梯（后续 change 提供实现）。
     * <p>本期仅实现 {@link PolicySourceMode#LOCAL}；配置为 {@link PolicySourceMode#REMOTE}
     * 但无远程实现时回退 local 并告警。</p>
     */
    private PolicySourceMode mode = PolicySourceMode.LOCAL;

    /**
     * 锁定策略配置
     */
    private LockoutPolicy lockout = new LockoutPolicy();

    @Data
    public static class LockoutPolicy {
        /**
         * 是否启用自动锁定
         */
        private boolean enabled = true;

        /**
         * 失败次数阈值
         */
        private int maxAttempts = 5;

        /**
         * 锁定时长（分钟），0=永久锁定
         */
        private int lockDurationMinutes = 30;

        /**
         * 失败计数窗口期（分钟）
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
