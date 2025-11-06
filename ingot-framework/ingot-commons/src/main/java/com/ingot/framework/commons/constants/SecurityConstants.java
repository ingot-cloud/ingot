package com.ingot.framework.commons.constants;

/**
 * <p>Description  : SecurityConstants.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/5/9.</p>
 * <p>Time         : 下午3:08.</p>
 */
public interface SecurityConstants {
    String OAUTH2_BEARER_TYPE = "Bearer";
    String OAUTH2_BASIC_TYPE = "Basic";
    String OAUTH2_BASIC_TYPE_WITH_SPACE = OAUTH2_BASIC_TYPE + " ";
    String OAUTH2_BEARER_TYPE_WITH_SPACE = OAUTH2_BEARER_TYPE + " ";


    /**
     * The endpoint URI for access token requests.
     */
    String TOKEN_ENDPOINT_URI = "/oauth2/token";

    /**
     * Then endpoint URI for pre authorize
     */
    String PRE_AUTHORIZE_URI = "/oauth2/pre_authorize";

    /**
     * Then endpoint URI for authorize
     */
    String AUTHORIZE_URI = "/oauth2/authorize";

    /**
     * Renew token
     */
    String HEADER_RENEW_TOKEN = "Renew-Header";
    /**
     * 请求来源，内部 Header 字段
     */
    String HEADER_FROM = HeaderConstants.SECURITY_FROM;
    /**
     * 内部请求标识
     */
    String HEADER_FROM_INSIDE_VALUE = "Inside";

    /**
     * Grant type
     */
    interface GrantType {
        String PASSWORD = "password";
        String AUTHORIZATION_CODE = "authorization_code";
        String REFRESH_TOKEN = "refresh_token";
        String CLIENT_CREDENTIALS = "client_credentials";
        String SOCIAL = "social";
        String PRE_AUTHORIZATION_CODE = "pre_authorization_code";
    }
    interface PreAuthorizationGrantType {
        String PASSWORD = "password";
        String SOCIAL = "social";
        String SESSION = "session";
    }
}
