package com.ingot.framework.core.constants;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

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
     * Jwt token enhancer claim key
     */
    interface TokenEnhancer {
        String KEY_USER_OBJECT = "user";
        String KEY_FIELD_USERNAME = "username";
        String KEY_FIELD_USER_ID = "id";
        String KEY_FIELD_DEPT_ID = "deptId";
        String KEY_FIELD_TENANT_ID = "tenantId";
        String KEY_FIELD_AUTH_TYPE = "authType";

        String KEY_JTI = "jti";
    }

    /**
     * Token 登录类型
     */
    @Getter
    enum TokenAuthType {
        /**
         * 登录类型，唯一类型，当前账号只能在一个地方登录
         */
        UNIQUE("unique", "unq"),
        /**
         * 登录类型，默认标准类型，单点登录不互踢
         */
        STANDARD("standard", "std");

        private final String type;
        private final String shortType;

        TokenAuthType(String type, String shortType) {
            this.type = type;
            this.shortType = shortType;
        }

        public static TokenAuthType get(String type) {
            if (StrUtil.isEmpty(type)) {
                return null;
            }
            TokenAuthType[] arr = TokenAuthType.values();
            for (TokenAuthType item : arr) {
                if (StrUtil.equals(item.type, type)) {
                    return item;
                }
            }

            return null;
        }
    }

    /**
     * Security path
     */
    interface Path {
        /**
         * 密码登录
         */
        String TOKEN_PASSWORD = "/auth/token";
    }

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
