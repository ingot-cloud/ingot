package com.ingot.cloud.iam.web.inner;

import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotRequest;
import com.ingot.cloud.iam.authorization.snapshot.LocalAuthorizationSnapshotLoader;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>内部授权快照只使用认证传递的成员上下文，并从新模型求值精确 ACTION。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping("/inner/authorization")
@RequiredArgsConstructor
public class InnerAuthorizationAPI implements RShortcuts {
    private final LocalAuthorizationSnapshotLoader snapshots;

    /**
     * 返回当前认证用户的授权快照。
     *
     * @param request 可选身份；与上下文不一致时 403
     * @return 快照
     */
    @PostMapping("/snapshot")
    public R<AuthorizationSnapshotDTO> snapshot(@RequestBody(required = false) AuthorizationSnapshotRequest request) {
        return ok(snapshots.assemble(
                request == null ? null : request.getTenantId(),
                request == null ? null : request.getUserId()));
    }
}
