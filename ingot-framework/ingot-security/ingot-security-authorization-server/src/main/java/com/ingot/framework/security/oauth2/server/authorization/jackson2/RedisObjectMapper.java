package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ingot.framework.commons.jackson.InJavaTimeModule;
import org.springframework.security.jackson2.SecurityJackson2Modules;

/**
 * <p>Description  : RedisObjectMapper.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/18.</p>
 * <p>Time         : 15:55.</p>
 */
public class RedisObjectMapper {

    public static void config(ObjectMapper objectMapper) {
        // 初始化 ObjectMapper
        // 启用「类型信息」反序列化，用于 Redis 存储数据
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ClassLoader classLoader = RedisObjectMapper.class.getClassLoader();
        objectMapper.registerModules(SecurityJackson2Modules.getModules(classLoader));
        objectMapper.registerModules(
                new JavaTimeModule(),
                new InJavaTimeModule(),
                new InOAuth2AuthorizationServerJackson2Module()
        );
    }
}
