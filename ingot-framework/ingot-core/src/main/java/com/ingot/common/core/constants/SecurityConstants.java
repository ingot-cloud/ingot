package com.ingot.common.core.constants;

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
     * 资源服务器默认bean名称
     */
    String RESOURCE_SERVER_CONFIGURER = "resourceServerConfigurerAdapter";

    /**
     * 客户端模式
     */
    String CLIENT_CREDENTIALS = "client_credentials";

    /**
     * Renew token
     */
    String HEADER_RENEW_TOKEN = "Renew-Header";
    /**
     * 请求来源，内部 Header 字段
     */
    String HEADER_FROM = "Ingot-From";
    /**
     * 内部请求标识
     */
    String HEADER_FROM_INSIDE_VALUE = "Inside";
    /**
     * 显示图形验证码 header，1为需要显示验证码，0为不需要
     */
    String HEADER_VALIDATE_IMAGE_DISPLAY_TIME = "ingot-image-code";

    /**
     * Context key
     */
    String CONTEXT_KEY_CURRENT_USER = "ctx-token-current-user";
    String CONTEXT_KEY_TOKEN = "ctx-token";

    /**
     * Jwt token claim key
     */
    String TOKEN_ENHANCER_KEY_USER_NAME = "username";
    String TOKEN_ENHANCER_KEY_USER_ID = "sub";
    String TOKEN_ENHANCER_KEY_TENANT_ID = "tenant_id";
    String TOKEN_ENHANCER_KEY_DEPT_ID = "dept_id";
    String TOKEN_ENHANCER_KEY_TIMESTAMP = "timestamp";
    String TOKEN_ENHANCER_KEY_AUTH_TYPE = "auth_type";

    /**
     * 登录类型，唯一类型，当前账号只能在一个地方登录
     */
    String AUTH_TYPE_UNIQUE = "unique";
    /**
     * 登录类型，默认标准类型，单点登录不互踢
     */
    String AUTH_TYPE_STANDARD = "standard";
    /*
     * Security path start.
     */
    /**
     * 密码登录
     */
    String PATH_LOGIN_PASSWORD = "/auth/login";
    /**
     * 用户刷新token
     */
    String PATH_REFRESH_TOKEN_USER = "/auth/user/refreshToken";
    /**
     * 退出登录
     */
    String PATH_LOGOUT_AUTH = "/auth/logout";
    /**
     * 手机验证码登录请求处理url
     */
    String PATH_LOGIN_MOBILE = "/auth/mobile";
    /**
     * 用户手机号修改密码
     */
    String PATH_USER_MODIFY_PASSWORD_BY_MOBILE = "/auth/modifyPasswordByMobile";
    /**
     * 处理验证码的url前缀
     */
    String PATH_VALIDATE_CODE_URL_PREFIX = "/code";
    /*
     * Security path end.
     */

    /**
     * 验证图片验证码时，http请求中默认的携带图片验证码信息的参数的名称
     */
    String DEFAULT_PARAMETER_NAME_CODE_IMAGE = "image_code";
    /**
     * 验证短信验证码时，http请求中默认的携带短信验证码信息的参数的名称
     */
    String DEFAULT_PARAMETER_NAME_CODE_SMS = "sms_code";
    /**
     * 验证邮箱验证码时，http请求中默认的携带短信验证码信息的参数的名称
     */
    String DEFAULT_PARAMETER_NAME_CODE_EMAIL = "email_code";
    /**
     * 发送短信验证码 或 验证短信验证码时，传递手机号的参数的名称
     */
    String DEFAULT_PARAMETER_NAME_MOBILE = "mobile";
    /**
     * 发送邮箱验证码 或 验证邮箱验证码时，传递邮箱的参数的名称
     */
    String DEFAULT_PARAMETER_NAME_EMAIL = "email";
}
