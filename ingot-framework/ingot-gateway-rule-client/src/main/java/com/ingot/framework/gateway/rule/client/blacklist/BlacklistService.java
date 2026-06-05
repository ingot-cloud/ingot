package com.ingot.framework.gateway.rule.client.blacklist;

import com.ingot.framework.gateway.rule.client.blacklist.model.IpKeyType;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListSnapshot;

/**
 * 黑白名单查询 SPI。
 *
 * <p>实现负责把 DB / yaml 中的名单条目编译成可高效查询的索引（IP set / CIDR / UA / Referer 正则）。
 * 网关热路径仅调用 {@link #isBlocked} / {@link #isWhitelisted}，避免线性扫描。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
public interface BlacklistService {

    /**
     * 是否在黑名单中（任一维度匹配即返回 true）。
     *
     * @param ip      客户端 IP（{@code X-Client-Real-IP}），可能为 null
     * @param device  设备指纹（{@code X-In-Ca-Sig}），可能为 null
     * @param userId  用户 ID（{@code X-User-Id}），可能为 null
     * @param ua      User-Agent，可能为 null
     * @param referer Referer，可能为 null
     */
    boolean isBlocked(String ip, String device, String userId, String ua, String referer);

    /**
     * 是否在白名单中（任一维度匹配即返回 true）。
     */
    boolean isWhitelisted(String ip, String device, String userId, String ua, String referer);

    /**
     * 精确判断某个键是否在指定类型的名单中（用于运营查询）。
     */
    boolean contains(IpKeyType keyType, String keyValue, boolean blacklist);

    /**
     * 当前快照。
     */
    IpListSnapshot getSnapshot();

    /**
     * 失效本地 L1 编译缓存，下次读取重新加载。
     */
    void evictAll();
}
