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
 * 挑战策略服务 — local 模式实现。
 *
 * <p>激活条件：{@code ingot.security.challenge.enabled=true} 且
 * {@code ingot.security.challenge.policy.mode=local}（默认）。</p>
 *
 * <p>从 {@link com.ingot.framework.gateway.rule.client.challenge.config.ChallengeProperties.Policy}
 * 读取 yaml 策略与分组，编译为 {@link CompiledChallengePolicy} 并缓存到
 * {@link LocalCompiledCache}。</p>
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

    /** 按路径 + 方法 + 触发类型匹配策略；委托 {@link CompiledChallengePolicy#match}。 */
    @Override
    public ChallengePolicy match(String requestPath, HttpMethod method, ChallengeTrigger trigger) {
        return resolve().match(requestPath, method, trigger);
    }

    /** 返回 yaml 原始策略列表 + 进程内版本号。 */
    @Override
    public ChallengeSnapshot getSnapshot() {
        return new ChallengeSnapshot(properties.getPolicy().getPolicies(), version.get());
    }

    /** 清空 L1 编译缓存，下次 match 重新从 yaml 编译。 */
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
