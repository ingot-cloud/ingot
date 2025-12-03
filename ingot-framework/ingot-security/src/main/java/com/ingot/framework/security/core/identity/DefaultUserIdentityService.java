package com.ingot.framework.security.core.identity;

import java.util.List;

import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DefaultUserIdentityService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/2.</p>
 * <p>Time         : 08:31.</p>
 */
@RequiredArgsConstructor
public class DefaultUserIdentityService implements UserIdentityService {
    private final List<UserIdentityResolver> resolvers;

    @Override
    public UserDetailsResponse loadUser(UserDetailsRequest request) {
        return resolvers.stream()
                .filter(r -> r.supports(request.getType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported user identity type"))
                .load(request);
    }
}
