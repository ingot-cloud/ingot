package com.ingot.cloud.auth.service;

import java.security.Principal;
import java.util.List;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.server.authorization.jackson2.IngotOAuth2AuthorizationServerJackson2Module;
import com.ingot.framework.security.web.authentication.UserDetailsCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.core.Authentication;
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

    private UserDetailsCacheService userDetailsCacheService;

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
        Object principal = authorization.getAttribute(Principal.class.getName());
        if (principal instanceof Authentication) {
            IngotUser user = (IngotUser) ((Authentication) principal).getPrincipal();
            OAuth2AccessToken accessToken = authorization.getAccessToken().getToken();
            userDetailsCacheService.save(
                    user, accessToken.getExpiresAt(), accessToken.getTokenValue());
        }

        super.save(authorization);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        Object principal = authorization.getAttribute(Principal.class.getName());
        if (principal instanceof Authentication) {
            IngotUser user = (IngotUser) ((Authentication) principal).getPrincipal();
            OAuth2AccessToken accessToken = authorization.getAccessToken().getToken();
            userDetailsCacheService.remove(user, accessToken.getTokenValue());
        }
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
    public void setUserDetailsCacheService(UserDetailsCacheService userDetailsCacheService) {
        this.userDetailsCacheService = userDetailsCacheService;
    }
}
