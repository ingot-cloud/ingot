package com.ingot.framework.gateway.rule.client.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import com.ingot.framework.gateway.rule.client.ratelimit.model.EndpointGroup;
import lombok.experimental.UtilityClass;

/**
 * API 路径分组解析器：按 {@code groupCode} 查找 {@link EndpointGroup} 中的路径模式列表。
 *
 * <p>限流规则与挑战策略均可通过 {@code groupCode} 引用分组，避免多条规则重复声明路径。
 * 编译阶段将分组列表转为 {@code Function<String, List<EndpointPattern>>} 供规则 / 策略编译使用。</p>
 *
 * <p>仅返回 {@code enabled=true} 且 {@code code} 非空的分组；未命中或分组无 pattern 时返回 null。</p>
 *
 * @author jy
 * @since 2026/5/28
 */
@UtilityClass
public class GroupPatternResolver {

    /**
     * 从分组列表构建 groupCode → patternList 查找函数。
     *
     * @param groups 分组列表；null 或空时返回恒为 null 的函数
     * @return 接收 groupCode，返回对应路径模式列表；未命中返回 null
     */
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

    /** 空分组列表常量，用于 remote 快照无 groups 时的降级。 */
    public static List<EndpointGroup> emptyGroups() {
        return Collections.emptyList();
    }
}
