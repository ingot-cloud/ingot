package com.ingot.framework.gateway.rule.client.challenge.internal;

import java.util.Collections;
import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.ChallengePolicyVO;
import com.ingot.cloud.security.api.model.vo.policy.EndpointPatternVO;
import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeSnapshot;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;
import com.ingot.framework.gateway.rule.client.internal.GroupPatternResolver;
import com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SnapshotAssembler;
import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import com.ingot.framework.gateway.rule.client.ratelimit.model.EndpointGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

/**
 * 挑战策略服务 — remote 模式实现。
 *
 * <p>激活条件：{@code ingot.security.challenge.enabled=true} 且
 * {@code ingot.security.challenge.policy.mode=remote}。</p>
 *
 * <p>通过 {@link RemoteSnapshotFetcher} 拉取全量快照中的 {@code challengePolicies} 与
 * {@code groups}，转换为 {@link ChallengePolicy} 后编译为 {@link CompiledChallengePolicy}
 * 并缓存到 {@link LocalCompiledCache}。不支持的 trigger 类型跳过并打 warn。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteChallengePolicyService implements ChallengePolicyService {

    private final RemoteSnapshotFetcher fetcher;
    private final LocalCompiledCache<Compiled> cache = new LocalCompiledCache<>();

    /** 按路径 + 方法 + 触发类型匹配策略；cache miss 时拉取远端并编译。 */
    @Override
    public ChallengePolicy match(String requestPath, HttpMethod method, ChallengeTrigger trigger) {
        return resolve().compiled.match(requestPath, method, trigger);
    }

    /** 返回远端快照原始策略列表 + 版本号。 */
    @Override
    public ChallengeSnapshot getSnapshot() {
        return resolve().snapshot;
    }

    /** 清空 L1 缓存，下次 match 重新拉取远端并编译。 */
    @Override
    public void evictAll() {
        cache.evictAll();
        log.debug("[Challenge] remote policies evicted");
    }

    private Compiled resolve() {
        return cache.get(() -> {
            SecurityPolicySnapshotVO vo = fetcher.fetch();
            List<ChallengePolicyVO> rawList = vo == null || vo.getChallengePolicies() == null
                    ? Collections.emptyList() : vo.getChallengePolicies();
            List<ChallengePolicy> policies = rawList.stream()
                    .map(RemoteChallengePolicyService::toPolicy)
                    .filter(RemoteChallengePolicyService::isActivePolicy)
                    .toList();
            List<EndpointGroup> groups = vo == null || vo.getGroups() == null
                    ? GroupPatternResolver.emptyGroups()
                    : vo.getGroups().stream().map(SnapshotAssembler::toGroup).toList();
            ChallengeSnapshot snap = new ChallengeSnapshot(policies, vo == null ? 0 : vo.getVersion());
            CompiledChallengePolicy c = CompiledChallengePolicy.compile(policies,
                    GroupPatternResolver.fromGroups(groups));
            log.info("[Challenge] remote policies compiled, size={} version={}",
                    c.all().size(), snap.getVersion());
            return new Compiled(snap, c);
        });
    }

    private static ChallengePolicy toPolicy(ChallengePolicyVO v) {
        try {
            return ChallengePolicy.builder()
                    .id(v.getId())
                    .code(v.getCode())
                    .groupCode(v.getGroupCode())
                    .patternList(v.getPatternList() == null ? List.of()
                            : v.getPatternList().stream().map(RemoteChallengePolicyService::toPattern).toList())
                    .trigger(ChallengeTrigger.parse(v.getTrigger()))
                    .challengeType(v.getChallengeType())
                    .passTokenTtlSec(orZero(v.getPassTokenTtlSec()))
                    .passTokenRemaining(orZero(v.getPassTokenRemaining()))
                    .scope(v.getScope())
                    .enabled(v.isEnabled())
                    .priority(v.getPriority())
                    .build();
        } catch (IllegalArgumentException ex) {
            log.warn("[Challenge] skip policy {} with unsupported trigger {}", v.getCode(), v.getTrigger());
            return null;
        }
    }

    private static boolean isActivePolicy(ChallengePolicy policy) {
        return policy != null && policy.isEnabled() && policy.getCode() != null;
    }

    private static EndpointPattern toPattern(EndpointPatternVO v) {
        return EndpointPattern.of(v.getPath(), v.getMethod());
    }

    private static int orZero(Integer i) {
        return i == null ? 0 : i;
    }

    /** 缓存条目：原始快照 + 编译后的策略索引。 */
    private record Compiled(ChallengeSnapshot snapshot, CompiledChallengePolicy compiled) {
    }
}
