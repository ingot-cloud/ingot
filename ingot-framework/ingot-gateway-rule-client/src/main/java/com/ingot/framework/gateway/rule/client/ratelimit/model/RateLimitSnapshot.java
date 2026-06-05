package com.ingot.framework.gateway.rule.client.ratelimit.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 限流规则快照：规则 + 分组定义 + 版本号。
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitSnapshot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<RateLimitRule> rules;

    private List<EndpointGroup> groups;

    /**
     * 单调递增版本号，用于一致性校验。
     */
    private long version;

    public static RateLimitSnapshot empty() {
        return new RateLimitSnapshot(Collections.emptyList(), Collections.emptyList(), 0L);
    }
}
