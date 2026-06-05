package com.ingot.framework.gateway.rule.client.challenge.internal;

import java.util.concurrent.atomic.AtomicLong;

import com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.config.ChallengeProperties;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeSnapshot;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;
import com.ingot.framework.gateway.rule.client.internal.GroupPatternResolver;
import com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

/**
 * local 模式挑战策略服务。
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@RequiredArgsConstructor
public class LocalChallengePolicyService implements ChallengePolicyService {

    private final ChallengeProperties properties;
    private final LocalCompiledCache<CompiledChallengePolicy> cache = new LocalCompiledCache<>();
    private final AtomicLong version = new AtomicLong();

    @Override
    public ChallengePolicy match(String requestPath, HttpMethod method, ChallengeTrigger trigger) {
        return resolve().match(requestPath, method, trigger);
    }

    @Override
    public ChallengeSnapshot getSnapshot() {
        return new ChallengeSnapshot(properties.getPolicy().getPolicies(), version.get());
    }

    @Override
    public void evictAll() {
        cache.evictAll();
        log.debug("[Challenge] local policies evicted");
    }

    private CompiledChallengePolicy resolve() {
        return cache.get(() -> {
            long v = version.incrementAndGet();
            CompiledChallengePolicy c = CompiledChallengePolicy.compile(
                    properties.getPolicy().getPolicies(),
                    GroupPatternResolver.fromGroups(properties.getPolicy().getGroups()));
            log.info("[Challenge] local policies compiled, size={} version={}",
                    c.all().size(), v);
            return c;
        });
    }
}
