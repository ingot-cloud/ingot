package com.ingot.framework.gateway.rule.client.blacklist.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * IP / 设备 / UA 等维度的黑白名单快照。
 *
 * <p>由 {@link com.ingot.framework.gateway.rule.client.blacklist.BlacklistService} 提供；
 * 网关 {@code BlacklistFilter} 在请求早期按 {@link IpListItem} 逐条匹配，
 * 命中黑名单返回 403，命中白名单则跳过后续黑名单、挑战与 Sentinel 限流检查。</p>
 *
 * <p>local 模式条目来自
 * {@link com.ingot.framework.gateway.rule.client.blacklist.config.BlacklistProperties}；
 * remote 模式经 Inner Feign 拉取后由
 * {@link com.ingot.framework.gateway.rule.client.internal.SnapshotAssembler} 组装，
 * 并已按 {@code effective_at} / {@code expires_at} 过滤掉未生效或已过期记录。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IpListSnapshot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前生效的所有名单条目（已按 effective_at / expires_at 过滤，且仅含 enabled 项）。
     * 每条对应 {@link IpListItem}，涵盖 IP、CIDR、设备指纹、UA 正则等匹配维度。
     * 空列表表示无静态黑白名单规则（临时封禁由 Redis 独立维护，不纳入本快照）。
     */
    private List<IpListItem> items;

    /**
     * 单调递增版本号，用于一致性校验与跨节点失效广播。
     * Platform 改名单后版本递增，各网关节点据此 evict 本地 L1 缓存并重新拉取。
     * {@code 0} 表示初始空快照。
     */
    private long version;

    public static IpListSnapshot empty() {
        return new IpListSnapshot(Collections.emptyList(), 0L);
    }
}
