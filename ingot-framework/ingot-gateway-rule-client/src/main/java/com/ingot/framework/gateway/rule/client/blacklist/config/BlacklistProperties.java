package com.ingot.framework.gateway.rule.client.blacklist.config;

import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.gateway.rule.client.blacklist.model.IpListItem;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 黑白名单域配置。
 *
 * <p>配置前缀：{@code ingot.security.blacklist}。</p>
 *
 * <h3>典型配置 — local 模式（本机调试）</h3>
 *
 * <pre>{@code
 * ingot:
 *   security:
 *     blacklist:
 *       enabled: true
 *       policy:
 *         mode: local
 *         items:
 *           # 黑名单 IP → 403 FORBIDDEN_BLOCKED
 *           - list-type: BLACK          # BLACK 或 black；DB 短码 B/W 仅 remote 模式由 SDK 解析
 *             key-type: IP             # 枚举全名；DB 短码 IP/DV/UI/CD/UA/RF 亦可
 *             key-value: "203.0.113.10"
 *             enabled: true
 *           # 黑名单 CIDR 段
 *           - list-type: BLACK
 *             key-type: CIDR
 *             key-value: "10.0.0.0/24"
 *             enabled: true
 *           # 黑名单设备指纹（Header In-Ca-Sig）
 *           - list-type: BLACK
 *             key-type: DEVICE          # DB 短码 DV
 *             key-value: "abc-device-hash"
 *             enabled: true
 *           # UA 正则（Pattern.find 子串匹配）
 *           - list-type: BLACK
 *             key-type: USER_AGENT      # DB 短码 UA
 *             key-value: "(?i)badbot"
 *             enabled: true
 *           # 白名单：跳过后续黑名单检查、挑战、Sentinel 限流
 *           - list-type: WHITE         # DB 短码 W
 *             key-type: IP
 *             key-value: "198.51.100.1"
 *             enabled: true
 *           # 定时生效（可选）
 *           - list-type: BLACK
 *             key-type: IP
 *             key-value: "192.0.2.1"
 *             enabled: true
 *             effective-at: "2026-06-01T00:00:00"
 *             expires-at: "2026-12-31T23:59:59"
 * }</pre>
 *
 * <h3>典型配置 — remote 模式（生产 / 多节点）</h3>
 *
 * <pre>{@code
 * ingot:
 *   security:
 *     policy:
 *       client:
 *         enabled: true
 *         invalidation-enabled: true    # Platform 改名单后跨节点热更新
 *     blacklist:
 *       enabled: true
 *       policy:
 *         mode: remote                  # 规则由 Platform /inner/security/policy/snapshot 拉取
 * }</pre>
 *
 * <h3>与 Redis 临时封禁的关系</h3>
 * <p>限流违规达阈值（默认 60s 内 30 次）后，{@code TempBlockStore} 写入
 * {@code in:gw:bl:tmp:IP:{ip}}，TTL 默认 15 分钟；{@code BlacklistFilter} 在静态名单
 * 之前先查临时封禁。临时封禁<b>不依赖</b>本开关，但静态名单需 {@code enabled=true}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.security.blacklist")
public class BlacklistProperties {

    /**
     * 黑白名单域总开关。
     * <ul>
     *     <li>{@code false}（默认）：不装配 {@code BlacklistService}；静态名单不生效。</li>
     *     <li>{@code true}：装配 SDK + {@link com.ingot.cloud.gateway.security.BlacklistFilter}。</li>
     * </ul>
     * <p>关闭后 {@link com.ingot.cloud.gateway.security.TempBlockStore} 临时封禁仍由
     * {@link com.ingot.cloud.gateway.security.SentinelBlockHandler} 写入，
     * {@link com.ingot.cloud.gateway.security.BlacklistFilter} 仍会检查 Redis 临时封禁。</p>
     */
    private boolean enabled = false;

    /** 黑白名单加载配置：模式 + local 模式下的名单条目列表。 */
    private Policy policy = new Policy();

    /**
     * 黑白名单加载配置。
     */
    @Getter
    @Setter
    public static class Policy {
        /**
         * 加载模式：
         * <ul>
         *     <li>{@link Mode#LOCAL}（默认）— 读下方 {@link #items} yaml 配置</li>
         *     <li>{@link Mode#REMOTE} — Feign 拉 ingot-service-security 快照，
         *         DB {@code gateway_ip_list} 的短码由 SDK {@code IpKeyType.fromCode} 解析</li>
         * </ul>
         */
        private Mode mode = Mode.LOCAL;

        /**
         * local 模式下的名单条目；remote 模式下被忽略。
         * 黑/白通过 {@link IpListItem#getListType()} 区分。
         */
        private List<IpListItem> items = new ArrayList<>();
    }

    /**
     * 黑白名单加载模式。
     */
    public enum Mode {
        /**
         * 从本机 yaml {@link Policy#getItems()} 加载。
         * 适合本机调试 / 单实例；DB 短码（B/W、IP/DV 等）在 local 模式下也可写枚举全名。
         */
        LOCAL,
        /**
         * 从 ingot-service-security 远端快照加载（快照字段 {@code ipList}）。
         * 适合生产 / 多节点；DB {@code gateway_ip_list} 短码由 SDK {@code fromCode} 解析。
         */
        REMOTE
    }
}
