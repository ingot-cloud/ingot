package com.ingot.framework.security.access.actuate;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ingot.framework.cache.source.CacheSourceHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * <p>登录失败保护策略降级可观测端点。</p>
 *
 * <p>通过 {@code GET /actuator/loginfailurepolicy} 查询当前策略来源
 * （{@code REMOTE} / {@code LAST_KNOWN_GOOD} / {@code LOCAL_FLOOR}）及累计降级次数。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 框架另提供 {@code GET /actuator/layeredcache} 汇总所有分层缓存实例；
 *          本端点保留是为了不破坏既有运维与排障文档中的字段与路径。
 */
@Endpoint(id = "loginfailurepolicy")
@RequiredArgsConstructor
public class LoginFailurePolicyEndpoint {

    private final CacheSourceHolder sourceHolder;

    /**
     * 返回当前来源与累计降级计数。
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
