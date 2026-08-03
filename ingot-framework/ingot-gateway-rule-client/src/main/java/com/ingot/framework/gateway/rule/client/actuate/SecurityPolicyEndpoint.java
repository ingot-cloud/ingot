package com.ingot.framework.gateway.rule.client.actuate;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ingot.framework.cache.source.CacheSourceHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * <p>网关安全策略快照降级可观测端点。</p>
 *
 * <p>通过 {@code GET /actuator/securitypolicy} 查询当前快照来源
 * （{@code REMOTE} / {@code LAST_KNOWN_GOOD} / {@code LOCAL_FLOOR}）及累计降级次数。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 框架另提供 {@code GET /actuator/layeredcache} 汇总所有分层缓存实例；
 *          本端点保留是为了不破坏既有运维与排障文档中的字段与路径。
 */
@Endpoint(id = "securitypolicy")
@RequiredArgsConstructor
public class SecurityPolicyEndpoint {

    private final CacheSourceHolder sourceHolder;

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
