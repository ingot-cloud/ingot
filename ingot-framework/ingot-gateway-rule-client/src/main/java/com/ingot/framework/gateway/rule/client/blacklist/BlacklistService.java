package com.ingot.framework.gateway.rule.client.blacklist;

import com.ingot.framework.gateway.rule.client.blacklist.model.IpKeyType;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListSnapshot;

/**
 * 黑白名单查询 SPI。
 *
 * <p>实现负责把 DB / yaml 中的名单条目编译成可高效查询的索引
 *（精确集合 / CIDR 段 / UA·Referer 正则），网关热路径仅调用
 * {@link #isBlocked} / {@link #isWhitelisted}，避免线性扫描。</p>
 *
 * <h3>实现与装配</h3>
 * <ul>
 *     <li>{@code policy.mode=local} — {@link com.ingot.framework.gateway.rule.client.blacklist.internal.LocalBlacklistService}</li>
 *     <li>{@code policy.mode=remote} — {@link com.ingot.framework.gateway.rule.client.blacklist.internal.RemoteBlacklistService}</li>
 * </ul>
 *
 * <h3>配置开关</h3>
 * <p>需 {@code ingot.security.blacklist.enabled=true} 才会装配实现类。
 * yaml 示例见 {@link com.ingot.framework.gateway.rule.client.blacklist.config.BlacklistProperties}。</p>
 *
 * <p>注意：Redis 临时封禁（{@code TempBlockStore}）由网关侧独立处理，不经过本 SPI。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
public interface BlacklistService {

    /**
     * 是否在黑名单中（任一维度匹配即返回 true）。
     * <p>匹配顺序：精确 IP → 设备 → 用户 → CIDR → UA 正则 → Referer 正则。</p>
     *
     * @param ip      客户端 IP（{@code X-Client-Real-IP}），可能为 null
     * @param device  设备指纹（{@code X-In-Ca-Sig}），可能为 null
     * @param userId  用户 ID（{@code X-User-Id}），可能为 null
     * @param ua      User-Agent，可能为 null
     * @param referer Referer，可能为 null
     * @return 命中黑名单返回 true
     */
    boolean isBlocked(String ip, String device, String userId, String ua, String referer);

    /**
     * 是否在白名单中（任一维度匹配即返回 true）。
     * <p>白名单命中后网关跳过后续黑名单检查、挑战策略与 Sentinel 限流。</p>
     */
    boolean isWhitelisted(String ip, String device, String userId, String ua, String referer);

    /**
     * 精确判断某个键是否在指定类型的名单中（用于运营查询 / 管理端校验）。
     *
     * @param keyType   键类型（IP / CIDR / DEVICE / USER / USER_AGENT / REFERER）
     * @param keyValue  键值
     * @param blacklist true 查黑名单，false 查白名单
     */
    boolean contains(IpKeyType keyType, String keyValue, boolean blacklist);

    /**
     * 获取当前名单快照（原始条目 + 版本号），不触发重新编译。
     */
    IpListSnapshot getSnapshot();

    /**
     * 失效本地 L1 编译缓存，下次查询将重新从 yaml 或远端加载并编译索引。
     * <p>由 Coordinator 在收到 {@code IP_LIST} / {@code ALL} 失效事件时调用。</p>
     */
    void evictAll();
}
