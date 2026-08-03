package com.ingot.framework.gateway.rule.client.internal;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;

/**
 * <p>监听 Spring Cloud 配置刷新事件，在 Nacos / 本地 yaml 热更新后触发 local 模式策略的缓存失效。</p>
 *
 * <p>{@link org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder}
 * 会在配置变更时重绑定 {@code @ConfigurationProperties}，但 local 模式的 Service 使用
 * {@link com.ingot.framework.cache.derived.LazyDerivedCache} 缓存编译产物，必须显式
 * {@code evictAll()} 才能读到新配置。各域 AutoConfiguration 按属性前缀注册刷新回调；
 * 限流域还需在网关侧额外调用 Sentinel 规则重载。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class LocalPolicyEnvironmentRefreshListener {

    private final List<RefreshTarget> targets = new CopyOnWriteArrayList<>();

    /**
     * 注册一个属性前缀与对应的刷新动作。
     *
     * @param propertyPrefix 配置键前缀，如 {@code ingot.security.ratelimit.}
     * @param refresher      刷新回调，通常为 {@code service::evictAll} 或 Sentinel reload
     */
    public void register(String propertyPrefix, Runnable refresher) {
        if (propertyPrefix == null || propertyPrefix.isBlank() || refresher == null) {
            return;
        }
        targets.add(new RefreshTarget(propertyPrefix, refresher));
        log.info("[SecurityPolicy] local config refresh registered, prefix={}", propertyPrefix);
    }

    @EventListener
    public void onEnvironmentChange(EnvironmentChangeEvent event) {
        if (event == null || event.getKeys() == null || event.getKeys().isEmpty()) {
            return;
        }
        Set<String> keys = event.getKeys();
        for (RefreshTarget target : targets) {
            if (matches(keys, target.propertyPrefix)) {
                log.info("[SecurityPolicy] local config refresh triggered, prefix={}, keys={}",
                        target.propertyPrefix, keys);
                try {
                    target.refresher.run();
                } catch (Exception e) {
                    log.warn("[SecurityPolicy] local config refresh failed, prefix={}",
                            target.propertyPrefix, e);
                }
            }
        }
    }

    private static boolean matches(Set<String> keys, String prefix) {
        for (String key : keys) {
            if (key != null && key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private record RefreshTarget(String propertyPrefix, Runnable refresher) {
    }
}
