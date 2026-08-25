package com.ingot.framework.security.credential.actuate;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ingot.framework.cache.source.CacheSourceHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * <p>凭证策略降级可观测端点，暴露当前生效来源与累计降级次数。</p>
 *
 * <p>通过 {@code GET /actuator/credentialpolicy} 查询，供运维判断安全中心是否处于降级态
 * （{@code LAST_KNOWN_GOOD} / {@code LOCAL_FLOOR}）及其发生频次。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 框架另提供 {@code GET /actuator/layeredcache} 汇总所有分层缓存实例；
 *          本端点保留是为了不破坏既有运维与排障文档中的字段与路径。
 */
@Endpoint(id = "credentialpolicy")
@RequiredArgsConstructor
public class CredentialPolicyEndpoint {

    private final CacheSourceHolder sourceHolder;

    /**
     * 返回当前来源与累计降级计数，字段与顺序与迁移前一致。
     *
     * @return 字段顺序固定的观测快照
     */
    @ReadOperation
    public Map<String, Object> source() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentSource", sourceHolder.current());
        result.put("lastKnownGoodCount", sourceHolder.lastKnownGoodCount());
        result.put("localFloorCount", sourceHolder.localFloorCount());
        result.put("lastDegradeAt", sourceHolder.lastDegradeAt());
        return result;
    }
}
