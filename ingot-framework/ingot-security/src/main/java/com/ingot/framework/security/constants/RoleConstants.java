package com.ingot.framework.security.constants;

/**
 * <p>Description  : RoleConstants.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/18.</p>
 * <p>Time         : 下午1:38.</p>
 */
public interface RoleConstants {

    /**
     * 超管角色Id
     */
    long ROLE_ADMIN_ID = 1;

    /**
     * 匿名
     */
    String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";

    /**
     * 超级管理员权限
     */
    String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * 普通管理员
     */
    String ROLE_MANAGER = "ROLE_MANAGER";

    /**
     * 普通用户权限
     */
    String ROLE_USER = "ROLE_USER";

    /**
     * 是否有访问用户中心 /auth 相关接口权限
     */
    String ROLE_API_PMS_AUTH = "ROLE_API_PMS_AUTH";

    /**
     * 是否有访问鉴权中心 /app 相关接口权限
     */
    String ROLE_API_AC_APP = "ROLE_API_AC_APP";

    /**
     * actuator端点角色
     */
    String ROLE_API_ACTUATOR = "ROLE_API_ACTUATOR";

    /**
     * 服务角色，鉴权中心
     */
    String ROLE_SERVICE_AC = "ROLE_SERVICE_AC";
}
