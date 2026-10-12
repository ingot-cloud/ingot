package com.ingot.framework.security.core.userdetails;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.ingot.framework.commons.model.security.CredentialExpirationReason;
import com.ingot.framework.security.core.InSecurityMessageSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

/**
 * <p>面向 {@link InUser} 的账号状态检查器，按凭证过期原因选择当前配置文案。</p>
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
    private final CredentialExpirationMessageResolver expirationMessages;

    /** 创建使用内置安全文案的账号状态检查器。 */
    public InUserDetailsChecker() {
        this(new InSecurityMessageSource());
    }

    /** 使用指定消息资源检查账号状态，未配置自定义过期文案时使用该资源。 */
    public InUserDetailsChecker(MessageSource messageSource) {
        this(messageSource, new CredentialExpirationMessageResolver(new StandardEnvironment()));
    }

    /** 保留指定安全资源，并在凭证过期时按原因解析当前环境文案。 */
    public InUserDetailsChecker(MessageSource messageSource, CredentialExpirationMessageResolver expirationMessages) {
        this.messages = new MessageSourceAccessor(messageSource);
        this.expirationMessages = expirationMessages;
    }

    /** 同步认证 Provider 的安全消息资源，不替换动态文案解析器。 */
    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /** 检查启用、账号有效期、凭证有效期及锁定状态，失败时抛对应认证异常。 */
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
            throw new CredentialsExpiredException(expirationMessages.resolve(expirationReason(user), messages));
        }
        if (!user.isAccountNonLocked()) {
            throw new LockedException(resolveLockedMessage(user));
        }
    }

    private CredentialExpirationReason expirationReason(UserDetails user) {
        if (user instanceof InUser inUser && inUser.getMeta() != null) {
            Object rawReason = inUser.getMeta().get(InUserMetaKeys.CREDENTIAL_EXPIRATION_REASON);
            if (rawReason instanceof String value) {
                try {
                    return CredentialExpirationReason.getEnum(value);
                } catch (IllegalArgumentException ignored) {
                    // 滚动发布期间未知字面量只降级提示，不改变凭证过期阻断。
                }
            }
        }
        return CredentialExpirationReason.UNKNOWN;
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
                        Duration.between(LocalDateTime.now(ZoneOffset.UTC), lockedUntil).toMinutes());
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
