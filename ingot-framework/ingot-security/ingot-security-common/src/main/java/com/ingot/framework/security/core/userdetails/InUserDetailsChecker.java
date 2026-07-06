package com.ingot.framework.security.core.userdetails;

import java.time.Duration;
import java.time.LocalDateTime;

import com.ingot.framework.security.core.InSecurityMessageSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

/**
 * 面向 {@link InUser} 的账号状态检查器。
 * <p>
 * 功能与 Spring Security 的 {@code AccountStatusUserDetailsChecker} 对齐，
 * 但在账号已锁定场景下会从 {@link InUser#getMeta()} 中读取
 * {@link InUserMetaKeys#LOCKED_UNTIL}，给出包含"剩余多少分钟"的友好提示。
 * </p>
 *
 * <p>检查顺序保持与框架一致：disabled → account expired → credentials expired → locked。</p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
public class InUserDetailsChecker implements UserDetailsChecker {

    private MessageSourceAccessor messages;

    public InUserDetailsChecker() {
        this(new InSecurityMessageSource());
    }

    public InUserDetailsChecker(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    @Override
    public void check(UserDetails user) {
        if (!user.isEnabled()) {
            throw new DisabledException(messages.getMessage(
                    "AccountStatusUserDetailsChecker.disabled", "User is disabled"));
        }
        if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException(messages.getMessage(
                    "AccountStatusUserDetailsChecker.expired", "User account has expired"));
        }
        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException(messages.getMessage(
                    "AccountStatusUserDetailsChecker.credentialsExpired", "User credentials have expired"));
        }
        if (!user.isAccountNonLocked()) {
            throw new LockedException(resolveLockedMessage(user));
        }
    }

    /**
     * 锁定消息：
     * <ul>
     *   <li>meta 中有 lockedUntil 且在未来 → "请 N 分钟后再尝试"</li>
     *   <li>meta 中没有或已过期 → "永久锁定，请联系管理员"</li>
     * </ul>
     */
    protected String resolveLockedMessage(UserDetails user) {
        if (user instanceof InUser inUser) {
            LocalDateTime lockedUntil = inUser.getMetaValue(InUserMetaKeys.LOCKED_UNTIL, LocalDateTime.class);
            if (lockedUntil != null) {
                long remainingMinutes = Math.max(1L,
                        Duration.between(LocalDateTime.now(), lockedUntil).toMinutes());
                return messages.getMessage(
                        "InUserDetailsChecker.lockedTemporary",
                        new Object[]{remainingMinutes},
                        "Account is locked. Please try again in " + remainingMinutes + " minutes.");
            }
            return messages.getMessage(
                    "InUserDetailsChecker.lockedPermanent",
                    "Account is permanently locked. Please contact the administrator.");
        }
        return messages.getMessage(
                "AccountStatusUserDetailsChecker.locked", "User account is locked");
    }
}
