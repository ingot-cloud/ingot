package com.ingot.framework.security.oauth2.server.authorization.authentication;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.common.TenantMainDTO;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * <p>
 * Description : OAuth2PreAuthorizationCodeRequestAuthenticationToken.
 * </p>
 * <p>
 * Author : wangchao.
 * </p>
 * <p>
 * Date : 2023/7/26.
 * </p>
 * <p>
 * Time : 2:49 PM.
 * </p>
 */
public class OAuth2PreAuthorizationCodeRequestAuthenticationToken extends AbstractAuthenticationToken {

    /** 已验证的用户主体。 */
    private final Object principal;

    @Getter
    /** 与预授权绑定的OAuth协议参数。 */
    private final Map<String, Object> additionalParameters;

    @Getter
    /** 未认证请求使用的注册客户端。 */
    private final RegisteredClient registeredClient;

    @Getter
    /** 预授权凭据。 */
    private final String preAuthorization;

    @Getter
    /** 允许选择的候选租户。 */
    private final List<TenantMainDTO> allowList;

    @Getter
    /** 认证窗口初始寿命秒数，恢复时不重新计时。 */
    private final long timeToLive;

    /** 预授权窗口的 UTC 截止秒数，序列化读取后不得重新计时。 */
    @Getter
    private final long authenticationExpiresAt;

    /** 创建空的未认证请求。 */
    public static OAuth2PreAuthorizationCodeRequestAuthenticationToken unauthenticated() {
        return new OAuth2PreAuthorizationCodeRequestAuthenticationToken("", "", null, null);
    }

    /** 创建绑定客户端和协议参数的未认证请求。 */
    public static OAuth2PreAuthorizationCodeRequestAuthenticationToken unauthenticated(Object userPrincipal,
            String preAuthorization, RegisteredClient registeredClient, Map<String, Object> additionalParameters) {
        return new OAuth2PreAuthorizationCodeRequestAuthenticationToken(userPrincipal, preAuthorization,
                registeredClient, additionalParameters);
    }

    /** 凭据验证成功后开始固定的预授权窗口。 */
    public static OAuth2PreAuthorizationCodeRequestAuthenticationToken authenticated(Object userPrincipal,
            List<TenantMainDTO> allowList, Map<String, Object> additionalParameters, long timeToLive) {
        return new OAuth2PreAuthorizationCodeRequestAuthenticationToken(userPrincipal, additionalParameters, allowList,
                timeToLive);
    }

    /** 构造未认证请求。 */
    public OAuth2PreAuthorizationCodeRequestAuthenticationToken(Object principal, String preAuthorization,
            RegisteredClient registeredClient, Map<String, Object> additionalParameters) {
        super(null);
        this.principal = principal;
        this.additionalParameters = Collections.unmodifiableMap(
                additionalParameters != null ? new HashMap<>(additionalParameters) : Collections.emptyMap());
        this.registeredClient = registeredClient;
        this.preAuthorization = preAuthorization;
        this.allowList = Collections.emptyList();
        this.timeToLive = 0L;
        this.authenticationExpiresAt = 0L;
    }

    /** 验证成功后构造预授权身份，并固定UTC截止时间。 */
    public OAuth2PreAuthorizationCodeRequestAuthenticationToken(Object userPrincipal,
            Map<String, Object> additionalParameters, List<TenantMainDTO> allowList, long timeToLive) {
        this(userPrincipal, additionalParameters, allowList, timeToLive,
                java.time.Instant.now().getEpochSecond() + timeToLive);
    }

    /** 从安全存储恢复原始期限，不延长已验证身份。 */
    public OAuth2PreAuthorizationCodeRequestAuthenticationToken(Object userPrincipal,
            Map<String, Object> additionalParameters, List<TenantMainDTO> allowList, long timeToLive,
            long authenticationExpiresAt) {
        super(null);
        this.principal = userPrincipal;
        this.additionalParameters = Collections.unmodifiableMap(
                additionalParameters != null ? new HashMap<>(additionalParameters) : Collections.emptyMap());
        this.registeredClient = null;
        this.preAuthorization = null;
        this.allowList = allowList == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(allowList);
        this.timeToLive = timeToLive;
        this.authenticationExpiresAt = authenticationExpiresAt;
        super.setAuthenticated(true); // must use super, as we override
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
    }

}
