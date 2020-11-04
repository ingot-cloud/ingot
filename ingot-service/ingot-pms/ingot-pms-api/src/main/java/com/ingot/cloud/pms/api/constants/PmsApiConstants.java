package com.ingot.cloud.pms.api.constants;

/**
 * <p>Description  : UcApiConstants.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/18.</p>
 * <p>Time         : 下午1:46.</p>
 */
public interface PmsApiConstants {

    /**
     * 授权认证，处理Admin登录数据，唯一登录模式
     */
    String PATH_AUTH_HANDLE_ADMIN_UNIQUE_LOGIN_DATA = "/auth/user/unique/handleUserUniqueLoginData";

    /**
     * 授权认证，处理Admin登录数据，标准登录模式
     */
    String PATH_AUTH_HANDLE_ADMIN_STANDARD_LOGIN_DATA = "/auth/user/standard/handleUserStandardLoginData";

    /**
     * 授权认证，处理登出
     */
    String PATH_AUTH_HANDLE_LOGOUT = "/auth/handleLogout";

    /**
     * 授权认证，刷新token
     */
    String PATH_AUTH_REFRESH_TOKEN = "/auth/refreshToken";

    /**
     * 根据手机号修改用户密码
     */
    String PATH_MODIFY_PASSWORD_BY_MOBILE = "/user/modifyPasswordByMobile";

    /**
     * 获取所有应用信息
     */
    String PATH_CLIENT_GET = "/client/get";

    /**
     * 获取指定应用授权信息
     */
    String PATH_CLIENT_CLIENT_ID_GRANT = "/client/{appId}/grant";

    /**
     * 创建应用
     */
    String PATH_CLIENT_CREATE = "/client/crt";

    /**
     * 更新应用信息
     */
    String PATH_CLIENT_UPDATE = "/client/upd";

    /**
     * 删除应用
     */
    String PATH_CLIENT_DELETE = "/client/del";

    /**
     * 指定应用授权服务
     */
    String PATH_CLIENT_GRANT_ADD = "/client/grant/add";

    /**
     * 指定应用接触授权
     */
    String PATH_CLIENT_GRANT_DELETE = "/client/grant/del";

    /**
     * 获取指定类型的所有应用信息
     */
    String PATH_CLIENT_GET_WITH_TYPE = "/client/getWithType";

    /**
     * 绑定菜单
     */
    String PATH_CLIENT_BIND_MENU = "/client/bingMenu";

}
