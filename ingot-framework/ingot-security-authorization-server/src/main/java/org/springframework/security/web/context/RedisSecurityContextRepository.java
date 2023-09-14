package org.springframework.security.web.context;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.security.jackson2.IngotSecurityJackson2Modules;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <p>Description  : RedisSecurityContextRepository.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/5.</p>
 * <p>Time         : 4:13 PM.</p>
 */
@Slf4j
public class RedisSecurityContextRepository implements SecurityContextRepository, SecurityContextRevokeRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
            .getContextHolderStrategy();

    public RedisSecurityContextRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        ClassLoader classLoader = JdbcOAuth2AuthorizationService.class.getClassLoader();
        IngotSecurityJackson2Modules.registerModules(objectMapper, classLoader);
    }

    @Override
    @Deprecated
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        throw new UnsupportedOperationException("Deprecated Use loadDeferredContext(HttpServletRequest) instead.");
    }

    @Override
    public DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
        Supplier<SecurityContext> supplier = () -> loadContext(request);
        return new SupplierDeferredSecurityContext(supplier, this.securityContextHolderStrategy);
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionId(request);
        if (StrUtil.isEmpty(sessionId)) {
            return;
        }

        String key = key(sessionId);
        SecurityContext emptyContext = this.securityContextHolderStrategy.createEmptyContext();
        if (emptyContext.equals(context)) {
            redisTemplate.delete(key);
        } else {
            try {
                // 默认半小时过期
                long timeout = 1800;
                Authentication authentication = context.getAuthentication();
                if (authentication instanceof OAuth2PreAuthorizationCodeRequestAuthenticationToken token) {
                    timeout = token.getTimeToLive();
                }
                String value = this.objectMapper.writeValueAsString(context);
                redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
            } catch (JsonProcessingException e) {
                log.warn("[RedisSecurityContextRepository] 保存 SecurityContext 失败", e);
            }
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        if (StrUtil.isEmpty(sessionId)) {
            return false;
        }
        Boolean result = redisTemplate.hasKey(key(sessionId));
        return result != null ? result : false;
    }

    @Override
    public void revokeContext(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        if (StrUtil.isEmpty(sessionId)) {
            return;
        }
        redisTemplate.delete(key(sessionId));
    }

    private SecurityContext loadContext(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String sessionId = getSessionId(request);
        if (StrUtil.isEmpty(sessionId)) {
            return null;
        }

        String key = key(sessionId);
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return this.objectMapper.readValue((String) value, SecurityContext.class);
        } catch (JsonProcessingException e) {
            log.warn("[RedisSecurityContextRepository] 读取 SecurityContext 失败", e);
            return null;
        }
    }

    private String getSessionId(HttpServletRequest request) {
        String sessionId = request.getHeader(IngotOAuth2ParameterNames.SESSION_ID);
        if (StrUtil.isEmpty(sessionId)) {
            sessionId = request.getParameter(IngotOAuth2ParameterNames.SESSION_ID);
        }

        if (StrUtil.isEmpty(sessionId)) {
            HttpSession session = request.getSession(Boolean.FALSE);
            if (session != null) {
                sessionId = session.getId();
            }
        }

        if (StrUtil.isEmpty(sessionId)) {
            sessionId = Arrays.stream(request.getCookies())
                    .filter(cookie -> cookie.getName().equals("JSESSIONID"))
                    .map(Cookie::getValue)
                    .findFirst().orElse(null);
        }

        return sessionId;
    }

    private String key(String sessionId) {
        return CacheConstants.SECURITY_CONTEXT + ":" + sessionId;
    }
}
