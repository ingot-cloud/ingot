package com.ingot.framework.cache.actuate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ingot.framework.cache.registry.LayeredCacheDescriptor;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * <p>分层缓存汇总观测端点，一次列出进程内所有缓存实例的层次开关与当前降级态。</p>
 *
 * <p>通过 {@code GET /actuator/layeredcache} 访问。各模块原有的专属端点
 * （{@code credentialpolicy}、{@code securitypolicy}、{@code loginfailurepolicy}）继续保留，
 * 本端点是增量的横向视图，便于故障时一次看清哪些缓存正在降级。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see LayeredCacheRegistry
 */
@Endpoint(id = "layeredcache")
@RequiredArgsConstructor
public class LayeredCacheEndpoint {

    private final LayeredCacheRegistry registry;

    @ReadOperation
    public Map<String, Object> caches() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (LayeredCacheDescriptor d : registry.all()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", d.name());
            item.put("l1Enabled", d.l1Enabled());
            item.put("l2Enabled", d.l2Enabled());
            item.put("resilienceEnabled", d.resilienceEnabled());
            item.put("localFloorEnabled", d.localFloorEnabled());
            item.put("currentSource", d.sourceHolder().current());
            item.put("lastKnownGoodCount", d.sourceHolder().lastKnownGoodCount());
            item.put("localFloorCount", d.sourceHolder().localFloorCount());
            item.put("lastDegradeAt", d.sourceHolder().lastDegradeAt());
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", items.size());
        result.put("caches", items);
        return result;
    }
}
