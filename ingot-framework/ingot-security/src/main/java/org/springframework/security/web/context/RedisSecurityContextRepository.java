package org.springframework.security.web.context;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <p>Description  : RedisSecurityContextRepository.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/5.</p>
 * <p>Time         : 4:13 PM.</p>
 */
@RequiredArgsConstructor
public class RedisSecurityContextRepository implements SecurityContextRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
            .getContextHolderStrategy();

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
            redisTemplate.opsForValue().set(key, context, 3600, TimeUnit.SECONDS);
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

    private SecurityContext loadContext(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String sessionId = getSessionId(request);
        if (StrUtil.isEmpty(sessionId)) {
            return null;
        }

        return (SecurityContext) redisTemplate.opsForValue().get(key(sessionId));
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

        return sessionId;
    }

    private String key(String sessionId) {
        return CacheConstants.SECURITY_CONTEXT + ":" + sessionId;
    }
}
