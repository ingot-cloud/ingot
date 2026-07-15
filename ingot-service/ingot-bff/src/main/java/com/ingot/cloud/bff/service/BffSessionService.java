package com.ingot.cloud.bff.service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.model.enums.FingerprintMode;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.model.bff.BffSession;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.utils.FingerprintUtil;
import com.ingot.framework.commons.utils.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

/**
 * <p>BFF 会话管理服务，负责 Session 全生命周期管理</p>
 *
 * <p>核心职责：</p>
 * <ul>
 *     <li>Redis 中的 {@link BffSession} 读写（key 格式 {@code in:bff_session:{sessionId}}）</li>
 *     <li>HttpOnly Cookie 下发与清除（手动拼接 Set-Cookie 以支持 SameSite 属性）</li>
 *     <li>客户端指纹校验 —— 支持前端设备指纹（推荐）和服务端 IP+UA 两种模式</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 创建 session 并下发 Cookie
 * BffSession session = new BffSession();
 * session.setClientId("ingot-bff");
 * String sessionId = bffSessionService.createSession(session, request, response);
 *
 * // 后续请求自动从 Cookie 获取并校验指纹
 * BffSession current = bffSessionService.getSession(request);
 * }</pre>
 *
 * @author jy
 * @since 1.0.0
 *
 * @see BffSession
 * @see BffProperties.CookieConfig
 * @see BffProperties.SecurityConfig
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BffSessionService {
    private final StringRedisTemplate redisTemplate;
    private final BffProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 创建新 session，下发 HttpOnly Cookie。
     *
     * @param session  会话数据
     * @param request  当前请求（用于提取客户端指纹）
     * @param response 响应（用于写 Cookie）
     * @return sessionId
     */
    public String createSession(BffSession session, HttpServletRequest request, HttpServletResponse response) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        session.setCreatedAt(Instant.now().getEpochSecond());

        if (properties.getSecurity().isFingerprintEnabled()) {
            session.setFingerprint(resolveFingerprint(request));
        }

        saveSession(sessionId, session, properties.getSessionTtl());
        setCookie(sessionId, properties.getSessionTtl(), response);

        log.info("[BffSession] created, id={}", sessionId);
        return sessionId;
    }

    /**
     * 从当前请求的 Cookie 获取 session，同时校验客户端指纹。
     *
     * @return session（指纹不匹配或不存在均返回 null）
     */
    public BffSession getSession(HttpServletRequest request) {
        String sessionId = getSessionIdFromCookie(request);
        if (StrUtil.isEmpty(sessionId)) {
            return null;
        }
        BffSession session = getSession(sessionId);
        if (session == null) {
            return null;
        }

        if (properties.getSecurity().isFingerprintEnabled() && StrUtil.isNotEmpty(session.getFingerprint())) {
            String current = resolveFingerprint(request);
            if (!StrUtil.equals(session.getFingerprint(), current)) {
                log.warn("[BffSession] fingerprint mismatch, sessionId={}, expected={}, actual={}",
                        sessionId, session.getFingerprint(), current);
                return null;
            }
        }

        return session;
    }

    public BffSession getSession(String sessionId) {
        String value = redisTemplate.opsForValue().get(CacheConstants.bffSessionKey(sessionId));
        if (StrUtil.isEmpty(value)) {
            return null;
        }
        try {
            return objectMapper.readValue(value, BffSession.class);
        } catch (JsonProcessingException e) {
            log.warn("[BffSession] deserialize failed, sessionId={}", sessionId, e);
            return null;
        }
    }

    /**
     * 更新 session 并指定 TTL（秒），同时刷新 Cookie 的 Max-Age。
     * 典型场景：Token 换取成功后，将 session TTL 与 accessToken 有效期对齐。
     */
    public void updateSession(String sessionId, BffSession session, long ttlSeconds, HttpServletResponse response) {
        saveSession(sessionId, session, ttlSeconds);
        setCookie(sessionId, ttlSeconds, response);
    }

    /**
     * 销毁 session：删除 Redis + 清除 Cookie。
     */
    public void removeSession(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionIdFromCookie(request);
        if (StrUtil.isNotEmpty(sessionId)) {
            redisTemplate.delete(CacheConstants.bffSessionKey(sessionId));
            removeCookie(response);
            log.info("[BffSession] removed, id={}", sessionId);
        }
    }

    public String getSessionIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (CacheConstants.BFF_SESSION_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    // ---- internal ----

    private void saveSession(String sessionId, BffSession session, long ttlSeconds) {
        try {
            String value = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(CacheConstants.bffSessionKey(sessionId),
                    value, ttlSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save BFF session", e);
        }
    }

    /**
     * 通过 Set-Cookie 响应头设置 Cookie，支持 SameSite 属性。
     * Servlet Cookie API 不支持 SameSite，因此手动拼接 Set-Cookie Header。
     */
    private void setCookie(String sessionId, long maxAgeSeconds, HttpServletResponse response) {
        BffProperties.CookieConfig cc = properties.getCookie();

        StringBuilder sb = new StringBuilder();
        sb.append(CacheConstants.BFF_SESSION_COOKIE_NAME).append("=").append(sessionId);
        sb.append("; Path=/");
        sb.append("; Max-Age=").append(maxAgeSeconds);
        sb.append("; HttpOnly");

        if (StrUtil.isNotEmpty(cc.getDomain())) {
            sb.append("; Domain=").append(cc.getDomain());
        }
        if (cc.isSecure()) {
            sb.append("; Secure");
        }
        if (StrUtil.isNotEmpty(cc.getSameSite())) {
            sb.append("; SameSite=").append(cc.getSameSite());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, sb.toString());
    }

    private void removeCookie(HttpServletResponse response) {
        BffProperties.CookieConfig cc = properties.getCookie();

        StringBuilder sb = new StringBuilder();
        sb.append(CacheConstants.BFF_SESSION_COOKIE_NAME).append("=");
        sb.append("; Path=/");
        sb.append("; Max-Age=0");
        sb.append("; HttpOnly");

        if (StrUtil.isNotEmpty(cc.getDomain())) {
            sb.append("; Domain=").append(cc.getDomain());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, sb.toString());
    }

    private String resolveFingerprint(HttpServletRequest request) {
        String mode = properties.getSecurity().getFingerprintMode();
        if (FingerprintMode.DEVICE.getValue().equalsIgnoreCase(mode)) {
            String deviceFp = request.getHeader(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER);
            if (StrUtil.isNotEmpty(deviceFp)) {
                return deviceFp;
            }
            log.warn("[BffSession] device fingerprint header missing, falling back to ip_ua");
        }
        // 优先读网关统一设置的 In-Inner-Client-Real-IP，保证与网关侧指纹计算使用同一 IP
        String ip = request.getHeader(HeaderConstants.INNER_CLIENT_REAL_IP);
        if (StrUtil.isEmpty(ip)) {
            ip = WebUtil.getClientIP(request);
        }
        String ua = request.getHeader(HttpHeaders.USER_AGENT);
        return FingerprintUtil.compute(ip, ua);
    }
}
