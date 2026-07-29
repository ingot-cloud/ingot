package com.ingot.cloud.security.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 统一安全事件配置（{@code ingot.security.event.*}）。
 *
 * <p>PMS、Member 与 ingot-security 共用同一配置结构，但各自 Nacos dataId 独立生效
 * （如 {@code in-service-pms.yml}、{@code in-service-member.yml}、{@code in-service-security.yml}）。
 * 代码中的值为<b>缺省默认</b>；生产环境建议在 Nacos 按服务职责覆盖，尤其是 {@code retention.days}。</p>
 *
 * <p>上报开关语义：{@link #enabled} 为总开关；{@code false} 时不做任何上报（本地与中心均不写）。
 * {@code true} 时由 {@link #mode} 决定落点：{@code local} 仅业务库，{@code remote} 业务库 + 安全中心。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "ingot.security.event")
public class SecurityEventProperties {

    /**
     * 是否启用安全事件上报（总开关）。
     * <p>{@code false}：本地与中心均不上报；{@code true}：由 {@link #mode} 决定上报目标。</p>
     */
    private boolean enabled = true;

    /**
     * 上报目标（仅 {@link #enabled}={@code true} 时生效）。
     * <ul>
     *     <li>{@code local}：仅写入业务库 {@code account_security_event}</li>
     *     <li>{@code remote}：写入业务库并异步上报 ingot-security {@code security_event}</li>
     * </ul>
     */
    private String mode = "local";

    /**
     * 上报方模块标识，写入中心库 {@code security_event.source_module}。
     * <p>示例：{@code ingot-pms}、{@code ingot-member}；网关 ACCESS 类事件由网关侧映射固定值。</p>
     */
    private String sourceModule = "unknown";

    /**
     * 按事件大类控制是否上报（{@code mode=remote} 时对中心上报生效；{@code local} 时写本地表不受限）。
     */
    private Categories categories = new Categories();

    /**
     * 过期事件物理删除策略（与 {@link #enabled}、{@link #mode} 无关）。
     */
    private Retention retention = new Retention();

    /**
     * 异步上报至安全中心时的有界队列与攒批参数（{@code mode=remote} 时生效）。
     */
    private Async async = new Async();

    /**
     * 是否开启任意形式的安全事件上报。
     */
    public boolean isReportingEnabled() {
        return enabled;
    }

    /**
     * 是否仅上报至本地业务库。
     */
    public boolean isLocalMode() {
        return enabled && "local".equalsIgnoreCase(mode);
    }

    /**
     * 是否上报至本地业务库并同步至安全中心。
     */
    public boolean isRemoteMode() {
        return enabled && "remote".equalsIgnoreCase(mode);
    }

    public boolean isCategoryEnabled(String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        return switch (category.toUpperCase()) {
            case "AUTH" -> categories.auth;
            case "ACCOUNT" -> categories.account;
            case "CREDENTIAL" -> categories.credential;
            case "ACCESS" -> categories.access;
            default -> true;
        };
    }

    /**
     * {@code mode=remote} 时，该类别是否允许上报中心。
     */
    public boolean isRemoteCategoryEnabled(String category) {
        return isRemoteMode() && isCategoryEnabled(category);
    }

    public boolean isRetentionActive() {
        return retention != null && retention.isEnabled() && retention.getDays() > 0;
    }

    /**
     * 安全事件大类上报开关，对应 {@code security_event.event_category}。
     */
    @Data
    public static class Categories {

        /** 认证类：登录成功/失败等（AUTH）。 */
        private boolean auth = true;

        /** 账号类：锁定/解锁/创建等（ACCOUNT）。 */
        private boolean account = true;

        /** 凭证类：改密/重置等（CREDENTIAL）。 */
        private boolean credential = true;

        /** 访问防护类：网关封禁/限流违规等（ACCESS）；通常仅网关产生。 */
        private boolean access = true;
    }

    /**
     * 过期安全事件定时清理参数（各服务独立配置保留天数）。
     */
    @Data
    public static class Retention {

        /**
         * 是否启用定时清理任务。
         * <p>{@code false} 时不删除历史记录（等同永久保留）。</p>
         */
        private boolean enabled = true;

        /**
         * 保留天数；早于 {@code now - days} 的记录将被物理删除。
         * <p>{@code 0} 表示永久保留。代码默认 {@code 30}；Nacos 样例中 PMS/Member 多为 {@code 90}，
         * 中心库建议配置更长（如 {@code 90}～{@code 180}），具体以合规与容量为准。</p>
         */
        private int days = 30;

        /**
         * 单批 {@code DELETE ... LIMIT N} 条数，避免长事务与大锁。
         */
        private int batchSize = 500;

        /**
         * 单次定时任务最多连续执行的批次数，防止清理洪峰占用连接过久。
         */
        private int maxRounds = 100;
    }

    /**
     * 异步远程上报缓冲参数，防止突发流量撑爆堆内存。
     */
    @Data
    public static class Async {

        /**
         * 待上报事件有界队列容量；满时丢弃新事件并打 warn（不阻塞业务线程）。
         */
        private int queueCapacity = 2048;

        /**
         * 单次 {@code reportBatch} 条数上限。
         */
        private int batchSize = 32;

        /**
         * 消费者攒批时 poll 超时（毫秒）；队列空则等待该时长后再次 poll。
         */
        private long pollTimeoutMs = 100;

        /**
         * 关闭时等待 worker 排空队列的最长时间（毫秒）。
         */
        private long shutdownTimeoutMs = 5000;
    }
}
