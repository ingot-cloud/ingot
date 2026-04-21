package com.ingot.framework.security.core.credential;

import com.ingot.framework.security.core.InSecurityMessageSource;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.core.userdetails.InUserMetaKeys;
import com.ingot.framework.security.oauth2.core.InAuthorizationGrantType;
import com.ingot.framework.security.oauth2.core.OAuth2Authentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

/**
 * 默认凭证检查器
 * <p>
 * 负责 PASSWORD 授权模式下的密码验证。这是框架内置的基础实现，
 * 服务层可继承本类并在 {@code super.check()} 调用后追加更多策略检查
 * （如密码过期、强制改密等），注册为 Spring Bean 后框架会自动替换本默认实现。
 * </p>
 *
 * <p>
 * 密码错误时，会根据 {@link InUser#getMeta()} 中的
 * {@link InUserMetaKeys#FAILED_LOGIN_COUNT}、
 * {@link InUserMetaKeys#MAX_FAILED_ATTEMPTS}、
 * {@link InUserMetaKeys#HINT_AFTER_ATTEMPTS} 分级返回消息：
 * </p>
 * <ul>
 *   <li>前几次：返回通用"用户名或密码错误"，避免暴露用户存在性</li>
 *   <li>从第 {@code hintAfterAttempts} 次（含本次）开始：返回"再错 X 次将锁定"的具体提示</li>
 *   <li>若 meta 中缺失上述任一字段：退回通用提示</li>
 * </ul>
 *
 * @author jymot
 * @see UserCredentialChecker
 * @since 2026-02-13
 */
@Slf4j
public class DefaultUserCredentialChecker implements UserCredentialChecker {

    /**
     * 未从 meta 拿到 hintAfterAttempts 时的兜底默认：从第 3 次开始给出详细提示
     */
    private static final int DEFAULT_HINT_AFTER_ATTEMPTS = 3;

    protected MessageSourceAccessor messages;

    private PasswordEncoder passwordEncoder;

    public DefaultUserCredentialChecker() {
        setPasswordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder());
        setMessageSource(new InSecurityMessageSource());
    }

    /**
     * 设置 PasswordEncoder
     */
    @Override
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 设置 MessageSource
     */
    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * 仅 PASSWORD 授权模式执行密码验证，其他模式直接通过。
     */
    @Override
    public void check(UserDetails user, OAuth2Authentication token) {
        if (token.getGrantType() != InAuthorizationGrantType.PASSWORD) {
            return;
        }

        if (token.getCredentials() == null) {
            log.debug("Failed to authenticate since no credentials provided");
            throw new BadCredentialsException(defaultBadCredentialsMessage());
        }

        String presentedPassword = token.getCredentials().toString();
        if (!this.passwordEncoder.matches(presentedPassword, user.getPassword())) {
            log.debug("Failed to authenticate since password does not match stored value");
            throw new BadCredentialsException(resolveBadCredentialsMessage(user));
        }
    }

    /**
     * 根据 {@link InUser#getMeta()} 分级返回密码错误消息。
     * <p>
     * 本次失败后的"即将达到的失败次数"为 {@code failedLoginCount + 1}；
     * 若未达到 {@code hintAfterAttempts}，返回通用提示；否则返回"再错 X 次将锁定"。
     * </p>
     */
    protected String resolveBadCredentialsMessage(UserDetails user) {
        if (!(user instanceof InUser inUser)) {
            return defaultBadCredentialsMessage();
        }
        Integer failed = inUser.getMetaValue(InUserMetaKeys.FAILED_LOGIN_COUNT, Integer.class);
        Integer max = inUser.getMetaValue(InUserMetaKeys.MAX_FAILED_ATTEMPTS, Integer.class);
        if (failed == null || max == null || max <= 0) {
            return defaultBadCredentialsMessage();
        }

        Integer hintAfter = inUser.getMetaValue(InUserMetaKeys.HINT_AFTER_ATTEMPTS, Integer.class);
        int hintThreshold = hintAfter != null && hintAfter > 0 ? hintAfter : DEFAULT_HINT_AFTER_ATTEMPTS;

        int attemptsAfterThisFail = failed + 1;
        int remaining = max - attemptsAfterThisFail;
        // 剩余次数 <= 0 意味着"本次失败就会触发自动锁定"；
        // 此时仍由 badCredentials 返回，锁定动作由 RecordLoginFailure 异步落地，
        // 下次登录时会命中 locked 分支给出锁定消息。
        if (remaining <= 0 || attemptsAfterThisFail < hintThreshold) {
            return defaultBadCredentialsMessage();
        }
        return messages.getMessage(
                "AbstractUserDetailsAuthenticationProvider.badCredentialsWithHint",
                new Object[]{remaining},
                "Incorrect password. Your account will be locked after " + remaining + " more failed attempts.");
    }

    private String defaultBadCredentialsMessage() {
        return messages.getMessage(
                "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials");
    }

    protected PasswordEncoder getPasswordEncoder() {
        return this.passwordEncoder;
    }
}
