package com.ingot.framework.commons.constants;

/**
 * <p>Description  : RoleConstants.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/18.</p>
 * <p>Time         : 下午1:38.</p>
 */
public interface RoleConstants {
    /**
     * 角色前缀
     */
    String META_ROLE_CODE_PREFIX = "role_";
    /**
     * 组织角色前缀
     */
    String ORG_ROLE_CODE_PREFIX = "role_org_";


    /**
     * 超管角色编码
     */
    String ROLE_ADMIN_CODE = "role_admin";

    /**
     * 管理员角色编码
     */
    String ROLE_ORG_ADMIN_CODE = "role_org_admin";

    /**
     * 子管理员角色编码
     */
    String ROLE_ORG_SUB_ADMIN_CODE = "role_org_sub_admin";

    /**
     * 主管
     */
    String ROLE_ORG_MANAGER = "role_org_manager";

    /**
     * 用户角色编码
     */
    String ROLE_USER_CODE = "role_user";
}
