package com.ingot.framework.security.core.identity.social;

import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.security.core.InSecurityMessageSource;
import org.springframework.context.support.MessageSourceAccessor;

/**
 * <p>Description  : DefaultUserSocialResolver.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:41.</p>
 */
public class DefaultUserSocialResolver implements UserSocialResolver<Object> {
    private final MessageSourceAccessor messages = InSecurityMessageSource.getAccessor();

    @Override
    public boolean supports(SocialTypeEnum socialType) {
        return true;
    }

    @Override
    public String getUniqueID(String code) {
        throw new UnsupportedOperationException(messages.getMessage("UserSocialResolver.unrealized"));
    }

    @Override
    public Object getUserInfo(String uniqueID) {
        throw new UnsupportedOperationException(messages.getMessage("UserSocialResolver.unrealized"));
    }

    @Override
    public void bind(Object user, String uniqueID) {
        throw new UnsupportedOperationException(messages.getMessage("UserSocialResolver.unrealized"));
    }
}
