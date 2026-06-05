package com.ingot.framework.gateway.rule.client.challenge.model;

import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 挑战策略 POJO。
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengePolicy implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String code;

    private String groupCode;

    private List<EndpointPattern> patternList;

    private ChallengeTrigger trigger;

    /**
     * SLIDER / IMAGE / SMS / EMAIL。
     */
    private String challengeType;

    private int passTokenTtlSec;

    private int passTokenRemaining;

    private String scope;

    private boolean enabled;

    private int priority;
}
