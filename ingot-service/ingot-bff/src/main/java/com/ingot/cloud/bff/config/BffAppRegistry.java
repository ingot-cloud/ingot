package com.ingot.cloud.bff.config;

import com.ingot.cloud.bff.error.BffAuthException;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.bff.BffErrorCode;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.utils.BffOriginPolicy;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 按网关应用身份和精确请求来源验证同源双入口。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class BffAppRegistry {

    private final BffProperties properties;

    /** 启动时拒绝旧四站配置、非法 origin 和重复应用。 */
    @PostConstruct
    public void validate() {
        BffOriginPolicy.validate(properties.getApps(), properties.isRequireHttps());
    }

    /** 校验网关注入身份与真实 Host 一致；BFF 必须位于受信网关之后。 */
    public BffAppRegistration requireFromRequest(HttpServletRequest request) {
        BffAppRegistration app = BffOriginPolicy.match(request.getHeader(HttpHeaders.HOST), properties.getApps());
        if (app == null || !app.getAppId().equals(request.getHeader(HeaderConstants.INNER_BFF_APP_ID)))
            throw new BffAuthException(BffErrorCode.ENTRY_MISMATCH);
        return app;
    }

    /** 固定路径和应用授权域必须一致。 */
    public void requireEntry(String entry, BffAppRegistration app) {
        String expected = app.getDomain() == AuthorizationDomain.PLATFORM ? BffConstants.ENTRY_PLATFORM
                : BffConstants.ENTRY_TENANT;
        if (!expected.equals(entry))
            throw new BffAuthException(BffErrorCode.ENTRY_MISMATCH);
    }

    /** 写请求要求精确 Origin；GET 允许同源 Fetch Metadata 或精确 Referer，不放开 CORS。 */
    public void requireSource(HttpServletRequest request, BffAppRegistration app) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null) {
            try {
                if (app.getOrigin().equals(BffOriginPolicy.normalize(origin)))
                    return;
            }
            catch (IllegalArgumentException ignored) {
            }
            throw new BffAuthException(BffErrorCode.ENTRY_MISMATCH);
        }
        boolean read = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        if (read && BffConstants.SAME_ORIGIN.equals(request.getHeader(BffConstants.FETCH_SITE_HEADER)))
            return;
        if (read) {
            String referer = request.getHeader(HttpHeaders.REFERER);
            try {
                java.net.URI uri = java.net.URI.create(referer == null ? "" : referer);
                if (app.getOrigin().equals(BffOriginPolicy.normalize(uri.getScheme() + "://" + uri.getRawAuthority())))
                    return;
            }
            catch (IllegalArgumentException ignored) {
            }
        }
        throw new BffAuthException(BffErrorCode.ENTRY_MISMATCH);
    }

}
