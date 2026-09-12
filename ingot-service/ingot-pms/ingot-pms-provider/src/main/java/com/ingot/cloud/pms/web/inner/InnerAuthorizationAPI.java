package com.ingot.cloud.pms.web.inner;

import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationSnapshotRequest;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotAccess;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>内部授权快照接口，只使用认证传递的租户与用户。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping("/inner/authorization")
@RequiredArgsConstructor
public class InnerAuthorizationAPI implements RShortcuts {

    private final AuthorizationSnapshotAccess snapshotAccess;

    /**
     * 返回当前认证用户的授权快照。
     *
     * @param request 可选身份；与上下文不一致时 403
     * @return 快照
     */
    @PostMapping("/snapshot")
    public R<AuthorizationSnapshotDTO> snapshot(@RequestBody(required = false) AuthorizationSnapshotRequest request) {
        InUser user = SecurityAuthContext.getUser();
        if (user == null || user.getId() == null || user.getTenantId() == null) {
            throw new AuthorizationDeniedException("AuthorizationDenied");
        }
        if (request != null) {
            if (request.getTenantId() != null && !request.getTenantId().equals(user.getTenantId())) {
                throw new AuthorizationDeniedException("AuthorizationDenied");
            }
            if (request.getUserId() != null && !request.getUserId().equals(user.getId())) {
                throw new AuthorizationDeniedException("AuthorizationDenied");
            }
        }
        return ok(snapshotAccess.require(user.getTenantId(), user.getId()));
    }
}
