package com.ingot.framework.security.core.userdetails;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

/**
 * <p>Description  : IngotPostAuthenticationChecks.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-27.</p>
 * <p>Time         : 11:27.</p>
 */
@Slf4j
public class IngotPostAuthenticationChecks implements UserDetailsChecker {
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    @Override public void check(UserDetails user) {
        if (!user.isCredentialsNonExpired()) {
            log.debug("User account credentials have expired");

            throw new CredentialsExpiredException(messages.getMessage(
                    "AppAuthenticationProvider.credentialsExpired",
                    "User credentials have expired"));
        }
    }
}
