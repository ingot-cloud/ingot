package com.ingot.framework.cache.coordinator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.InvalidationEvent;
import com.ingot.framework.eventbus.Subscription;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>跨节点失效广播的订阅与派发中心，按事件携带的域把清理动作分发给已注册的 evictor。</p>
 *
 * <p>同一个域允许注册<b>多个</b> evictor（例如缓存清理与规则重载各注册一个），按注册顺序串行执行，
 * 单个 evictor 抛异常只记日志，不影响同域其余回调与其他域。</p>
 *
 * <p>发布方收不到自己广播的事件（bus 已按 origin 过滤回环），因此写侧必须在广播前先清理本地缓存，
 * 不能依赖本协调器。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * LayeredCacheCoordinator<DictInvalidationEvent, String> coordinator =
 *         new LayeredCacheCoordinator<>(bus, DictInvalidationEvent.class,
 *                 DictInvalidationEvent::getCode, null);
 * coordinator.register("user_status", cache::evictAll);
 * }</pre>
 *
 * @param <E> 失效事件类型
 * @param <D> 域标识类型
 * @author jy
 * @since 1.0.0
 * @apiNote 子类可固化泛型参数以对外保持稳定的具体类型，例如网关的
 *          {@code SecurityPolicyCacheCoordinator}。
 */
@Slf4j
public class LayeredCacheCoordinator<E extends InvalidationEvent, D> {

    private final InvalidationBus bus;
    private final Class<E> eventType;
    private final Function<E, D> domainExtractor;
    private final D allDomain;
    private final Map<D, List<Runnable>> evictors = new LinkedHashMap<>();

    private Subscription subscription;

    /**
     * @param bus             失效广播总线
     * @param eventType       订阅的事件类
     * @param domainExtractor 从事件中提取域标识
     * @param allDomain       表示「全部域」的标识；{@code null} 域值也按全部处理
     */
    public LayeredCacheCoordinator(InvalidationBus bus,
                                   Class<E> eventType,
                                   Function<E, D> domainExtractor,
                                   D allDomain) {
        this.bus = bus;
        this.eventType = eventType;
        this.domainExtractor = domainExtractor;
        this.allDomain = allDomain;
    }

    /**
     * 注册一个域的清理回调；同域多次调用按顺序追加，后注册者不覆盖先注册者。
     *
     * @param domain  域标识
     * @param evictor 清理动作
     */
    public synchronized void register(D domain, Runnable evictor) {
        evictors.computeIfAbsent(domain, d -> new ArrayList<>()).add(evictor);
        log.info("[Cache] evictor registered, domain={}, total={}", domain, evictors.get(domain).size());
    }

    /**
     * 订阅失效广播，启动后开始接收跨节点事件。
     */
    @PostConstruct
    public void start() {
        this.subscription = bus.subscribe(eventType, this::handle);
        log.info("[Cache] coordinator subscribed, event={}", eventType.getSimpleName());
    }

    /**
     * 取消订阅，容器销毁时自动调用。
     */
    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    /**
     * 处理一次失效事件：按域派发；域为空或等于「全部域」时回调所有已注册项。
     *
     * @param event 失效事件
     */
    protected void handle(E event) {
        D domain = domainExtractor.apply(event);
        log.info("[Cache] invalidate, domain={} origin={}", domain, event.getOrigin());
        if (domain == null || domain.equals(allDomain)) {
            snapshotEvictors().forEach(this::runAll);
            return;
        }
        List<Runnable> list = snapshotEvictors().get(domain);
        if (list != null && !list.isEmpty()) {
            runAll(domain, list);
        }
    }

    private synchronized Map<D, List<Runnable>> snapshotEvictors() {
        return new LinkedHashMap<>(evictors);
    }

    private void runAll(D domain, List<Runnable> list) {
        for (Runnable r : list) {
            try {
                r.run();
            } catch (Exception e) {
                log.warn("[Cache] evict failed, domain={}", domain, e);
            }
        }
    }
}
