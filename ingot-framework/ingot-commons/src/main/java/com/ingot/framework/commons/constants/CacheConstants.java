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

    /* 元数据 */
    /**
     * 元数据-权限
     */
    String META_PERMISSIONS = IGNORE_TENANT_PREFIX + ":meta:permissions";

    /**
     * 元数据-角色
     */
    String META_ROLES = IGNORE_TENANT_PREFIX + ":meta:roles";

    /**
     * 元数据-角色权限
     */
    String META_ROLE_PERMISSIONS = IGNORE_TENANT_PREFIX + ":meta:role_permissions";

    /**
     * 元数据-菜单
     */
    String META_MENUS = IGNORE_TENANT_PREFIX + ":meta:menus";

    /**
     * 元数据-应用
     */
    String META_APPS = IGNORE_TENANT_PREFIX + ":meta:apps";

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
