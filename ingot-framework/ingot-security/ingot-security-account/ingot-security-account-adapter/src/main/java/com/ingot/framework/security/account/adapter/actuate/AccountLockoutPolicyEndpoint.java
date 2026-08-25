package com.ingot.framework.security.account.adapter.actuate;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ingot.framework.cache.source.CacheSourceHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * <p>账号锁定策略降级可观测端点。</p>
 *
 * <p>通过 {@code GET /actuator/accountlockoutpolicy} 查询当前策略来源
 * （{@code REMOTE} / {@code LAST_KNOWN_GOOD} / {@code LOCAL_FLOOR}）及累计降级次数。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Endpoint(id = "accountlockoutpolicy")
@RequiredArgsConstructor
public class AccountLockoutPolicyEndpoint {

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
