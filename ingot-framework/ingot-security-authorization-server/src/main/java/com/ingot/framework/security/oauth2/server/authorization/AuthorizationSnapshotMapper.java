package com.ingot.framework.security.oauth2.server.authorization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * OAuth2Authorization 和 AuthorizationSnapshot 之间的转换工具
 *
 * <p>Author: jy</p>
 * <p>Date: 2024/12/17</p>
 */
public class AuthorizationSnapshotMapper {

    /**
     * 将 OAuth2Authorization 转换为 AuthorizationSnapshot
     */
    public static AuthorizationSnapshot toSnapshot(OAuth2Authorization authorization) {
        if (authorization == null) {
            return null;
        }

        AuthorizationSnapshot snapshot = new AuthorizationSnapshot();

        // 基本信息
        snapshot.setId(authorization.getId());
        snapshot.setRegisteredClientId(authorization.getRegisteredClientId());
        snapshot.setPrincipalName(authorization.getPrincipalName());
        snapshot.setAuthorizationGrantType(authorization.getAuthorizationGrantType().getValue());
        snapshot.setAuthorizedScopes(new HashSet<>(authorization.getAuthorizedScopes()));
        snapshot.setAttributes(new HashMap<>(authorization.getAttributes()));

        // State 参数
        if (authorization.getAttribute(OAuth2ParameterNames.STATE) != null) {
            snapshot.setState(authorization.getAttribute(OAuth2ParameterNames.STATE));
        }

        // Authorization Code
        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                authorization.getToken(OAuth2AuthorizationCode.class);
        if (authorizationCode != null) {
            OAuth2Token token = authorizationCode.getToken();
            snapshot.setAuthorizationCodeValue(token.getTokenValue());
            snapshot.setAuthorizationCodeIssuedAt(token.getIssuedAt());
            snapshot.setAuthorizationCodeExpiresAt(token.getExpiresAt());
            snapshot.setAuthorizationCodeMetadata(new HashMap<>(authorizationCode.getMetadata()));
        }

        // Access Token
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken != null) {
            OAuth2AccessToken token = accessToken.getToken();
            snapshot.setAccessTokenValue(token.getTokenValue());
            snapshot.setAccessTokenIssuedAt(token.getIssuedAt());
            snapshot.setAccessTokenExpiresAt(token.getExpiresAt());
            snapshot.setAccessTokenType(token.getTokenType().getValue());
            snapshot.setAccessTokenScopes(new HashSet<>(token.getScopes()));
            snapshot.setAccessTokenMetadata(new HashMap<>(accessToken.getMetadata()));
        }

