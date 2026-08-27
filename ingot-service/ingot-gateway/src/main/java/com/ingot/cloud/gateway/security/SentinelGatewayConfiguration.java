package com.ingot.cloud.gateway.security;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.framework.cache.coordinator.CacheRefreshPublisher;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.gateway.rule.client.config.GatewayRuleClientProperties;
import com.ingot.framework.gateway.rule.client.internal.LocalPolicyEnvironmentRefreshListener;
import com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator;
import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>把 SDK 的 {@link RateLimitSnapshot} 编译为 Sentinel Gateway 的
 * {@link ApiDefinition} + {@link GatewayFlowRule} 并加载到运行时。</p>
 *
 * <p>装载条件：必须有 {@link RateLimitRuleService} bean（即
 * {@code ingot.security.ratelimit.enabled=true}）；为关时本类
 * 静默 — 不影响现有 Nacos 规则路径。</p>
 *
 * <h3>四条刷新路径</h3>
 * <ol>
 *     <li><b>失效广播</b>：Platform 改规则 → {@link SecurityPolicyCacheCoordinator} 回调
 *         {@link #reloadRules()}，先 {@code evictAll()} 清缓存再无条件重载，秒级生效。</li>
     *     <li><b>Nacos 热更新</b>：local 模式下 {@code in-security-gateway.yml} 变更 →
 *         {@code EnvironmentChangeEvent} 触发 {@link #reloadRules()}，无需重启。</li>
 *     <li><b>TTL 懒刷新</b>：缓存 TTL 到期后由请求流量触发重新拉取，共享快照层广播刷新事件，
 *         本类比对快照引用后按需重载。这条路径是广播丢失时的兜底。</li>
 *     <li><b>定时兜底</b>：仅当部署中只启用限流域、没有其他域的流量驱动共享快照刷新时才需要，
 *         由 {@code ingot.security.policy.client.cache.refresh-interval} 打开，默认关闭。</li>
 * </ol>
 *
 * <p>后两条路径都走 {@link #reloadIfChanged()}，用<b>快照对象引用</b>判定是否真的变了。
 * 引用比对能直接复用 SDK 派生缓存已有的版本判定结果——版本未变时派生缓存返回同一个对象，
 * 因此这里不需要重复解析版本号，也不会因 TTL 刷新而反复重建 Sentinel 规则造成计数器抖动。</p>
 *
 * <h3>当前限制（已知）</h3>
 * <ul>
 *     <li>{@link EndpointPattern#getMethod()} 暂不参与 Sentinel 编译 —
 *         Sentinel Gateway 的 {@link ApiPathPredicateItem} 不支持 HTTP method 过滤；
 *         字段保留供将来扩展（自定义 ApiPredicate 或 Filter 链）。</li>
 *     <li>Ant 路径（如 {@code /pms/**}）由 {@link SentinelPathPredicateCompiler} 整 pattern
 *         交给 Sentinel PREFIX / {@code AntPathMatcher}，勿截断为 {@code /pms}。</li>
 *     <li>规则 priority 仅用于编译时的稳定排序，不影响 Sentinel 运行期行为
 *         （Sentinel 同 path 多 rule 各自独立计数）。</li>
 * </ul>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class SentinelGatewayConfiguration {

    private final ObjectProvider<RateLimitRuleService> rateLimitProvider;
    private final ObjectProvider<SecurityPolicyCacheCoordinator> coordinatorProvider;
    private final ObjectProvider<CacheRefreshPublisher<SecurityPolicySnapshotVO>> refreshPublisherProvider;
    private final ObjectProvider<GatewayRuleClientProperties> policyPropertiesProvider;
    private final ObjectProvider<LocalPolicyEnvironmentRefreshListener> localRefreshListenerProvider;

    /**
     * 上次已加载进 Sentinel 的快照；用引用比对避免无变化时重复 loadRules。
     */
    private final AtomicReference<RateLimitSnapshot> loaded = new AtomicReference<>();

    private ScheduledExecutorService refreshScheduler;

    @PostConstruct
    public void registerCoordinator() {
        SecurityPolicyCacheCoordinator coordinator = coordinatorProvider.getIfAvailable();
        if (coordinator != null) {
            Runnable reload = this::reloadRules;
            coordinator.register(SecurityPolicyDomain.RATE_LIMIT_RULE, reload);
            coordinator.register(SecurityPolicyDomain.ENDPOINT_GROUP, reload);
        }

        CacheRefreshPublisher<SecurityPolicySnapshotVO> publisher = refreshPublisherProvider.getIfAvailable();
        if (publisher != null) {
            publisher.addListener(vo -> reloadIfChanged());
        }

        LocalPolicyEnvironmentRefreshListener localRefreshListener =
                localRefreshListenerProvider.getIfAvailable();
        if (localRefreshListener != null) {
            localRefreshListener.register("ingot.security.ratelimit.", this::reloadRules);
        }

        startScheduledRefresh();
    }

    @PreDestroy
    public void stopScheduledRefresh() {
        if (refreshScheduler != null) {
            refreshScheduler.shutdownNow();
            refreshScheduler = null;
        }
    }

    @Bean
    public ApplicationRunner sentinelRulesBootstrapRunner() {
        return args -> reloadRules();
    }

    /**
     * 强制重新拉取快照并全量替换 Sentinel 规则。
     *
     * <p>先 {@link RateLimitRuleService#evictAll()} 清掉共享快照与派生缓存，确保 remote 模式下
     * 拿到的是最新的远端结果而不是刚被广播判定为过期的那一份。</p>
     */
    public synchronized void reloadRules() {
        RateLimitRuleService service = rateLimitProvider.getIfAvailable();
        if (service == null) {
            return;
        }
        service.evictAll();
        applySnapshot(service.getSnapshot());
    }

    /**
     * 按需重载：快照对象未变化时直接跳过，不触碰 Sentinel 运行时。
     *
     * <p>供 TTL 懒刷新与定时兜底两条路径调用。不清缓存，因此不会引发额外的远端调用。</p>
     */
    public synchronized void reloadIfChanged() {
        RateLimitRuleService service = rateLimitProvider.getIfAvailable();
        if (service == null) {
            return;
        }
        RateLimitSnapshot snapshot = service.getSnapshot();
        if (snapshot == loaded.get()) {
            return;
        }
        applySnapshot(snapshot);
    }

    private void startScheduledRefresh() {
        GatewayRuleClientProperties properties = policyPropertiesProvider.getIfAvailable();
        Duration interval = properties == null ? null : properties.getCache().getRefreshInterval();
        if (interval == null || interval.isZero() || interval.isNegative()) {
            return;
        }
        refreshScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sentinel-rule-refresh");
            t.setDaemon(true);
            return t;
        });
        refreshScheduler.scheduleWithFixedDelay(this::reloadIfChanged,
                interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
        log.info("[Sentinel] scheduled rule refresh enabled, interval={}", interval);
    }

    private void applySnapshot(RateLimitSnapshot snapshot) {
        try {
            Map<String, EndpointGroup> groupMap = indexGroups(snapshot.getGroups());

            Set<ApiDefinition> apiDefinitions = new HashSet<>();
            Set<GatewayFlowRule> flowRules = new HashSet<>();
            int patternMissingSkipped = 0;

            List<RateLimitRule> ordered = snapshot.getRules() == null ? List.of()
                    : snapshot.getRules().stream()
                    .filter(Objects::nonNull)
                    .filter(RateLimitRule::isEnabled)
                    .sorted(Comparator.comparingInt(RateLimitRule::getPriority))
                    .toList();

            for (RateLimitRule rule : ordered) {
                List<EndpointPattern> patterns = resolvePatterns(rule, groupMap);
                if (patterns == null || patterns.isEmpty()) {
                    patternMissingSkipped++;
                    log.warn("[Sentinel] rule {} has no effective pattern, skip", rule.getCode());
                    continue;
                }
                apiDefinitions.add(buildApiDefinition(rule.getCode(), patterns));
                flowRules.add(buildFlowRule(rule));
            }

            GatewayApiDefinitionManager.loadApiDefinitions(apiDefinitions);
            GatewayRuleManager.loadRules(flowRules);
            loaded.set(snapshot);
            log.info("[Sentinel] reloaded api={} rules={} patternSkipped={} (snapshot version={})",
                    apiDefinitions.size(), flowRules.size(), patternMissingSkipped,
                    snapshot.getVersion());
        } catch (Exception e) {
            log.warn("[Sentinel] reload rules failed", e);
        }
    }

    private static Map<String, EndpointGroup> indexGroups(List<EndpointGroup> groups) {
        Map<String, EndpointGroup> map = new HashMap<>();
        if (groups == null) return map;
        for (EndpointGroup g : groups) {
            if (g.isEnabled() && g.getCode() != null) {
                map.put(g.getCode(), g);
            }
        }
        return map;
    }

    private static List<EndpointPattern> resolvePatterns(RateLimitRule rule,
                                                         Map<String, EndpointGroup> groupMap) {
        if (rule.getGroupCode() != null && !rule.getGroupCode().isBlank()) {
            EndpointGroup g = groupMap.get(rule.getGroupCode());
            if (g != null) {
                return g.getPatternList();
            }
        }
        return rule.getPatternList();
    }

    private static ApiDefinition buildApiDefinition(String code, List<EndpointPattern> patterns) {
        Set<ApiPredicateItem> items = new HashSet<>();
        for (EndpointPattern p : patterns) {
            if (p == null || p.getPath() == null || p.getPath().isBlank()) continue;
            items.add(SentinelPathPredicateCompiler.compile(p.getPath()));
        }
        return new ApiDefinition(code).setPredicateItems(items);
    }

    private static GatewayFlowRule buildFlowRule(RateLimitRule rule) {
        GatewayFlowRule r = new GatewayFlowRule(rule.getCode());
        r.setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME);
        r.setGrade(RuleConstant.FLOW_GRADE_QPS);
        r.setCount(rule.getQps());
        r.setIntervalSec(Math.max(GatewaySecurityConstants.MIN_RATE_LIMIT_INTERVAL_SEC, rule.getIntervalSec()));
        r.setBurst(rule.getBurst());
        r.setControlBehavior(RateLimitControlBehavior.fromCode(rule.getControlBehavior()).isQueue()
                ? RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER
                : RuleConstant.CONTROL_BEHAVIOR_DEFAULT);

        GatewayParamFlowItem param = new GatewayParamFlowItem();
        switch (Objects.requireNonNullElse(rule.getDimension(), RateLimitDimension.IP)) {
            case DEVICE -> {
                param.setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER);
                param.setFieldName(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER);
            }
            case USER -> {
                // userId 由 AuthContextRelayFilter 解析 JWT → IdentityResolveFilter 回填 In-Inner-User-Id
                param.setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER);
                param.setFieldName(HeaderConstants.INNER_USER_ID);
            }
            case CLIENT -> {
                param.setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER);
                param.setFieldName(HeaderConstants.INNER_CLIENT_ID);
            }
            default -> {
                // IP 维度：必须读 IdentityResolveFilter 标准化后的 In-Inner-Client-Real-IP，
                // 不能用 Sentinel 的 PARAM_PARSE_STRATEGY_CLIENT_IP（直接取
                // RemoteAddress，反向代理 / K8s Service 后会拿到代理 IP，导致限流粒度错位）。
                param.setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER);
                param.setFieldName(HeaderConstants.INNER_CLIENT_REAL_IP);
            }
        }
        r.setParamItem(param);
        return r;
    }
}
