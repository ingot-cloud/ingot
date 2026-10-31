package com.ingot.framework.commons.utils;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;

/**
 * <p>
 * Gateway 与 BFF 共用的精确 origin 规范化和注册表验证。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class BffOriginPolicy {

    private static final String HTTPS = "https";

    private static final String HTTP = "http";

    private static final int HTTPS_PORT = 443;

    private static final int HTTP_PORT = 80;

    private BffOriginPolicy() {
    }

    /** 解析无路径、无身份和查询参数的 HTTP origin，并规范化默认端口。 */
    public static String normalize(String origin) {
        if (origin == null || origin.isBlank())
            throw new IllegalArgumentException("缺少 BFF origin");
        URI uri = URI.create(origin);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if ((!HTTP.equals(scheme) && !HTTPS.equals(scheme)) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty() && !"/".equals(uri.getRawPath()))
                || uri.getPort() == 0 || uri.getPort() > 65535) {
            throw new IllegalArgumentException("BFF origin 必须是精确的 HTTP(S) origin");
        }
        int port = uri.getPort();
        String suffix = port < 0 || (HTTPS.equals(scheme) && port == HTTPS_PORT)
                || (HTTP.equals(scheme) && port == HTTP_PORT) ? "" : ":" + port;
        return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + suffix;
    }

    /** 校验两个固定内部应用；生产支持合法 IP HTTPS，HTTP 仅限回环开发。 */
    public static void validate(List<BffAppRegistration> apps, boolean requireHttps) {
        if (apps == null || apps.size() != 2)
            throw new IllegalStateException("必须注册平台、租户两个 BFF 应用");
        Set<String> ids = new HashSet<>();
        Set<String> hosts = new HashSet<>();
        Set<String> clients = new HashSet<>();
        for (BffAppRegistration app : apps) {
            String origin = normalize(app.getOrigin());
            URI uri = URI.create(origin);
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            boolean loopback = host.equals("localhost") || host.endsWith(".localhost") || host.equals("127.0.0.1")
                    || host.equals("[::1]") || host.equals("::1");
            if (requireHttps && !HTTPS.equals(uri.getScheme()) || !requireHttps && !loopback)
                throw new IllegalStateException("生产 BFF 必须 HTTPS；HTTP 仅限 loopback DEV");
            boolean platform = BffConstants.APP_PLATFORM.equals(app.getAppId())
                    && app.getDomain() == AuthorizationDomain.PLATFORM;
            boolean tenant = BffConstants.APP_TENANT.equals(app.getAppId())
                    && app.getDomain() == AuthorizationDomain.TENANT;
            if ((!platform && !tenant) || !ids.add(app.getAppId()) || !hosts.add(authority(origin))
                    || app.getOauthClientId() == null || app.getOauthClientId().isBlank()
                    || !clients.add(app.getOauthClientId()) || app.getOauthScope() == null
                    || app.getOauthScope().isBlank() || app.getOauthRedirectUri() == null
                    || app.getOauthRedirectUri().isBlank()
                    || !BffConstants.DEFAULT_RETURN_TO.equals(app.getDefaultReturnTo()))
                throw new IllegalStateException("BFF 应用身份、origin 或 OAuth 注册不合法/重复");
            app.setOrigin(origin);
        }
    }

    /** 返回含非默认端口的规范化 Host，用于可信网关映射。 */
    public static String authority(String origin) {
        return URI.create(normalize(origin)).getRawAuthority();
    }

    /** 按受信 Host 查找应用；未知或非法 Host 不匹配。 */
    public static BffAppRegistration match(String host, List<BffAppRegistration> apps) {
        if (host == null || apps == null)
            return null;
        for (BffAppRegistration app : apps) {
            URI uri = URI.create(app.getOrigin());
            try {
                if (authority(uri.getScheme() + "://" + host).equals(authority(app.getOrigin())))
                    return app;
            }
            catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

}
