package com.ingot.framework.security.access.actuate;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ingot.framework.security.access.internal.LoginFailurePolicySourceHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * 登录失败保护策略降级可观测端点。
 *
 * <p>通过 {@code GET /actuator/loginfailurepolicy} 查询当前策略来源及累计降级次数。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Endpoint(id = "loginfailurepolicy")
@RequiredArgsConstructor
public class LoginFailurePolicyEndpoint {

    private final LoginFailurePolicySourceHolder sourceHolder;

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
