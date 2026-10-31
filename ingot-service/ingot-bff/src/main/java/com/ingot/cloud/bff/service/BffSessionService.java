package com.ingot.cloud.bff.service;

import java.util.Objects;
import com.ingot.cloud.auth.api.model.vo.InnerSessionVO;
import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.cloud.bff.config.BffAppRegistry;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.error.BffAuthException;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.bff.BffErrorCode;
import com.ingot.framework.commons.model.bff.BffFingerprintMode;
import com.ingot.framework.commons.model.bff.BffSession;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.utils.BffCookiePolicy;
import com.ingot.framework.commons.utils.FingerprintUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 当前应用正式 Cookie 的校验、投递确认及同源恢复，不跨应用读取。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BffSessionService {

    private final BffProperties properties;

    private final BffAppRegistry registry;

    private final LoginTransactionService store;

    private final RemoteAuthSessionService authSessions;

    /** 校验实际请求 Cookie 和权威 sid；确认交付不会延长会话寿命。 */
    public BffSession getSession(HttpServletRequest request, boolean confirmDelivery) {
        BffAppRegistration app = registry.requireFromRequest(request);
        BffSession session = store.readSession(app.getAppId(), sessionId(request, app));
        if (session == null || !verifyFingerprint(session, request))
            return null;
        if (!validSid(session, app)) {
            store.invalidate(session);
            return null;
        }
        if (confirmDelivery && !store.confirm(session))
            return null;
        return session;
    }

    /** 验证 Auth 会话的用户/客户端/期限；依赖故障返回503而非伪装退出。 */
    public boolean validSid(BffSession session, BffAppRegistration app) {
        InnerSessionVO info = readAuthSession(session);
        return matchesAuthSession(session, app, info) && Objects.equals(info.getUserId(), session.getUserId());
    }

    /**
     * 从 Auth 权威 sid 会话绑定新登录的用户身份；Token 响应不会公开内部用户字段。
     * 确认客户端、应用、租户及有效期后才允许提交正式会话，查不到或依赖异常时失败关闭。
     */
    public void bindIdentity(BffSession session, BffAppRegistration app) {
        InnerSessionVO info = readAuthSession(session);
        if (!matchesAuthSession(session, app, info) || info.getUserId() == null) {
            log.warn("[BffSession] issued Auth session identity not confirmed, app={}", app.getAppId());
            throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
        }
        session.setUserId(info.getUserId());
    }

    private InnerSessionVO readAuthSession(BffSession session) {
        R<InnerSessionVO> result;
        try {
            result = authSessions.getBySid(session.getSid());
        }
        catch (RuntimeException exception) {
            log.warn("[BffSession] Auth session lookup unavailable, app={}, errorType={}", session.getAppId(),
                    exception.getClass().getSimpleName());
            throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
        }
        if (result == null || !result.isSuccess()) {
            log.warn("[BffSession] Auth session lookup failed, app={}", session.getAppId());
            throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
        }
        return result.getData();
    }

    private boolean matchesAuthSession(BffSession session, BffAppRegistration app, InnerSessionVO info) {
        return app.getAppId().equals(session.getAppId()) && app.getDomain().getValue().equals(session.getDomain())
                && info != null && Objects.equals(info.getSid(), session.getSid())
                && Objects.equals(info.getClientId(), app.getOauthClientId())
                && (session.getTenantId() == null
                        || Objects.equals(session.getTenantId(), String.valueOf(info.getTenantId())))
                && info.getExpiresAt() != null && info.getExpiresAt().getEpochSecond() > store.now();
    }

    /** 新会话指纹；设备模式要求实际设备头，关闭时不保存。 */
    public String fingerprint(HttpServletRequest request) {
        if (!properties.getSecurity().isFingerprintEnabled())
            return null;
        return resolveFingerprint(properties.getSecurity().getFingerprintMode(), request);
    }

    /** 恢复/会话读取使用创建时的来源，不允许用IP回退冒充设备。 */
    public boolean verifyFingerprint(BffSession session, HttpServletRequest request) {
        if (session.getFingerprint() == null)
            return true;
        try {
            return session.getFingerprint().equals(resolveFingerprint(session.getFingerprintMode(), request));
        }
        catch (BffAuthException ignored) {
            return false;
        }
    }

    /** 只读取可信 app 的正式 Cookie，不从其他名称回退。 */
    public String sessionId(HttpServletRequest request, BffAppRegistration app) {
        return cookie(request, BffCookiePolicy.sessionCookieName(properties.isRequireHttps(), app.getAppId()));
    }

    /** 只读取可信 app 的绑定 Cookie。 */
    public String bindingId(HttpServletRequest request, BffAppRegistration app) {
        return cookie(request, BffCookiePolicy.bindingCookieName(properties.isRequireHttps(), app.getAppId()));
    }

    /** 仅清理当前Host实际携带的已知v1 Cookie，退役Host由固定入口引导清理。 */
    public void clearLegacy(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null)
            for (Cookie value : cookies)
                if (BffCookiePolicy.LEGACY_COOKIES.contains(value.getName()))
                    write(value.getName(), "", 0, response);
    }

    /** 生成或延长当前绑定的Cookie，仅显式POST准备/提交路径可调用。 */
    public void writeBinding(String appId, String id, long ttl, HttpServletResponse response) {
        write(BffCookiePolicy.bindingCookieName(properties.isRequireHttps(), appId), id, ttl, response);
    }

    /** 提交或恢复同一个正式会话的Cookie；不产生新session。 */
    public void writeSession(BffSession session, HttpServletResponse response) {
        write(BffCookiePolicy.sessionCookieName(properties.isRequireHttps(), session.getAppId()),
                session.getSessionId(), session.getExpiresAt() - store.now(), response);
    }

    /** 退出只清当前应用的两个HttpOnly Cookie。 */
    public void clear(BffAppRegistration app, HttpServletResponse response) {
        write(BffCookiePolicy.sessionCookieName(properties.isRequireHttps(), app.getAppId()), "", 0, response);
        write(BffCookiePolicy.bindingCookieName(properties.isRequireHttps(), app.getAppId()), "", 0, response);
    }

    private String cookie(HttpServletRequest request, String name) {
        Cookie[] values = request.getCookies();
        if (values != null)
            for (Cookie value : values)
                if (name.equals(value.getName()) && LoginTransactionService.validId(value.getValue()))
                    return value.getValue();
        return null;
    }

    private String resolveFingerprint(BffFingerprintMode mode, HttpServletRequest request) {
        if (mode == BffFingerprintMode.DEVICE) {
            String device = request.getHeader(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER);
            if (device == null || device.isBlank())
                throw new BffAuthException(BffErrorCode.INVALID_REQUEST);
            return device;
        }
        if (mode != BffFingerprintMode.IPUA)
            throw new BffAuthException(BffErrorCode.INVALID_REQUEST);
        return FingerprintUtil.compute(request.getHeader(HeaderConstants.INNER_CLIENT_REAL_IP),
                request.getHeader(HttpHeaders.USER_AGENT));
    }

    private void write(String name, String value, long ttl, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                BffCookiePolicy.setCookieHeader(name, value, ttl, properties.isRequireHttps()));
    }

}
