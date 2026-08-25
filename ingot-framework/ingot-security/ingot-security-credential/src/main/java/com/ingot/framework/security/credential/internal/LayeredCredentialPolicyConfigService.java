package com.ingot.framework.security.credential.internal;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import lombok.RequiredArgsConstructor;

/**
 * <p>凭证策略配置的缓存入口，把 {@link CredentialPolicyConfigService} 接到分层缓存链上。</p>
 *
 * <p>单 key 全量快照：{@link #CACHE_KEY} 对应 Redis {@code in:credential:configs:all}。
 * {@link #evictAll()} 清 L1 与 L2，不影响 LKG。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class LayeredCredentialPolicyConfigService implements CredentialPolicyConfigService {

    /**
     * 全量策略的缓存键；与历史 L2 key 后缀 {@code all} 对齐。
     */
    public static final String CACHE_KEY = "all";

    private final LayeredCache<String, List<CredentialPolicyConfigVO>> cache;

    @Override
    public List<CredentialPolicyConfigVO> getAll() {
        List<CredentialPolicyConfigVO> data = cache.get(CACHE_KEY);
        return data != null ? data : List.of();
    }

    @Override
    public void evictAll() {
        cache.evictAll();
    }
}
