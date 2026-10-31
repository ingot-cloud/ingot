package com.ingot.framework.commons.constants;

/**
 * <p>
 * 同源内部登录的固定入口、协议字段和运行边界。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface BffConstants {

    String ROOT = "/bff/auth";

    String CSRF = ROOT + "/csrf";

    String PLATFORM_TRANSACTIONS = ROOT + "/platform/transactions";

    String TENANT_TRANSACTIONS = ROOT + "/tenant/transactions";

    String TRANSACTION_VARIABLE = "id";

    String PLATFORM_TRANSACTION = PLATFORM_TRANSACTIONS + "/{" + TRANSACTION_VARIABLE + "}";

    String TENANT_TRANSACTION = TENANT_TRANSACTIONS + "/{" + TRANSACTION_VARIABLE + "}";

    String PLATFORM_RESUME = PLATFORM_TRANSACTION + "/resume";

    String TENANT_RESUME = TENANT_TRANSACTION + "/resume";

    String PLATFORM_LOGIN = ROOT + "/platform/login";

    String TENANT_LOGIN = ROOT + "/tenant/login";

    String TENANT_SELECT = ROOT + "/tenant/select";

    String ME = ROOT + "/me";

    String LOGOUT = ROOT + "/logout";

    String ENTRY_PLATFORM = "platform";

    String ENTRY_TENANT = "tenant";

    String APP_PLATFORM = "platform-admin";

    String APP_TENANT = "tenant-admin";

    String LOGIN_PATH = "/login/";

    String DEFAULT_RETURN_TO = "/";

    String DEFAULT_OAUTH_SCOPE = "system";

    String CSRF_HEADER = "X-CSRF-Token";

    String AUTHENTICATION_EXPIRES_AT = "authenticationExpiresAt";

    String FETCH_SITE_HEADER = "Sec-Fetch-Site";

    String SAME_ORIGIN = "same-origin";

    String CLEANUP_INTERVAL_EXPRESSION = "$" + "{ingot.bff.cleanup-interval:5000}";

    String OPAQUE_ID_REGEX = "[A-Za-z0-9_-]{22,128}";

    int TOKEN_BYTES = 16;

    int CSRF_BYTES = 24;

    int PKCE_BYTES = 32;

    long RETRY_AFTER_MILLIS = 1000;

    int CLEANUP_BATCH_SIZE = 100;

    long CLEANUP_LEASE_SECONDS = 30;

}
