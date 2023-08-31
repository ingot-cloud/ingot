package com.ingot.framework.security.oauth2.server.authorization.authentication;

import com.ingot.framework.core.utils.AssertionUtils;
import com.ingot.framework.security.common.constants.TokenAuthType;
import com.ingot.framework.security.core.IngotSecurityMessageSource;
import com.ingot.framework.security.core.authority.IngotAuthorityUtils;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.core.userdetails.OAuth2UserDetailsServiceManager;
import com.ingot.framework.security.oauth2.core.IngotAuthorizationGrantType;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.server.authorization.client.DefaultRegisteredClientChecker;
import com.ingot.framework.security.oauth2.server.authorization.client.RegisteredClientChecker;
import com.ingot.framework.security.oauth2.server.authorization.client.RegisteredClientOps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.Assert;

import java.util.Set;

import static com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2AuthenticationProviderUtils.getAuthenticatedClientElseThrowInvalidClient;

/**
 * <p>Description  : {@link OAuth2UserDetailsAuthenticationToken} Provider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/9.</p>
 * <p>Time         : 6:11 下午.</p>
 */
@Slf4j
public class OAuth2UserDetailsAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    /**
     * The plaintext password used to perform PasswordEncoder#matches(CharSequence,
     * String)} on when the user is not found to avoid SEC-2056.
     */
    private static final String USER_NOT_FOUND_PASSWORD = "userNotFoundPassword";

    /**
     * The password used to perform {@link PasswordEncoder#matches(CharSequence, String)}
     * on when the user is not found to avoid SEC-2056. This is necessary, because some
     * {@link PasswordEncoder} implementations will short circuit if the password is not
     * in a valid format.
     */
    private volatile String userNotFoundEncodedPassword;

    private OAuth2UserDetailsServiceManager userDetailsServiceManager;
    private UserDetailsPasswordService userDetailsPasswordService;
    private PasswordEncoder passwordEncoder;
    private UserDetailsChecker authenticationChecks = new AccountStatusUserDetailsChecker();
    private RegisteredClientChecker clientChecker = new DefaultRegisteredClientChecker();
    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    public OAuth2UserDetailsAuthenticationProvider() {
        setPasswordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder());
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2UserDetailsAuthenticationToken unauthenticatedToken =
                (OAuth2UserDetailsAuthenticationToken) authentication;

        // 验证 client
        OAuth2ClientAuthenticationToken clientPrincipal =
                getAuthenticatedClientElseThrowInvalidClient(unauthenticatedToken);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        if (registeredClient == null ||
                !registeredClient.getAuthorizationGrantTypes().contains(unauthenticatedToken.getGrantType())) {
            OAuth2ErrorUtils.throwAuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        this.clientChecker.check(registeredClient);
        UserDetails user = retrieveUser(registeredClient, unauthenticatedToken);
        this.authenticationChecks.check(user);
        this.additionalAuthenticationChecks(registeredClient, user, unauthenticatedToken);
        return createSuccessAuthentication(user, unauthenticatedToken);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (OAuth2UserDetailsAuthenticationToken.class.isAssignableFrom(authentication));
    }

    @Override
    protected void doAfterPropertiesSet() {
        Assert.notNull(this.userDetailsServiceManager, "A OAuth2UserDetailsService must be set");
        // 强制使用 IngotSecurityMessageSource，覆盖默认的以及MessageSourceAware接口注入的MessageSource
        setMessageSource(new IngotSecurityMessageSource());
    }

    protected UserDetails retrieveUser(RegisteredClient registeredClient,
                                       OAuth2UserDetailsAuthenticationToken authentication) {
        prepareTimingAttackProtection();
        try {
            UserDetails loadedUser = this.getUserDetailsServiceManager().loadUser(authentication);
            // 填充客户端信息
            TokenAuthType tokenAuthType = RegisteredClientOps.of(registeredClient).getTokenAuthType();
            loadedUser = IngotUser.fillClientInfo((IngotUser) loadedUser,
                    registeredClient.getClientId(), tokenAuthType.getValue());
            return loadedUser;
        } catch (UsernameNotFoundException ex) {
            mitigateAgainstTimingAttack(authentication);
            log.debug("Failed to find user '" + authentication.getName() + "'");
            if (!this.hideUserNotFoundExceptions) {
                throw ex;
            }
            throw new BadCredentialsException(this.messages
                    .getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }
    }

    protected void additionalAuthenticationChecks(RegisteredClient registeredClient,
                                                  UserDetails user,
                                                  OAuth2UserDetailsAuthenticationToken token) {
        // 判断是否可以登陆该客户端
        Set<String> grantClients = IngotAuthorityUtils.extractClientIds(user.getAuthorities());
        boolean grant = grantClients.contains(registeredClient.getClientId());
        AssertionUtils.check(grant, () -> OAuth2ErrorUtils.throwNotAllowClient(this.messages
                .getMessage("OAuth2UserDetailsAuthenticationProvider.notAllowClient",
                        "不允许访问客户端")));

        // 只有密码模式才需要进行密码验证
        if (token.getGrantType() != IngotAuthorizationGrantType.PASSWORD) {
            return;
        }

        if (token.getCredentials() == null) {
            log.debug("Failed to authenticate since no credentials provided");
            throw new BadCredentialsException(this.messages
                    .getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }
        String presentedPassword = token.getCredentials().toString();
        if (!this.passwordEncoder.matches(presentedPassword, user.getPassword())) {
            log.debug("Failed to authenticate since password does not match stored value");
            throw new BadCredentialsException(this.messages
                    .getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }
    }

    protected Authentication createSuccessAuthentication(UserDetails user,
                                                         OAuth2UserDetailsAuthenticationToken authentication) {
        boolean upgradeEncoding = this.userDetailsPasswordService != null
                && this.passwordEncoder.upgradeEncoding(user.getPassword());
        log.debug("[OAuth2UserDetailsAuthenticationProvider] - 是否进行upgradeEncoding={}", upgradeEncoding);
        if (upgradeEncoding) {
            String presentedPassword = authentication.getCredentials().toString();
            String newPassword = this.passwordEncoder.encode(presentedPassword);
            user = this.userDetailsPasswordService.updatePassword(user, newPassword);
        }

        OAuth2UserDetailsAuthenticationToken result =
                OAuth2UserDetailsAuthenticationToken.authenticated(user,
                        user.getPassword(),
                        authentication.getClient(),
                        this.authoritiesMapper.mapAuthorities(user.getAuthorities()));
        result.setDetails(authentication.getDetails());
        return result;
    }

    @Override
    protected final void additionalAuthenticationChecks(UserDetails userDetails,
                                                        UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        throw new RuntimeException("不可调用该方法(additionalAuthenticationChecks)");
    }

    @Override
    protected final UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        throw new RuntimeException("不可调用该方法(retrieveUser)");
    }

    @Override
    protected final Authentication createSuccessAuthentication(Object principal, Authentication authentication, UserDetails user) {
        throw new RuntimeException("不可调用该方法(createSuccessAuthentication)");
    }

    private void prepareTimingAttackProtection() {
        if (this.userNotFoundEncodedPassword == null) {
            this.userNotFoundEncodedPassword = this.passwordEncoder.encode(USER_NOT_FOUND_PASSWORD);
        }
    }

    private void mitigateAgainstTimingAttack(OAuth2UserDetailsAuthenticationToken authentication) {
        if (authentication.getCredentials() != null) {
            String presentedPassword = authentication.getCredentials().toString();
            this.passwordEncoder.matches(presentedPassword, this.userNotFoundEncodedPassword);
        }
    }

    /**
     * Sets the PasswordEncoder instance to be used to encode and validate passwords. If
     * not set, the password will be compared using
     * {@link PasswordEncoderFactories#createDelegatingPasswordEncoder()}
     *
     * @param passwordEncoder must be an instance of one of the {@code PasswordEncoder}
     *                        types.
     */
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");
        this.passwordEncoder = passwordEncoder;
        this.userNotFoundEncodedPassword = null;
    }

    protected PasswordEncoder getPasswordEncoder() {
        return this.passwordEncoder;
    }

    public void setUserDetailsServiceManager(OAuth2UserDetailsServiceManager userDetailsServiceManager) {
        this.userDetailsServiceManager = userDetailsServiceManager;
    }

    protected OAuth2UserDetailsServiceManager getUserDetailsServiceManager() {
        return this.userDetailsServiceManager;
    }

    public void setUserDetailsPasswordService(UserDetailsPasswordService userDetailsPasswordService) {
        this.userDetailsPasswordService = userDetailsPasswordService;
    }

    public void setAuthenticationChecks(UserDetailsChecker authenticationChecks) {
        this.authenticationChecks = authenticationChecks;
    }

    public void setClientChecker(RegisteredClientChecker clientChecker) {
        this.clientChecker = clientChecker;
    }

    protected UserDetailsChecker getAuthenticationChecks() {
        return authenticationChecks;
    }

    public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
        this.authoritiesMapper = authoritiesMapper;
    }
}
