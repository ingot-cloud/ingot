package com.ingot.cloud.auth.service;

import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.security.oauth2.server.authorization.code.OAuth2PreAuthorization;
import com.ingot.framework.security.oauth2.server.authorization.code.OAuth2PreAuthorizationService;
import com.ingot.framework.security.oauth2.server.authorization.jackson2.IngotOAuth2AuthorizationServerJackson2Module;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>Description  : DefaultPreAuthorizationCodeService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 4:53 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultOAuth2PreAuthorizationService implements OAuth2PreAuthorizationService {
    /**
     * 2分钟超时
     */
    private static final int EXPIRED_TIME = 2 * 60;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public DefaultOAuth2PreAuthorizationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        ClassLoader classLoader = JdbcOAuth2AuthorizationService.class.getClassLoader();
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        objectMapper.registerModules(securityModules);
        objectMapper.registerModule(new IngotOAuth2AuthorizationServerJackson2Module());
    }

    @Override
    public void save(OAuth2PreAuthorization authorization) {
        String code = authorization.getToken().getTokenValue();
        try {
            String value = objectMapper.writeValueAsString(authorization);
            redisTemplate.opsForValue().set(key(code), value,
                    EXPIRED_TIME + RandomUtil.randomInt(10), TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("[DefaultOAuth2PreAuthorizationService] - OAuth2PreAuthorization 序列化失败", e);
        }
    }

    @Override
    public OAuth2PreAuthorization get(String code) {
        String key = key(code);
        Object value = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue((String) value, OAuth2PreAuthorization.class);
        } catch (JsonProcessingException e) {
            log.warn("[DefaultOAuth2PreAuthorizationService] - OAuth2PreAuthorization 反序列化失败", e);
        }
        return null;
    }

    private String key(String code) {
        return CacheConstants.PRE_AUTHORIZATION + ":" + code;
    }
}
