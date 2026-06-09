package com.ingot.framework.gateway.rule.client.challenge.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;

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
                                                  java.util.function.Function<String, List<com.ingot.framework.gateway.rule.client.model.EndpointPattern>> groupResolver) {
        if (policies == null || policies.isEmpty()) return EMPTY;
        List<Entry> list = new ArrayList<>();
        for (ChallengePolicy p : policies) {
            if (!p.isEnabled()) continue;
            List<com.ingot.framework.gateway.rule.client.model.EndpointPattern> patterns =
                    p.getGroupCode() != null && groupResolver != null
                            ? groupResolver.apply(p.getGroupCode())
                            : p.getPatternList();
            if (patterns == null || patterns.isEmpty()) continue;
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

    /** 返回所有已编译（启用且有有效路径）的策略列表。 */
    public List<ChallengePolicy> all() {
        List<ChallengePolicy> result = new ArrayList<>(entries.size());
        for (Entry e : entries) result.add(e.policy);
        return result;
    }

    private record Entry(ChallengePolicy policy, List<PathMatcher.CompiledPattern> patterns) {
    }
}
