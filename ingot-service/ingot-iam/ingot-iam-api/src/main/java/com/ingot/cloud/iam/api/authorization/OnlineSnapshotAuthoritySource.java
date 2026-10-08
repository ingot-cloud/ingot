package com.ingot.cloud.iam.api.authorization;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.framework.commons.constants.PermissionConstants;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.oauth2.server.resource.access.expression.TrustedAuthoritySource;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <p>消费服务从当前在线快照取得接口资格；仅同一请求复用，不使用数据热缓存或 JWT 角色。</p>
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class OnlineSnapshotAuthoritySource implements TrustedAuthoritySource {
    private static final String REQUEST_SNAPSHOT = OnlineSnapshotAuthoritySource.class.getName() + ".snapshot";
    private final java.util.function.BiFunction<Long, Long, AuthorizationSnapshotDTO> loader;

    /** {@inheritDoc} */
    @Override
    public boolean requiresPasswordChange() {
        return Boolean.TRUE.equals(snapshot().getPasswordChangeRequired());
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> currentAuthorities() {
        var snapshot = snapshot();
        if (Boolean.TRUE.equals(snapshot.getPasswordChangeRequired())) return Set.of(PermissionConstants.INIT_PASSWORD);
        var context = SecurityAuthContext.getUser().getAuthorizationContext();
        var result = snapshot.getPermissionCodes().stream()
                .filter(code -> !RoleConstants.ROLE_ADMIN_CODE.equals(code))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (context.domain() == AuthorizationDomain.PLATFORM && snapshot.isPlatformAdministrator())
            result.add(RoleConstants.ROLE_ADMIN_CODE);
        return Set.copyOf(result);
    }

    private AuthorizationSnapshotDTO snapshot() {
        var user = SecurityAuthContext.getUser();
        var context = user == null ? null : user.getAuthorizationContext();
        if (context == null || user.getId() == null) throw new AuthorizationDeniedException(IamReasonCode.IDENTITY_INVALID.getCode());
        long tenant = context.tenantId() == null ? 0 : Long.parseLong(context.tenantId());
        var attributes = RequestContextHolder.getRequestAttributes();
        var request = attributes instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
        var snapshot = request == null ? null : (AuthorizationSnapshotDTO) request.getAttribute(REQUEST_SNAPSHOT);
        if (snapshot == null) snapshot = loader.apply(tenant, user.getId());
        if (snapshot == null || snapshot.getPermissionCodes() == null || snapshot.getPasswordChangeRequired() == null
                || snapshot.getExpiresAt() == null || !Instant.now().isBefore(snapshot.getExpiresAt())
                || !user.getId().equals(snapshot.getUserId()) || !Long.valueOf(tenant).equals(snapshot.getTenantId())) {
            throw new AuthorizationDeniedException(IamReasonCode.AUTHORIZATION_UNAVAILABLE.getCode());
        }
        if (request != null) request.setAttribute(REQUEST_SNAPSHOT, snapshot);
        return snapshot;
    }
}
