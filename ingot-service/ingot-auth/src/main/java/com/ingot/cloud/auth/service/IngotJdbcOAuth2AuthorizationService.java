package com.ingot.cloud.auth.service;

import java.util.List;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.auth.utils.OAuth2AuthorizationUtils;
import com.ingot.framework.security.oauth2.server.authorization.AuthorizationCache;
import com.ingot.framework.security.oauth2.server.authorization.jackson2.IngotOAuth2AuthorizationServerJackson2Module;
import com.ingot.framework.security.oauth2.server.authorization.AuthorizationCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * <p>Description  : IngotJdbcOAuth2AuthorizationService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/26.</p>
 * <p>Time         : 3:07 下午.</p>
 */
@Slf4j
public class IngotJdbcOAuth2AuthorizationService extends JdbcOAuth2AuthorizationService {

    private AuthorizationCacheService authorizationCacheService;

    public IngotJdbcOAuth2AuthorizationService(JdbcOperations jdbcOperations, RegisteredClientRepository registeredClientRepository) {
        super(jdbcOperations, registeredClientRepository);

        OAuth2AuthorizationRowMapper rowMapper = (OAuth2AuthorizationRowMapper) getAuthorizationRowMapper();

        // 自定义 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        ClassLoader classLoader = JdbcOAuth2AuthorizationService.class.getClassLoader();
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        objectMapper.registerModules(securityModules);
        objectMapper.registerModule(new IngotOAuth2AuthorizationServerJackson2Module());

        rowMapper.setObjectMapper(objectMapper);
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        OAuth2AuthorizationUtils.getUser(authorization).ifPresent(user -> {
            OAuth2AccessToken accessToken = authorization.getAccessToken().getToken();
            authorizationCacheService.save(
                    user, accessToken.getExpiresAt(),
                    AuthorizationCache.create(
                            authorization.getId(),
                            authorization.getRegisteredClientId(),
                            authorization.getPrincipalName(),
                            authorization.getAuthorizationGrantType().getValue(),
                            accessToken.getTokenValue()));
        });
        super.save(authorization);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        OAuth2AuthorizationUtils.getUser(authorization).ifPresent(user -> {
            OAuth2AccessToken accessToken = authorization.getAccessToken().getToken();
            authorizationCacheService.remove(user, accessToken.getTokenValue());
        });
        super.remove(authorization);
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return super.findById(id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        return super.findByToken(token, tokenType);
    }

    @Autowired
    public void setUserDetailsCacheService(AuthorizationCacheService authorizationCacheService) {
        this.authorizationCacheService = authorizationCacheService;
    }
}
