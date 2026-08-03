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
     * 违规升级域<b>装配</b>总开关；默认 false，避免影响现有部署。
     * <p>由 {@link ViolationEscalationAutoConfiguration} 上的 {@code @ConditionalOnProperty} 按属性键
     * {@code ingot.security.violation-escalation.enabled} 消费，是违规升级域生效的<b>唯一</b>门控，
     * 与 {@code ingot.security.policy.client.*} 互不级联；关闭时本类的 Properties Bean 不装配，
     * Nacos 地板中的违规升级片段随之为空。</p>
     * <p>注意与 {@link Policy#isEnabled()} 区分：本字段决定 Service 是否装配，
     * {@code policy.enabled} 决定装配后运行期是否真的计数并临时封禁；
     * 且 {@code policy.enabled} 在 {@link Mode#REMOTE} 下取自远端快照，yaml 值被忽略。</p>
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
         * 运行期是否启用违规计数与临时封禁；仅 {@link Mode#LOCAL} 下取本值，
         * {@link Mode#REMOTE} 下取自远端快照。与外层
         * {@link ViolationEscalationProperties#isEnabled()}（装配开关）语义不同。
         */
        private boolean enabled = true;
    }

    public enum Mode {
        LOCAL, REMOTE
    }
}
