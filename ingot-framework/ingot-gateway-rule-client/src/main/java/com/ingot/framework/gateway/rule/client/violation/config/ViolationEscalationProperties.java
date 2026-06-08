package com.ingot.framework.gateway.rule.client.violation.config;

import com.ingot.framework.gateway.rule.client.violation.model.ViolationEscalationConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 限流违规升级域配置。
 *
 * <p>配置前缀：{@code ingot.security.violation-escalation}。</p>
 *
 * <h3>local 模式示例</h3>
 * <pre>{@code
 * ingot:
 *   security:
 *     violation-escalation:
 *       enabled: true
 *       policy:
 *         mode: local
 *         window-sec: 60
 *         block-threshold: 30
 *         temp-block-ttl-sec: 900
 * }</pre>
 *
 * <h3>remote 模式示例</h3>
 * <pre>{@code
 * ingot:
 *   security:
 *     violation-escalation:
 *       enabled: true
 *       policy:
 *         mode: remote
 * }</pre>
 *
 * @author jy
 * @since 2026/6/5
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.security.violation-escalation")
public class ViolationEscalationProperties {

    /**
     * 违规升级域总开关；默认 false，避免影响现有部署。
     */
    private boolean enabled = false;

    private Policy policy = new Policy();

    @Getter
    @Setter
    public static class Policy {

        /** local：yaml 内联；remote：Feign 快照。 */
        private Mode mode = Mode.LOCAL;

        /**
         * 违规计数滑动窗口（秒）；local 模式生效。
         * 默认 {@link ViolationEscalationConfig#DEFAULT_WINDOW_SEC}。
         */
        private int windowSec = ViolationEscalationConfig.DEFAULT_WINDOW_SEC;

        /**
         * 窗口内限流拒绝次数阈值；local 模式生效。
         * 默认 {@link ViolationEscalationConfig#DEFAULT_BLOCK_THRESHOLD}。
         */
        private int blockThreshold = ViolationEscalationConfig.DEFAULT_BLOCK_THRESHOLD;

        /**
         * 临时封禁 TTL（秒）；local 模式生效。
         * 默认 {@link ViolationEscalationConfig#DEFAULT_TEMP_BLOCK_TTL_SEC}（15 分钟）。
         */
        private int tempBlockTtlSec = ViolationEscalationConfig.DEFAULT_TEMP_BLOCK_TTL_SEC;

        /**
         * 是否启用违规计数与临时封禁；local 模式生效。
         */
        private boolean enabled = true;
    }

    public enum Mode {
        LOCAL, REMOTE
    }
}
