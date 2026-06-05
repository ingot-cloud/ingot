package com.ingot.framework.gateway.rule.client.ratelimit;

import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitSnapshot;

/**
 * 限流规则查询 SPI。
 *
 * <p>调用方（网关 / 业务侧）通过本 SPI 获取当前生效的规则快照，
 * 实现可能来自 yaml（local）或 Feign（remote），由各自 AutoConfiguration 装配。</p>
 *
 * <p>SPI 同步返回；具体实现内部应使用 {@code LocalCompiledCache} 维护 L1 缓存，
 * 仅在 cache miss 时拉取远端。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
public interface RateLimitRuleService {

    /**
     * 获取当前生效的规则快照。
     */
    RateLimitSnapshot getSnapshot();

    /**
     * 失效本地 L1 缓存，下次读取重新加载。
     */
    void evictAll();
}
