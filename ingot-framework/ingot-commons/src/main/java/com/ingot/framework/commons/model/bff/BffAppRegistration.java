package com.ingot.framework.commons.model.bff;

import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import lombok.Data;

/**
 * <p>
 * 登录与管理共用一个可信 origin 的内部应用注册。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class BffAppRegistration {

    /** 固定应用键：platform-admin 或 tenant-admin，不等于 IAM applicationId。 */
    private String appId;

    /** 入口授权域，不能由浏览器改写。 */
    private AuthorizationDomain domain;

    /** 精确 scheme/host/port；同一应用的登录、管理和 API 共用。 */
    private String origin;

    /** 固定站内落地路径，默认 /，不接收客户端绝对 URL。 */
    private String defaultReturnTo = BffConstants.DEFAULT_RETURN_TO;

    /** Auth 注册的内部 OAuth 客户端 ID。 */
    private String oauthClientId;

    /** 内部 OAuth scope，默认 system。 */
    private String oauthScope = BffConstants.DEFAULT_OAUTH_SCOPE;

    /** 与 Auth RegisteredClient 精确一致的协议回调，不是浏览器业务跳转。 */
    private String oauthRedirectUri;

}
