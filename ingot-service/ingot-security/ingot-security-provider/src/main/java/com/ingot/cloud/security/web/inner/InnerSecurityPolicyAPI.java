package com.ingot.cloud.security.web.inner;

import com.ingot.cloud.security.api.model.dto.BlacklistReportDTO;
import com.ingot.cloud.security.api.model.vo.policy.ChallengePolicyVO;
import com.ingot.cloud.security.api.model.vo.policy.EndpointGroupVO;
import com.ingot.cloud.security.api.model.vo.policy.IpListItemVO;
import com.ingot.cloud.security.api.model.vo.policy.RateLimitRuleVO;
import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.cloud.security.model.domain.GatewayBlacklistEvent;
import com.ingot.cloud.security.service.policy.SecurityPolicyAdminService;
import com.ingot.cloud.security.service.policy.SecurityPolicySnapshot;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 安全策略内部接口（仅内网调用）。
 *
 * <p>提供策略快照查询与网关自动封禁事件上报。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping("/inner/security")
@RequiredArgsConstructor
public class InnerSecurityPolicyAPI implements RShortcuts {

    private final SecurityPolicyAdminService policyService;

    @GetMapping("/policy/snapshot")
    public R<SecurityPolicySnapshotVO> snapshot() {
        SecurityPolicySnapshot snap = policyService.snapshot();
        SecurityPolicySnapshotVO vo = new SecurityPolicySnapshotVO();
        vo.setVersion(snap.version());
        vo.setGroups(snap.groups().stream().map(e -> {
            EndpointGroupVO v = new EndpointGroupVO();
            BeanUtils.copyProperties(e, v);
            v.setEnabled(Boolean.TRUE.equals(e.getEnabled()));
            return v;
        }).toList());
        vo.setRateLimitRules(snap.rules().stream().map(e -> {
            RateLimitRuleVO v = new RateLimitRuleVO();
            BeanUtils.copyProperties(e, v);
            v.setEnabled(Boolean.TRUE.equals(e.getEnabled()));
            v.setQps(orZero(e.getQps()));
            v.setBurst(orZero(e.getBurst()));
            v.setIntervalSec(orZero(e.getIntervalSec()));
            v.setPriority(orZero(e.getPriority()));
            return v;
        }).toList());
        vo.setIpList(snap.ipList().stream().map(e -> {
            IpListItemVO v = new IpListItemVO();
            BeanUtils.copyProperties(e, v);
            v.setEnabled(Boolean.TRUE.equals(e.getEnabled()));
            return v;
        }).toList());
        vo.setChallengePolicies(snap.challengePolicies().stream().map(e -> {
            ChallengePolicyVO v = new ChallengePolicyVO();
            BeanUtils.copyProperties(e, v);
            v.setEnabled(Boolean.TRUE.equals(e.getEnabled()));
            v.setPriority(orZero(e.getPriority()));
            return v;
        }).toList());
        return ok(vo);
    }

    @PostMapping("/blacklist/report")
    public R<Void> reportBlacklist(@RequestBody BlacklistReportDTO dto) {
        GatewayBlacklistEvent event = new GatewayBlacklistEvent();
        BeanUtils.copyProperties(dto, event);
        policyService.recordEvent(event);
        return ok();
    }

    private static int orZero(Integer i) {
        return i == null ? 0 : i;
    }
}
