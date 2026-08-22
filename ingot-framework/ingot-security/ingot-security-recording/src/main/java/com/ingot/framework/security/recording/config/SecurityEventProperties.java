package com.ingot.framework.security.recording.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ingot.framework.security.event.codes.SecurityEventCategoryCodes;
import com.ingot.framework.security.recording.model.RecordingTarget;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>安全事件唯一运行时配置，前缀 {@code ingot.security.event}。</p>
 *
 * <p>PMS、Member、Gateway 与 ingot-security 共用同一配置结构，但各自 Nacos dataId 独立生效
 * （如 {@code in-service-pms.yml}、{@code in-service-member.yml}、{@code in-service-gateway.yml}、
 * {@code in-service-security.yml}）。完整样例见模块根目录 {@code example.yml}。</p>
 *
 * <p>经 {@link RecordingConfigResolver} 解析 {@code target}、{@code shadow-targets} 与
 * {@code delivery.memory}，供 dispatcher、Store、Transport 与 retention 消费。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see RecordingConfigResolver
 */
@Data
@ConfigurationProperties(prefix = "ingot.security.event")
public class SecurityEventProperties {

    /**
     * 是否启用安全事件上报（总开关）。
     * <p>{@code false}：不做任何上报；{@code true}：由 {@link #target} 决定落点。</p>
     * <p>须显式配置 {@code enabled=true} 才会装配 recording 框架（未配置时默认不启用）。</p>
     */
    private boolean enabled = false;

    /**
     * 主投递目标。
     * <ul>
     *     <li>{@link RecordingTarget#LOCAL}：写入本服务库 canonical {@code security_event}（须配 {@link #primaryStore}）</li>
     *     <li>{@link RecordingTarget#CENTER}：经 Feign/Transport 上报 ingot-security 中心 admission</li>
     * </ul>
     * <p>YAML 写 {@code local} / {@code center}；历史值 {@code remote} 由
     * {@link RecordingTargetConverter} 映射为 {@link RecordingTarget#CENTER}。</p>
     */
    private RecordingTarget target = RecordingTarget.LOCAL;

    /**
     * {@code target=local} 且 classpath 存在多个 {@code SecurityEventStore} 时的主 Store id。
     * <p>典型值：{@code mysql}。</p>
     */
    private String primaryStore;

    /**
     * 次要投递目标列表（迁移 shadow 用）；稳定态应为空列表。
     * <p>元素取值同 {@link #target}：{@link RecordingTarget#LOCAL} 或 {@link RecordingTarget#CENTER}。</p>
     */
    private List<RecordingTarget> shadowTargets = new ArrayList<>();

    /**
     * 上报方模块标识，写入 {@code security_event.source_module}。
     * <p>示例：{@code PMS}、{@code MEMBER}、{@code GATEWAY}、{@code SECURITY}。</p>
     */
    private String sourceModule = "unknown";

    /** 按事件大类控制是否上报（{@link #enabled}={@code true} 时生效）。 */
    private Categories categories = new Categories();

    /** BEST_EFFORT 内存队列与 DURABLE file spool 参数。 */
    private Delivery delivery = new Delivery();

    /** MySQL Store 写入并发与事务参数。 */
    private Mysql mysql = new Mysql();

    /** canonical {@code security_event} 过期物理删除策略。 */
    private Retention retention = new Retention();

    /**
     * 按 eventType 覆盖默认优先级，支持 Nacos 热刷新。
     * <p>键：eventType 字符串；值：{@code BEST_EFFORT} 或 {@code DURABLE}。</p>
     */
    private Map<String, String> priorityOverrides = new LinkedHashMap<>();

    /**
     * 是否开启任意形式的安全事件上报。
     */
    public boolean isReportingEnabled() {
        return enabled;
    }

    /**
     * 按事件大类判断是否允许上报；空白或未知类别视为开启（fail-open）。
     *
     * @param category {@link SecurityEventCategoryCodes} 中的分类 code，大小写不敏感
     */
    public boolean isCategoryEnabled(String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        return switch (category.toUpperCase(Locale.ROOT)) {
            case SecurityEventCategoryCodes.AUTH -> categories.auth;
            case SecurityEventCategoryCodes.ACCOUNT -> categories.account;
            case SecurityEventCategoryCodes.CREDENTIAL -> categories.credential;
            case SecurityEventCategoryCodes.ACCESS -> categories.access;
            default -> true;
        };
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

        /** 访问防护类：网关封禁/限流违规等（ACCESS）；通常仅 Gateway 产生。 */
        private boolean access = true;
    }

    /** 投递层：内存队列（BEST_EFFORT）与 file spool（DURABLE）。 */
    @Data
    public static class Delivery {

        /** BEST_EFFORT 有界内存队列与攒批参数。 */
        private MemoryQueueSettings memory = new MemoryQueueSettings();

        /** DURABLE 本地 file spool 目录与重试参数。 */
        private Spool spool = new Spool();
    }

    /** BEST_EFFORT 异步队列参数；队列满时丢弃并计数，不阻塞业务线程。 */
    @Data
    public static class MemoryQueueSettings {

        /** 待投递事件有界队列容量。 */
        private int queueCapacity = 2048;

        /** dispatcher 单次从队列 drain 的条数上限。 */
        private int batchSize = 32;

        /** 攒批 poll 超时（毫秒）；队列空则等待该时长后再次 poll。 */
        private long pollTimeoutMs = 100;

        /** 关闭时等待 worker 排空队列的最长时间（毫秒）。 */
        private long shutdownTimeoutMs = 5000;
    }

    /** DURABLE 事件本地 spool；中心/Store 不可用时保留 claim 并重放。 */
    @Data
    public static class Spool {

        /** spool 根目录。 */
        private String directory = "/ingot-data/security-recording/spool";

        /** 目录总配额（如 {@code 1GB}）。 */
        private String maxBytes = "1GB";

        /** 单个 segment 文件大小上限（如 {@code 64MB}）。 */
        private String segmentBytes = "64MB";

        /** producer 同步等待 spool 接纳 DURABLE 事件的最长时间（毫秒）。 */
        private long durableAckTimeoutMs = 20;

        /** 重放时每批从 spool 读取的条数上限。 */
        private int replayBatchSize = 32;

        /** 重试初始退避（毫秒）。 */
        private long retryInitialMs = 1000;

        /** 重试最大退避（毫秒）。 */
        private long retryMaxMs = 60000;
    }

    /** MySQL {@code SecurityEventStore} 写入侧参数。 */
    @Data
    public static class Mysql {

        /** 并发写入信号量许可数；默认 1 避免热点表写冲突。 */
        private int maxConcurrentWrites = 1;

        /** 单批 INSERT 事务超时（秒）。 */
        private int transactionTimeoutSeconds = 5;
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
         * <p>{@code 0} 表示永久保留。</p>
         */
        private int days = 30;

        /** 单批 {@code DELETE ... LIMIT N} 条数，避免长事务与大锁。 */
        private int batchSize = 500;

        /** 单次定时任务最多连续执行的批次数，防止清理洪峰占用连接过久。 */
        private int maxRounds = 100;

        /** 单次 retention 任务最大执行时长（秒）。 */
        private int maxDurationSeconds = 30;

        /**
         * 写入队列/spool 使用率超过该百分比时，retention 让步暂停删除。
         * <p>避免清理与写入高峰争抢资源。</p>
         */
        private int yieldQueueUsagePercent = 50;
    }
}
