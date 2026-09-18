package com.ingot.cloud.bff.config;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.bff.error.BffAuthException;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.bff.BffErrorCode;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * <p>按注入 appId 解析 Nacos 应用注册表；Origin 只做一致性校验。</p>
 *
 * <p>{@code require-https=true} 时拒绝空表、非 HTTPS origin 和跨主域。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
public class BffAppRegistry {
    private final BffProperties properties;
    private final Map<String, BffAppRegistration> byAppId = new LinkedHashMap<>();
    private final Map<String, BffAppRegistration> byHost = new LinkedHashMap<>();

    public BffAppRegistry(BffProperties properties) {
        this.properties = properties;
    }

    /**
     * 校验并索引注册表。{@code requireHttps} 时空表、HTTP origin、跨主域均启动失败。
     */
    @PostConstruct
    public void validate() {
        if (properties.getApps() == null || properties.getApps().isEmpty()) {
            if (properties.isRequireHttps()) {
                throw new IllegalStateException("BFF 应用注册表不能为空");
            }
            return;
        }
        for (BffAppRegistration app : properties.getApps()) {
            validateRow(app);
            if (byAppId.put(app.getAppId(), app) != null) {
                throw new IllegalStateException("重复的 BFF appId: " + app.getAppId());
            }
            indexHost(app, app.getAdminOrigin());
            indexHost(app, app.getLoginOrigin());
        }
    }

    public BffAppRegistration requireFromRequest(HttpServletRequest request) {
        String appId = request.getHeader(HeaderConstants.INNER_BFF_APP_ID);
        if (StrUtil.isBlank(appId)) {
            throw new BffAuthException(BffErrorCode.ENTRY_MISMATCH);
        }
        BffAppRegistration app = byAppId.get(appId);
        if (app == null) {
            throw new BffAuthException(BffErrorCode.ENTRY_MISMATCH);
        }
        return app;
    }

    public String requireEntryRole(HttpServletRequest request) {
        String role = request.getHeader(HeaderConstants.INNER_BFF_ENTRY);
        if (!BffConstants.ENTRY_ADMIN.equals(role) && !BffConstants.ENTRY_LOGIN.equals(role)) {
            throw new BffAuthException(BffErrorCode.ENTRY_MISMATCH);
        }
        return role;
    }

    public void requireAdmin(HttpServletRequest request, BffAppRegistration app) {
        if (!BffConstants.ENTRY_ADMIN.equals(requireEntryRole(request))) {
            throw new BffAuthException(BffErrorCode.ENTRY_MISMATCH);
        }
        requireOrigin(request, app.getAdminOrigin());
    }

    public void requireLogin(HttpServletRequest request, BffAppRegistration app) {
        if (!BffConstants.ENTRY_LOGIN.equals(requireEntryRole(request))) {
            throw new BffAuthException(BffErrorCode.ENTRY_MISMATCH);
        }
        requireOrigin(request, app.getLoginOrigin());
    }

    public void requireDomain(BffAppRegistration app, AuthorizationDomain expected) {
        if (app.getDomain() != expected) {
            throw new BffAuthException(BffErrorCode.ENTRY_MISMATCH);
        }
    }

    public String loginUrl(BffAppRegistration app, String transactionId) {
        return trimSlash(app.getLoginOrigin()) + app.getLoginPath() + "?tx=" + transactionId;
    }

    public String completionUrl(BffAppRegistration app, String ticket) {
        return trimSlash(app.getAdminOrigin()) + app.getCompletionPath() + "?ticket=" + ticket;
    }

    public BffAppRegistration resolveByHost(String host) {
        if (StrUtil.isBlank(host)) {
            return null;
        }
        return byHost.get(normalizeHost(host));
    }

    private void requireOrigin(HttpServletRequest request, String expectedOrigin) {
        String origin = request.getHeader("Origin");
        if (StrUtil.isBlank(origin)) {
            String referer = request.getHeader("Referer");
            if (StrUtil.isNotBlank(referer)) {
                origin = originOf(referer);
            }
        }
        if (!equalsOrigin(origin, expectedOrigin)) {
            throw new BffAuthException(BffErrorCode.ENTRY_MISMATCH);
        }
    }

    private void validateRow(BffAppRegistration app) {
        if (StrUtil.hasBlank(app.getAppId(), app.getAdminOrigin(), app.getLoginOrigin(),
                app.getOauthClientId(), app.getOauthRedirectUri()) || app.getDomain() == null) {
            throw new IllegalStateException("BFF 应用注册不完整: " + app.getAppId());
        }
        if (properties.isRequireHttps()) {
            requireHttpsOrigin(app.getAdminOrigin());
            requireHttpsOrigin(app.getLoginOrigin());
            requireSameRegistrableDomain(app);
        }
    }

    private void indexHost(BffAppRegistration app, String origin) {
        String host = hostOf(origin);
        if (byHost.put(host, app) != null) {
            throw new IllegalStateException("重复的 BFF origin host: " + host);
        }
    }

    private static void requireHttpsOrigin(String origin) {
        URI uri = URI.create(origin);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalStateException("BFF origin 必须 HTTPS: " + origin);
        }
    }

    private static void requireSameRegistrableDomain(BffAppRegistration app) {
        String adminHost = URI.create(app.getAdminOrigin()).getHost();
        String loginHost = URI.create(app.getLoginOrigin()).getHost();
        String adminParent = parentDomain(adminHost);
        String loginParent = parentDomain(loginHost);
        if (StrUtil.isBlank(adminParent) || !adminParent.equals(loginParent)) {
            throw new IllegalStateException("BFF origin 必须同一主域: " + app.getAppId());
        }
    }

    private static String parentDomain(String host) {
        if (StrUtil.isBlank(host)) {
            return "";
        }
        int index = host.indexOf('.');
        if (index < 0) {
            return host.toLowerCase(Locale.ROOT);
        }
        return host.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private static String originOf(String url) {
        URI uri = URI.create(url);
        int port = uri.getPort();
        StringBuilder builder = new StringBuilder();
        builder.append(uri.getScheme()).append("://").append(uri.getHost());
        if (port > 0) {
            builder.append(':').append(port);
        }
        return builder.toString();
    }

    private static boolean equalsOrigin(String actual, String expected) {
        if (StrUtil.isBlank(actual) || StrUtil.isBlank(expected)) {
            return false;
        }
        return trimSlash(actual).equalsIgnoreCase(trimSlash(expected));
    }

    static String hostOf(String origin) {
        URI uri = URI.create(origin);
        int port = uri.getPort();
        if (port < 0) {
            return uri.getHost().toLowerCase(Locale.ROOT);
        }
        return (uri.getHost() + ":" + port).toLowerCase(Locale.ROOT);
    }

    static String normalizeHost(String host) {
        String value = host.trim().toLowerCase(Locale.ROOT);
        if (value.endsWith(":443") && !value.contains("://")) {
            return StrUtil.removeSuffix(value, ":443");
        }
        if (value.endsWith(":80")) {
            return StrUtil.removeSuffix(value, ":80");
        }
        return value;
    }

    private static String trimSlash(String value) {
        return StrUtil.removeSuffix(value, "/");
    }
}
