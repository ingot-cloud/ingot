package com.ingot.framework.gateway.rule.client.challenge.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 挑战策略快照。
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeSnapshot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<ChallengePolicy> policies;

    private long version;

    public static ChallengeSnapshot empty() {
        return new ChallengeSnapshot(Collections.emptyList(), 0L);
    }
}
