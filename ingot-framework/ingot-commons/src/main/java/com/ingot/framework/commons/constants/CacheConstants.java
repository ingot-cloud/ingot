package com.ingot.framework.commons.constants;

/**
 * <p>Description  : 缓存常量.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/4.</p>
 * <p>Time         : 下午2:09.</p>
 */
public interface CacheConstants {

    /**
     * 忽略tenant前缀
     */
    String IGNORE_TENANT_PREFIX = "in";

    /**
     * OAuth2 客户端详情
     */
    String CLIENT_DETAILS = IGNORE_TENANT_PREFIX + ":client_details";

    /**
     * 保存security context
     */
    String SECURITY_CONTEXT = IGNORE_TENANT_PREFIX + ":security_context";

    /**
     * 预授权code
     */
    String PRE_AUTHORIZATION = IGNORE_TENANT_PREFIX + ":pre_authorization";

    /* 平台级 */
    /**
     * 平台-权限
     */
    String PLATFORM_PERMISSIONS = IGNORE_TENANT_PREFIX + ":platform:permissions";

    /**
     * 平台-角色
     */
    String PLATFORM_ROLES = IGNORE_TENANT_PREFIX + ":platform:roles";

    /**
     * 平台-角色权限
     */
    String PLATFORM_ROLE_PERMISSIONS = IGNORE_TENANT_PREFIX + ":platform:role_permissions";

    /**
     * 平台-菜单
     */
    String PLATFORM_MENUS = IGNORE_TENANT_PREFIX + ":platform:menus";

    /**
     * 平台-应用
     */
    String PLATFORM_APPS = IGNORE_TENANT_PREFIX + ":platform:apps";

    /**
     * 客户端用户角色
     */
    String MEMBER_ROLES = "member:roles";

    /**
     * 客户端用户权限
     */
    String MEMBER_PERMISSIONS = "member:permissions";

    /**
     * 客户端角色权限
     */
    String MEMBER_ROLE_PERMISSIONS = "member:role_permissions";

    /* 组织 */
    /**
     * 组织角色
     */
    String TENANT_ROLES = "roles";

    /**
     * 组织角色权限
     */
    String TENANT_ROLE_PERMISSIONS = "role_permissions";

    /**
     * 授权信息
     */
    String AUTHORIZATION_DETAILS = "auth_details";

    /**
     * BFF session key 前缀
     */
    String BFF_SESSION = IGNORE_TENANT_PREFIX + ":bff_session";

    /**
     * BFF session cookie 名称
     */
    String BFF_SESSION_COOKIE_NAME = "IN_SESSION";

    /**
     * 构建 BFF session 的完整 Redis key
     *
     * @param sessionId 会话 ID
     * @return {@code in:bff_session:{sessionId}}
     */
    static String bffSessionKey(String sessionId) {
        return BFF_SESSION + ":" + sessionId;
    }

    interface Security {
        String PREFIX = IGNORE_TENANT_PREFIX + ":security";
        
        /**
         * JWK 密钥前缀（新版，支持多密钥和轮换）
         */
        String AUTHORIZATION_KEY_PREFIX = PREFIX + ":jwk:key:";
        
        /**
         * 活跃的密钥 ID 集合
         */
        String AUTHORIZATION_KEY_IDS = PREFIX + ":jwk:key-ids";
        
        /**
         * 当前用于签名的密钥 ID
         */
        String AUTHORIZATION_CURRENT_KEY_ID = PREFIX + ":jwk:current-key-id";
    }
}
