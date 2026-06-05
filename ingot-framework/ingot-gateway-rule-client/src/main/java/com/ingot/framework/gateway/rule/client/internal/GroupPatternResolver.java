package com.ingot.framework.gateway.rule.client.internal;

import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import com.ingot.framework.gateway.rule.client.ratelimit.model.EndpointGroup;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 按 {@code groupCode} 解析 {@link EndpointGroup} 中的路径模式。
 *
 * @author jy
 * @since 2026/5/28
 */
public final class GroupPatternResolver {

    private GroupPatternResolver() {
    }

    public static Function<String, List<EndpointPattern>> fromGroups(List<EndpointGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return code -> null;
        }
        Map<String, List<EndpointPattern>> map = groups.stream()
                .filter(g -> g.getCode() != null && g.isEnabled())
                .collect(Collectors.toMap(EndpointGroup::getCode,
                        g -> g.getPatternList() == null ? List.of() : g.getPatternList(),
                        (a, b) -> a));
        return code -> {
            List<EndpointPattern> patterns = map.get(code);
            return patterns == null || patterns.isEmpty() ? null : patterns;
        };
    }

    public static List<EndpointGroup> emptyGroups() {
        return Collections.emptyList();
    }
}
