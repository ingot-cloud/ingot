package com.ingot.framework.gateway.rule.client.challenge.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import com.ingot.cloud.security.api.model.enums.ChallengeCaptchaType;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;
import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 编译后的挑战策略索引。
 *
 * <p>将每条启用策略的路径模式预编译为 Spring {@link org.springframework.web.util.pattern.PathPattern}，
 * 按 {@link ChallengePolicy#getPriority()} 升序排列；匹配时遍历同 trigger 的策略，
 * 返回首个路径命中的策略。</p>
 *
 * <p>路径来源：优先 {@code groupCode} 关联的分组 patternList，否则使用策略内联 patternList。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
public final class CompiledChallengePolicy {

    private static final CompiledChallengePolicy EMPTY = new CompiledChallengePolicy(List.of());

    private final List<Entry> entries;

    private CompiledChallengePolicy(List<Entry> entries) {
        this.entries = entries;
    }

    /** 空策略索引，match 恒返回 null。 */
    public static CompiledChallengePolicy empty() {
        return EMPTY;
    }

    /**
     * 编译策略列表为可匹配索引。
     *
     * @param policies      原始策略列表
     * @param groupResolver groupCode → patternList 查找函数，来自 {@link com.ingot.framework.gateway.rule.client.internal.GroupPatternResolver}
     */
    public static CompiledChallengePolicy compile(List<ChallengePolicy> policies,
                                                  Function<String, List<EndpointPattern>> groupResolver) {
        if (policies == null || policies.isEmpty()) return EMPTY;
        List<Entry> list = new ArrayList<>();
        for (ChallengePolicy p : policies) {
            if (!p.isEnabled()) continue;
            if (!ChallengeCaptchaType.isSupported(p.getChallengeType())) {
                log.warn("[Challenge] skip policy {} with unsupported challengeType {}",
                        p.getCode(), p.getChallengeType());
                continue;
            }
            List<EndpointPattern> patterns =
                    p.getGroupCode() != null && groupResolver != null
                            ? groupResolver.apply(p.getGroupCode())
                            : p.getPatternList();
            if (patterns == null || patterns.isEmpty()) continue;
            if (coversVcPath(patterns)) {
                log.warn("[Challenge] skip policy {} because patterns cover {}",
                        p.getCode(), ChallengeTypes.VC_PATH_PREFIX);
                continue;
            }
            list.add(new Entry(p, PathMatcher.compile(patterns)));
        }
        list.sort(Comparator.comparingInt(e -> e.policy.getPriority()));
        return new CompiledChallengePolicy(list);
    }

    /**
     * 按路径 + HTTP 方法 + 触发类型查找匹配策略。
     *
     * @return 首个命中的策略；无匹配返回 null
     */
    public ChallengePolicy match(String path, org.springframework.http.HttpMethod method,
                                 ChallengeTrigger trigger) {
        for (Entry e : entries) {
            if (e.policy.getTrigger() != trigger) continue;
            if (PathMatcher.matches(e.patterns, path, method)) {
                return e.policy;
            }
        }
        return null;
    }

    /**
     * 按路径 + HTTP 方法 + PassToken scope 查找匹配策略，忽略 trigger。
     *
     * @param path   请求路径
     * @param method HTTP 方法，可为 null
     * @param scope  策略 {@link ChallengePolicy#getScope()}；空则返回 null
     * @return 首个 path 命中且 scope 相等的已编译策略；无匹配返回 null
     */
    public ChallengePolicy matchByScope(String path, org.springframework.http.HttpMethod method, String scope) {
        if (scope == null || scope.isBlank()) {
            return null;
        }
        for (Entry e : entries) {
            if (!scope.equals(e.policy.getScope())) {
                continue;
            }
            if (PathMatcher.matches(e.patterns, path, method)) {
                return e.policy;
            }
        }
        return null;
    }

    /** 返回所有已编译（启用且有有效路径）的策略列表。 */
    public List<ChallengePolicy> all() {
        List<ChallengePolicy> result = new ArrayList<>(entries.size());
        for (Entry e : entries) result.add(e.policy);
        return result;
    }

    private static boolean coversVcPath(List<EndpointPattern> patterns) {
        for (EndpointPattern pattern : patterns) {
            if (pattern == null) {
                continue;
            }
            if (ChallengeTypes.isVcPath(pattern.getPath())) {
                return true;
            }
        }
        return false;
    }

    private record Entry(ChallengePolicy policy, List<PathMatcher.CompiledPattern> patterns) {
    }
}
