package com.ingot.framework.commons.constants;

/**
 * 内部系统 BFF 路径常量。
 *
 * @author jy
 * @since 1.0.0
 */
public interface BffConstants {

    String CSRF = "/bff/auth/csrf";
    String PLATFORM_TRANSACTIONS = "/bff/auth/platform/transactions";
    String TENANT_TRANSACTIONS = "/bff/auth/tenant/transactions";
    String PLATFORM_LOGIN = "/bff/auth/platform/login";
    String TENANT_LOGIN = "/bff/auth/tenant/login";
    String TENANT_SELECT = "/bff/auth/tenant/select";
    String PLATFORM_COMPLETE = "/bff/auth/platform/complete";
    String TENANT_COMPLETE = "/bff/auth/tenant/complete";
    String ME = "/bff/auth/me";
    String LOGOUT = "/bff/auth/logout";

    String ENTRY_ADMIN = "admin";
    String ENTRY_LOGIN = "login";
    String ENTRY_PLATFORM = "platform";
    String ENTRY_TENANT = "tenant";

    String CSRF_HEADER = "X-CSRF-Token";
}
