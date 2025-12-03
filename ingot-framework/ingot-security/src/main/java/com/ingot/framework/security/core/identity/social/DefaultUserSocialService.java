package com.ingot.framework.security.core.identity.social;

import java.util.List;

import com.ingot.framework.commons.error.IllegalOperationException;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.security.core.InSecurityMessageSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.MessageSourceAccessor;

/**
 * <p>Description  : DefaultSocialResolverService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:06.</p>
 */
@RequiredArgsConstructor
public class DefaultUserSocialService implements UserSocialService {
    private final List<UserSocialResolver<?>> socialProcessorList;
    private final MessageSourceAccessor messages = InSecurityMessageSource.getAccessor();

    @Override
    @SuppressWarnings({"rawtypes"})
    public String getUniqueID(SocialTypeEnum socialType, String code) {
        for (UserSocialResolver processor : socialProcessorList) {
            if (processor.supports(socialType)) {
                return processor.getUniqueID(code);
            }
        }
        throw new IllegalOperationException(
                messages.getMessage("SocialResolverService.SocialTypeIllegal"));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T getUserInfo(SocialTypeEnum socialType, String uniqueID) {
        for (UserSocialResolver processor : socialProcessorList) {
            if (processor.supports(socialType)) {
                return (T) processor.getUserInfo(uniqueID);
            }
        }
        throw new IllegalOperationException(
                messages.getMessage("SocialResolverService.SocialTypeIllegal"));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void bind(SocialTypeEnum socialType, T user, String uniqueID) {
        for (UserSocialResolver processor : socialProcessorList) {
            if (processor.supports(socialType)) {
                processor.bind(user, uniqueID);
                return;
            }
        }
        throw new IllegalOperationException(
                messages.getMessage("SocialResolverService.SocialTypeIllegal"));
    }
}