        // Refresh Token
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null) {
            OAuth2Token token = refreshToken.getToken();
            snapshot.setRefreshTokenValue(token.getTokenValue());
            snapshot.setRefreshTokenIssuedAt(token.getIssuedAt());
            snapshot.setRefreshTokenExpiresAt(token.getExpiresAt());
            snapshot.setRefreshTokenMetadata(new HashMap<>(refreshToken.getMetadata()));
        }

        // OIDC ID Token
        OAuth2Authorization.Token<OidcIdToken> oidcIdToken = authorization.getToken(OidcIdToken.class);
        if (oidcIdToken != null) {
            OAuth2Token token = oidcIdToken.getToken();
            snapshot.setOidcIdTokenValue(token.getTokenValue());
            snapshot.setOidcIdTokenIssuedAt(token.getIssuedAt());
            snapshot.setOidcIdTokenExpiresAt(token.getExpiresAt());
            snapshot.setOidcIdTokenMetadata(new HashMap<>(oidcIdToken.getMetadata()));
        }

        // User Code Token
        OAuth2Authorization.Token<OAuth2UserCode> userCodeToken = authorization.getToken(OAuth2UserCode.class);
        if (userCodeToken != null) {
            OAuth2Token token = userCodeToken.getToken();
            snapshot.setUserCodeValue(token.getTokenValue());
            snapshot.setUserCodeIssuedAt(token.getIssuedAt());
            snapshot.setUserCodeExpiresAt(token.getExpiresAt());
            snapshot.setUserCodeMetadata(new HashMap<>(userCodeToken.getMetadata()));
        }

        // Device Code Token
        OAuth2Authorization.Token<OAuth2DeviceCode> deviceCodeToken = authorization.getToken(OAuth2DeviceCode.class);
        if (deviceCodeToken != null) {
            OAuth2Token token = deviceCodeToken.getToken();
            snapshot.setDeviceCodeValue(token.getTokenValue());
            snapshot.setDeviceCodeIssuedAt(token.getIssuedAt());
            snapshot.setDeviceCodeExpiresAt(token.getExpiresAt());
            snapshot.setDeviceCodeMetadata(new HashMap<>(deviceCodeToken.getMetadata()));
        }
        return snapshot;
    }

    /**
     * 将 AuthorizationSnapshot 转换为 OAuth2Authorization
     * 
     * @param snapshot Snapshot 对象
     * @param registeredClientRepository RegisteredClient仓库，用于获取客户端信息
     * @return OAuth2Authorization对象
     */
    public static OAuth2Authorization fromSnapshot(AuthorizationSnapshot snapshot, 
                                                    RegisteredClientRepository registeredClientRepository) {
        if (snapshot == null) {
            return null;
        }

        // 获取 RegisteredClient
        RegisteredClient registeredClient = registeredClientRepository.findById(snapshot.getRegisteredClientId());
        if (registeredClient == null) {
            throw new IllegalStateException("RegisteredClient not found: " + snapshot.getRegisteredClientId());
        }

        // 使用 RegisteredClient 创建 Builder
        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient);
        
        builder.id(snapshot.getId())
                .principalName(snapshot.getPrincipalName())
                .authorizationGrantType(new AuthorizationGrantType(snapshot.getAuthorizationGrantType()));
        
        if (snapshot.getAuthorizedScopes() != null) {
            builder.authorizedScopes(snapshot.getAuthorizedScopes());
        }

        // Attributes
        if (snapshot.getAttributes() != null) {
            snapshot.getAttributes().forEach(builder::attribute);
        }

        // State
        if (snapshot.getState() != null) {
            builder.attribute(OAuth2ParameterNames.STATE, snapshot.getState());
        }

        // Authorization Code
        if (snapshot.getAuthorizationCodeValue() != null) {
            OAuth2AuthorizationCode authorizationCode = new OAuth2AuthorizationCode(
                    snapshot.getAuthorizationCodeValue(),
                    snapshot.getAuthorizationCodeIssuedAt(),
                    snapshot.getAuthorizationCodeExpiresAt()
            );
            builder.token(authorizationCode, metadata -> {
                if (snapshot.getAuthorizationCodeMetadata() != null) {
                    metadata.putAll(snapshot.getAuthorizationCodeMetadata());
                }
            });
        }

        // Access Token
        if (snapshot.getAccessTokenValue() != null) {
            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    snapshot.getAccessTokenValue(),
                    snapshot.getAccessTokenIssuedAt(),
                    snapshot.getAccessTokenExpiresAt(),
                    snapshot.getAccessTokenScopes()
            );
            builder.token(accessToken, metadata -> {
                if (snapshot.getAccessTokenMetadata() != null) {
                    metadata.putAll(snapshot.getAccessTokenMetadata());
                }
            });
        }

        // Refresh Token
        if (snapshot.getRefreshTokenValue() != null) {
            OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                    snapshot.getRefreshTokenValue(),
                    snapshot.getRefreshTokenIssuedAt(),
                    snapshot.getRefreshTokenExpiresAt()
            );
            builder.token(refreshToken, metadata -> {
                if (snapshot.getRefreshTokenMetadata() != null) {
                    metadata.putAll(snapshot.getRefreshTokenMetadata());
                }
            });
        }

        // OIDC ID Token
        if (snapshot.getOidcIdTokenValue() != null) {
            OidcIdToken oidcIdToken = new OidcIdToken(
                    snapshot.getOidcIdTokenValue(),
                    snapshot.getOidcIdTokenIssuedAt(),
                    snapshot.getOidcIdTokenExpiresAt(),
                    (Map<String, Object>) snapshot.getOidcIdTokenMetadata().get(OAuth2Authorization.Token.CLAIMS_METADATA_NAME)
            );
            builder.token(oidcIdToken, metadata -> {
                if (snapshot.getOidcIdTokenMetadata() != null) {
                    metadata.putAll(snapshot.getOidcIdTokenMetadata());
                }
            });
        }

        // User Code Token
        if (snapshot.getUserCodeValue() != null) {
            OAuth2UserCode userCode = new OAuth2UserCode(
                    snapshot.getUserCodeValue(),
                    snapshot.getUserCodeIssuedAt(),
                    snapshot.getUserCodeExpiresAt()
            );
            builder.token(userCode, metadata -> {
                if (snapshot.getUserCodeMetadata() != null) {
                    metadata.putAll(snapshot.getUserCodeMetadata());
                }
            });
        }

        // Device Code Token
        if (snapshot.getDeviceCodeValue() != null) {
            OAuth2DeviceCode deviceCode = new OAuth2DeviceCode(
                    snapshot.getDeviceCodeValue(),
                    snapshot.getDeviceCodeIssuedAt(),
                    snapshot.getDeviceCodeExpiresAt()
            );
            builder.token(deviceCode, metadata -> {
                if (snapshot.getDeviceCodeMetadata() != null) {
                    metadata.putAll(snapshot.getDeviceCodeMetadata());
                }
            });
        }

        return builder.build();
    }
}
