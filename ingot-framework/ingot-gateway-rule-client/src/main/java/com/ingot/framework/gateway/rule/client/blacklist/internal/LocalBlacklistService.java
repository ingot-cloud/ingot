package com.ingot.framework.gateway.rule.client.blacklist.internal;

import java.util.concurrent.atomic.AtomicLong;

import com.ingot.framework.cache.derived.LazyDerivedCache;
import com.ingot.framework.gateway.rule.client.blacklist.BlacklistService;
import com.ingot.framework.gateway.rule.client.blacklist.config.BlacklistProperties;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpKeyType;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListSnapshot;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>黑白名单服务 — local 模式实现。</p>
 *
 * <p>激活条件：{@code ingot.security.blacklist.enabled=true} 且
 * {@code ingot.security.blacklist.policy.mode=local}（默认）。</p>
 *
 * <p>从 {@link BlacklistProperties.Policy#getItems()} 读取 yaml 名单条目，
 * 编译为 {@link CompiledIpList} 索引并缓存到 {@link LazyDerivedCache}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
public class LocalBlacklistService implements BlacklistService {

    private final BlacklistProperties properties;
    private final LazyDerivedCache<CompiledIpList> cache;
    private final AtomicLong version = new AtomicLong();

    public LocalBlacklistService(BlacklistProperties properties) {
        this.properties = properties;
        this.cache = new LazyDerivedCache<>(this::compile);
    }

    /** 委托 {@link CompiledIpList#isBlocked} 做黑名单匹配。 */
    @Override
    public boolean isBlocked(String ip, String device, String userId, String clientId, String ua, String referer) {
        return resolve().isBlocked(ip, device, userId, clientId, ua, referer);
    }

    /** 委托 {@link CompiledIpList#isWhitelisted} 做白名单匹配。 */
    @Override
    public boolean isWhitelisted(String ip, String device, String userId, String clientId, String ua, String referer) {
        return resolve().isWhitelisted(ip, device, userId, clientId, ua, referer);
    }

    /** 委托 {@link CompiledIpList#contains} 做精确键查询。 */
    @Override
    public boolean contains(IpKeyType keyType, String keyValue, boolean blacklist) {
        return resolve().contains(keyType, keyValue, blacklist);
    }

    /** 返回 yaml 原始条目 + 进程内版本号（不触发重新编译）。 */
    @Override
    public IpListSnapshot getSnapshot() {
        return new IpListSnapshot(properties.getPolicy().getItems(), version.get());
    }

    /** 清空 L1 编译索引，下次查询重新从 yaml 编译。 */
    @Override
    public void evictAll() {
        cache.evictAll();
        log.debug("[Blacklist] local index evicted");
    }

    private CompiledIpList resolve() {
        return cache.get();
    }

    private CompiledIpList compile() {
        CompiledIpList compiled = CompiledIpList.compile(properties.getPolicy().getItems());
        long v = version.incrementAndGet();
        log.info("[Blacklist] local index compiled, size={} version={}", compiled.size(), v);
        return compiled;
    }
}
