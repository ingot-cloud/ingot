package com.ingot.framework.security.credential.actuate;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ingot.framework.security.credential.internal.CredentialPolicySourceHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * 凭证策略降级可观测端点，暴露当前生效来源与累计降级次数。
 *
 * <p>通过 {@code GET /actuator/credentialpolicy} 查询，供运维判断安全中心是否处于降级态
 * （{@code LAST_KNOWN_GOOD} / {@code LOCAL_FLOOR}）及其发生频次。仅当类路径存在 Spring Boot Actuator
 * 时装配。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Endpoint(id = "credentialpolicy")
@RequiredArgsConstructor
public class CredentialPolicyEndpoint {

    private final CredentialPolicySourceHolder sourceHolder;

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
