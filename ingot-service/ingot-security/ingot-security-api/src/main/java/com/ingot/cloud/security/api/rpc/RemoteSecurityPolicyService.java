package com.ingot.cloud.security.api.rpc;

import com.ingot.cloud.security.api.model.dto.BlacklistReportDTO;
import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 安全策略中心 Feign 接口（内网）。
 *
 * <p>网关通过本接口拉取规则全量快照、上报自动封禁事件。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@FeignClient(contextId = "RemoteSecurityPolicyService", value = ServiceNameConstants.SECURITY_SERVICE)
public interface RemoteSecurityPolicyService {

    /**
     * 拉取当前生效的策略快照（限流规则 + 分组 + 黑白名单 + 挑战策略）。
     */
    @GetMapping("/inner/security/policy/snapshot")
    R<SecurityPolicySnapshotVO> snapshot();

    /**
     * 上报自动封禁/解封/续期事件，由 ingot-security 落 {@code gateway_blacklist_event}。
     */
    @PostMapping("/inner/security/blacklist/report")
    R<Void> reportBlacklist(@RequestBody BlacklistReportDTO dto);
}
