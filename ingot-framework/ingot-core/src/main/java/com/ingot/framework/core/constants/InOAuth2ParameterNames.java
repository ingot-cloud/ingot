package com.ingot.framework.core.constants;

import com.ingot.framework.core.model.enums.SocialTypeEnum;

/**
 * <p>Description  : OAuth2参数名称.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/14.</p>
 * <p>Time         : 2:50 下午.</p>
 */
public interface InOAuth2ParameterNames {

    /**
     * {@code access_token} - used in Authorization Response and Access Token Response.
     */
    String ACCESS_TOKEN = "accessToken";

    /**
     * {@code token_type} - used in Authorization Response and Access Token Response.
     */
    String TOKEN_TYPE = "tokenType";

    /**
     * {@code expires_in} - used in Authorization Response and Access Token Response.
     */
    String EXPIRES_IN = "expiresIn";

    /**
     * {@code refresh_token} - used in Access Token Request and Access Token Response.
     */
    String REFRESH_TOKEN = "refreshToken";

    /**
     * {@code user_type} - used in request
     */
    String USER_TYPE = "user_type";

    /**
     * 社交类型 {@link SocialTypeEnum}, 在请求中使用
     */
    String SOCIAL_TYPE = "social_type";

    /**
     * 社交code, 在请求中使用，配合{@link #SOCIAL_TYPE}
     */
    String SOCIAL_CODE = "social_code";

    /**
     * 预授权, 在请求中使用
     */
    String PRE_GRANT_TYPE = "pre_grant_type";

    /**
     * tenant授权允许列表，用于响应
     */
    String PRE_ALLOW_LIST = "allows";

    /**
     * 登录的tenant id
     */
    String TENANT = "org";

    /**
     * 传递sessionId字段
     */
    String SESSION_ID = "s_token";
}
