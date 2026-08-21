package com.ingot.cloud.security.web.inner;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.SessionConcurrencyPolicyVO;
import com.ingot.cloud.security.model.domain.SessionConcurrencyPolicy;
import com.ingot.cloud.security.service.session.SessionConcurrencyPolicyAdminService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>并发会话策略 Inner API，Auth 侧分层缓存的远端数据源。</p>
 *
 * <p>返回策略全量（含 {@code enabled=false} 的记录），由 Auth 自行过滤与匹配：
 * 完整快照才能被安全地写入 LKG，只下发启用项会让「停用某条策略」在降级期表现为策略丢失。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping("/inner/security/session")
@RequiredArgsConstructor
public class InnerSessionConcurrencyPolicyAPI implements RShortcuts {

    private final SessionConcurrencyPolicyAdminService adminService;

    @GetMapping("/concurrency-policies")
    public R<List<SessionConcurrencyPolicyVO>> listPolicies() {
        return ok(adminService.list().stream().map(this::toVo).toList());
    }

    private SessionConcurrencyPolicyVO toVo(SessionConcurrencyPolicy entity) {
        SessionConcurrencyPolicyVO vo = new SessionConcurrencyPolicyVO();
        vo.setId(entity.getId());
        vo.setScope(entity.getScope());
        vo.setClientId(entity.getClientId());
        vo.setUserType(entity.getUserType());
        vo.setMaxSessions(entity.getMaxSessions());
        vo.setDimension(entity.getDimension());
        vo.setOverflow(entity.getOverflow());
        vo.setAdminForbidConcurrent(entity.getAdminForbidConcurrent());
        vo.setEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
