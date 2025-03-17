package com.ingot.cloud.pms.api.mybatisplus.extension.handlers;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * <p>Description  : CustomOAuth2TypeHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/11.</p>
 * <p>Time         : 11:44 上午.</p>
 */
@Slf4j
@MappedTypes({Object.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class CustomOAuth2TypeHandler extends AbstractJsonTypeHandler<Object> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Class<?> type;

    static {
        ClassLoader classLoader = JdbcRegisteredClientRepository.class.getClassLoader();
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        objectMapper.registerModules(securityModules);
        objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
    }

    public CustomOAuth2TypeHandler(Class<?> type) {
        super(type);
        if (log.isTraceEnabled()) {
            log.trace("IngotOAuth2TypeHandler(" + type + ")");
        }
        Assert.notNull(type, "Type argument cannot be null");
        this.type = type;
    }

    @Override
    public Object parse(String json) {
        try {
            if (ClientSettings.class.isAssignableFrom(type)) {
                Map<String, Object> clientSettingsMap = parseMap(json);
                return ClientSettings.withSettings(clientSettingsMap).build();
            }
            if (TokenSettings.class.isAssignableFrom(type)) {
                Map<String, Object> tokenSettingsMap = parseMap(json);
                return TokenSettings.withSettings(tokenSettingsMap).build();
            }
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toJson(Object obj) {
        try {
            if (obj instanceof ClientSettings) {
                return objectMapper.writeValueAsString(((ClientSettings) obj).getSettings());
            }
            if (obj instanceof TokenSettings) {
                return objectMapper.writeValueAsString(((TokenSettings) obj).getSettings());
            }
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> parseMap(String data) {
        try {
            return objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

}
