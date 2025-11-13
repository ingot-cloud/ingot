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

    /**
     * 元数据-权限
     */
    String META_AUTHORITIES = IGNORE_TENANT_PREFIX + ":meta:authorities";

    /**
     * 元数据-角色
     */
    String META_ROLES = IGNORE_TENANT_PREFIX + ":meta:roles";

    /**
     * 所有权限
     */
    String AUTHORITY_DETAILS = "authority_details";

    /**
     * 菜单
     */
    String MENU_DETAILS = "menu_details";

    /**
     * 授权信息
     */
    String AUTHORIZATION_DETAILS = "auth_details";

    interface Security {
        String PREFIX = IGNORE_TENANT_PREFIX + ":security";
        /**
         * 授权私钥
         */
        String AUTHORIZATION_PRI = PREFIX + ":key_pri";
        /**
         * 公钥
         */
        String AUTHORIZATION_PUB = PREFIX + ":key_pub";
        /**
         * Key ID
         */
        String AUTHORIZATION_KEY_ID = PREFIX + ":key_id";
    }
}
