package com.ingot.framework.security.core.identity;

import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserIdentityTypeEnum;
import com.ingot.framework.security.core.InSecurityMessageSource;
import org.springframework.context.support.MessageSourceAccessor;

/**
 * <p>Description  : DefaultUserIdentityResolver.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:49.</p>
 */
public class DefaultUserIdentityResolver implements UserIdentityResolver {
    private final MessageSourceAccessor messages = InSecurityMessageSource.getAccessor();

    @Override
    public boolean supports(UserIdentityTypeEnum type) {
        return true;
    }

    @Override
    public UserDetailsResponse load(UserDetailsRequest request) {
        throw new UnsupportedOperationException(messages.getMessage("UserIdentityResolver.unrealized"));
    }
}
