package com.ingot.framework.gateway.rule.client.blacklist.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 黑白名单快照。
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IpListSnapshot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前生效的所有名单条目（已按 effective_at/expires_at 过滤）。
     */
    private List<IpListItem> items;

    private long version;

    public static IpListSnapshot empty() {
        return new IpListSnapshot(Collections.emptyList(), 0L);
    }
}
