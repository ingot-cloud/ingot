package com.ingot.framework.data.mybatis.scope.authorization;

import com.ingot.framework.security.config.annotation.web.configurers.InHttpConfigurer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

/**
 * <p>把授权快照过滤器挂到 Bearer Token 过滤器之后，以便改写当前 Authentication。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class AuthorizationSnapshotHttpConfigurer extends InHttpConfigurer {

    private final AuthorizationSnapshotAccess snapshotAccess;

    @Override
    public void configure(HttpSecurity builder) {
        AuthorizationSnapshotFilter filter = new AuthorizationSnapshotFilter(snapshotAccess);
        builder.addFilterAfter(postProcess(filter), BearerTokenAuthenticationFilter.class);
    }
}
