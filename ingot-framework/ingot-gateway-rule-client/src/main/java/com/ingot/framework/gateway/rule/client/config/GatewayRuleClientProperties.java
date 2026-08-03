package com.ingot.framework.gateway.rule.client.config;

import com.ingot.framework.commons.constants.RedisKeyConstants;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;

/**
 * <p>安全策略客户端的基础设施调参，只影响共享快照缓存与失效订阅的行为细节，不做任何功能门控。</p>
 *
 * <p>本类<b>不含</b>「SDK 总开关」：快照拉取链属于能力层，仅在 Feign 客户端
 * {@link com.ingot.cloud.security.api.rpc.RemoteSecurityPolicyService} 已注册时无条件装配，
 * 装配后不主动发请求（按需 lazy fetch），因此没有必要用开关关闭。各域功能由
 * {@code ingot.security.<domain>.enabled} 独立控制，与本类配置<b>互不级联</b>。</p>
 *
 * <h3>缓存结构</h3>
 * <p>四个域共享一份 {@code SecurityPolicySnapshotVO} 的分层缓存
 * （{@code L1 Caffeine → L2 Redis → remote → LKG → Nacos 地板}），冷启动与全量失效后
 * 只产生一次远端调用。各域在其上用版本号驱动的派生缓存持有自己的编译产物
 * （{@code Pattern}、{@code PathPattern} 等不可序列化对象只留在本机）。</p>
 *
 * <p>一致性由三重机制保障：失效广播负责秒级生效，L1/L2 TTL 为广播丢失兜底，
 * 快照版本号比对避免无谓重编译与 Sentinel 规则抖动。</p>
 *
 * <h3>典型配置示例</h3>
 *
 * <pre>{@code
 * ingot:
 *   security:
 *     policy:
 *       client:
 *         invalidation-enabled: true    # 跨节点失效订阅（生产建议开）
 *         resilience-enabled: true      # remote → LKG → 地板 降级阶梯
 *         local-floor-enabled: true     # 无 LKG 时是否落 Nacos 地板
 *         cache:
 *           l1-ttl: 5m
 *           l2-ttl: 30m
 * }</pre>
 *
 * @author jy
 * @since 2026/5/26
 * @see GatewayRuleClientAutoConfiguration
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.security.policy.client")
public class GatewayRuleClientProperties {

    /**
     * 跨节点失效订阅开关。
     * <ul>
     *     <li>{@code true}（默认）：装配
     *         {@link com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator}，
     *         订阅 {@link com.ingot.cloud.security.api.event.SecurityPolicyInvalidationEvent}，
     *         Platform 改规则后各节点自动 evict + reload。</li>
     *     <li>{@code false}：不订阅；规则变更后依赖 L1/L2 TTL 自然收敛，或手动调用
     *         {@code POST /platform/security/policy/broadcast-invalidation} 触发。
     *         适用于单实例 + 纯 local 调试。</li>
     * </ul>
     */
    private boolean invalidationEnabled = true;

    /**
     * 是否启用 {@code remote → LKG → Nacos 地板} 弹性降级链。
     * <ul>
     *     <li>{@code true}（默认）：远端成功时写入 LKG 快照；远端不可用时依次回落 LKG、地板。</li>
     *     <li>{@code false}：退化为纯 Feign 直连，不写 LKG 也不降级，远端不可用直接抛异常。
     *         注意各域 {@code mode=remote} 仍可正常装配，L1/L2 缓存也仍然生效。</li>
     * </ul>
     */
    private boolean resilienceEnabled = true;

    /**
     * remote 不可用且 LKG 为空时，是否回落到 Nacos {@code in-security-policy.yml} 地板配置。
     * <p>{@code false} 时该场景抛
     * {@link com.ingot.framework.gateway.rule.client.internal.PolicyRemoteUnavailableException}
     * 而非 fail-open。仅在 {@link #resilienceEnabled} 为 {@code true} 时有意义。</p>
     */
    private boolean localFloorEnabled = true;

    /**
     * LKG（Last Known Good）快照的 Redis key；长存不过期，与 L2 热缓存命名空间区分。
     */
    private String lkgRedisKey = RedisKeyConstants.SecurityPolicy.LKG_SNAPSHOT;

    /**
     * 共享快照的分层缓存参数。
     */
    @NestedConfigurationProperty
    private Cache cache = new Cache();

    /**
     * <p>共享快照缓存的分层参数。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Getter
    @Setter
    public static class Cache {

        /**
         * 是否启用 L1 进程内缓存。关闭后每次读取都会穿透到 L2 或远端。
         */
        private boolean l1Enabled = true;

        /**
         * L1 存活时间，同时是失效广播丢失时的最长 stale 窗口。
         * 无单位数值按分钟解析，避免误当成毫秒导致写入即过期。
         */
        @DurationUnit(ChronoUnit.MINUTES)
        private Duration l1Ttl = Duration.ofMinutes(5);

        /**
         * L1 最大条目数；共享快照为单 key，取小值即可。
         */
        private long l1MaximumSize = 8;

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
         * L2 共享快照的 Redis key。
         */
        private String l2RedisKey = RedisKeyConstants.SecurityPolicy.SNAPSHOT;

        /**
         * Sentinel 规则的定时兜底刷新间隔；{@code null} 或非正表示关闭（默认）。
         * <p>限流域在请求路径上没有缓存读者，正常依赖其他域的流量刷新共享快照并触发版本比对。
         * 仅当部署中只启用限流域、其他域完全无流量时才需要打开本项。</p>
         */
        private Duration refreshInterval;
    }
}
