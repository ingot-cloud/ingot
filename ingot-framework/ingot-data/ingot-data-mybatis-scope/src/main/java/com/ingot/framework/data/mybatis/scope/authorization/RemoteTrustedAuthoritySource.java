package com.ingot.framework.data.mybatis.scope.authorization;

import java.time.Instant;
import java.util.Set;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.oauth2.server.resource.access.expression.TrustedAuthoritySource;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDeniedException;

/**
 * <p>消费服务的接口注解直接取得本次在线快照，不复用数据范围热缓存或 JWT 角色。</p>
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class RemoteTrustedAuthoritySource implements TrustedAuthoritySource {
    private final AuthorizationSnapshotLoader loader;

    /** {@inheritDoc} */
    @Override
    public Set<String> currentAuthorities() {
        var user = SecurityAuthContext.getUser();
        var context = user == null ? null : user.getAuthorizationContext();
        if (context == null || user.getId() == null) throw new AuthorizationDeniedException("IdentityInvalid");
        var snapshot = loader.load(context.tenantId() == null ? 0 : Long.parseLong(context.tenantId()), user.getId());
        if (snapshot == null || snapshot.getPermissionCodes() == null || snapshot.getExpiresAt() == null
                || !Instant.now().isBefore(snapshot.getExpiresAt()) || !user.getId().equals(snapshot.getUserId())
                || snapshot.getTenantId() != (context.tenantId() == null ? 0 : Long.parseLong(context.tenantId()))) {
            throw new AuthorizationDeniedException("AuthorizationUnavailable");
        }
        var result = snapshot.getPermissionCodes().stream()
                .filter(code -> !RoleConstants.ROLE_ADMIN_CODE.equals(code))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        if (context.domain() == AuthorizationDomain.PLATFORM && snapshot.isPlatformAdministrator())
            result.add(RoleConstants.ROLE_ADMIN_CODE);
        return Set.copyOf(result);
    }
}
