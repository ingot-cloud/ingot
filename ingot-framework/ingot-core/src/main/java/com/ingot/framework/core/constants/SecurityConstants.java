package com.ingot.framework.core.constants;

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
