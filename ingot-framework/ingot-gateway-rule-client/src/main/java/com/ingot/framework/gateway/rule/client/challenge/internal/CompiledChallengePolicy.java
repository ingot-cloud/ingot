package com.ingot.framework.gateway.rule.client.challenge.internal;

import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 编译后的挑战策略：path -> PathPattern 已预编译，匹配按 priority 排序。
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

    public static CompiledChallengePolicy empty() {
        return EMPTY;
    }

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

    public List<ChallengePolicy> all() {
        List<ChallengePolicy> result = new ArrayList<>(entries.size());
        for (Entry e : entries) result.add(e.policy);
        return result;
    }

    private record Entry(ChallengePolicy policy, List<PathMatcher.CompiledPattern> patterns) {
    }
}
