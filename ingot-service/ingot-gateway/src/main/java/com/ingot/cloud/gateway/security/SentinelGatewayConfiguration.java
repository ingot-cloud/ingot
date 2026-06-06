package com.ingot.cloud.gateway.security;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator;
import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.model.EndpointGroup;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitControlBehavior;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitDimension;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitRule;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitSnapshot;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 把 SDK 的 {@link RateLimitSnapshot} 编译为 Sentinel Gateway 的
 * {@link ApiDefinition} + {@link GatewayFlowRule} 并加载到运行时。
 *
 * <p>装载条件：必须有 {@link RateLimitRuleService} bean（即
 * {@code ingot.security.ratelimit.enabled=true}）；为关时本类
 * 静默 — 不影响现有 Nacos 规则路径。</p>
 *
 * <h3>热更新闭环</h3>
 * <ol>
 *     <li>Platform 改规则 → security-provider 发
 *         {@link com.ingot.cloud.security.api.event.SecurityPolicyInvalidationEvent}</li>
 *     <li>{@link SecurityPolicyCacheCoordinator} 收到事件 → 串行回调本类注册的
 *         {@link #reloadRules()}</li>
 *     <li>{@link #reloadRules()} 内部 <b>先调用 {@code service.evictAll()} 清 L1</b>，
 *         再 {@code getSnapshot()} 重新编译，最后 {@link GatewayRuleManager#loadRules}
 *         全量替换 Sentinel 规则。</li>
 * </ol>
 *
 * <p>由于 {@code RateLimitAutoConfiguration} 中已存在另一个 evictor，
 * Coordinator 会按注册顺序串行触发；本类 reload 内自带 evict 不依赖那条注册顺序，
 * 因此重复 evict 无副作用，保证 remote 模式失效后必拿新快照。</p>
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

    @PostConstruct
    public void registerCoordinator() {
        SecurityPolicyCacheCoordinator coordinator = coordinatorProvider.getIfAvailable();
        if (coordinator == null) {
            return;
        }
        Runnable reload = this::reloadRules;
        coordinator.register(SecurityPolicyDomain.RATE_LIMIT_RULE, reload);
        coordinator.register(SecurityPolicyDomain.ENDPOINT_GROUP, reload);
    }

    @Bean
    public ApplicationRunner sentinelRulesBootstrapRunner() {
        return args -> reloadRules();
    }

    /**
     * 重新从 SDK 拉快照并刷新 Sentinel 规则。
     *
     * <p>内部先调用 {@link RateLimitRuleService#evictAll()} 强制下次取快照穿透
     * L1 缓存，确保 remote 模式下能拿到最新 Feign 结果；接着按 priority 排序
     * 编译并全量替换 Sentinel 当前规则。</p>
     */
    public synchronized void reloadRules() {
        RateLimitRuleService service = rateLimitProvider.getIfAvailable();
        if (service == null) {
            return;
        }
        try {
            service.evictAll();
            RateLimitSnapshot snapshot = service.getSnapshot();
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
                // userId 由 AuthContextRelayFilter 解析 JWT → IdentityResolveFilter 回填 X-User-Id
                param.setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER);
                param.setFieldName(HeaderConstants.X_USER_ID);
            }
            default -> {
                // IP 维度：必须读 IdentityResolveFilter 标准化后的 X-Client-Real-IP，
                // 不能用 Sentinel 的 PARAM_PARSE_STRATEGY_CLIENT_IP（直接取
                // RemoteAddress，反向代理 / K8s Service 后会拿到代理 IP，导致限流粒度错位）。
                param.setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER);
                param.setFieldName(HeaderConstants.CLIENT_REAL_IP);
            }
        }
        r.setParamItem(param);
        return r;
    }
}
