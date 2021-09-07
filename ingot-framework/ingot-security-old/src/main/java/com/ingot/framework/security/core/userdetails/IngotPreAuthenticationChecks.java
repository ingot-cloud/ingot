package com.ingot.framework.security.core.userdetails;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

/**
 * <p>Description  : IngotPreAuthenticationChecks.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-27.</p>
 * <p>Time         : 11:25.</p>
 */
@Slf4j
public class IngotPreAuthenticationChecks implements UserDetailsChecker {
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    @Override public void check(UserDetails user) {
        if (!user.isAccountNonLocked()) {
            log.debug("User account is locked");

            throw new LockedException(messages.getMessage(
                    "AppAuthenticationProvider.locked",
                    "User account is locked"));
        }

        if (!user.isEnabled()) {
            log.debug("User account is disabled");

            throw new DisabledException(messages.getMessage(
                    "AppAuthenticationProvider.disabled",
                    "User is disabled"));
        }

        if (!user.isAccountNonExpired()) {
            log.debug("User account is expired");

            throw new AccountExpiredException(messages.getMessage(
                    "AppAuthenticationProvider.expired",
                    "User account has expired"));
        }
    }
}
