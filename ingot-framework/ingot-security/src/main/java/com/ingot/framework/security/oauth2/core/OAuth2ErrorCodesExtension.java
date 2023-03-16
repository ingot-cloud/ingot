package com.ingot.framework.security.oauth2.core;

import com.ingot.framework.core.model.status.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : OAuth2ErrorCodesExtension.
 * {@link org.springframework.security.oauth2.core.OAuth2ErrorCodes}</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/17.</p>
 * <p>Time         : 2:33 下午.</p>
 */
@Getter
@RequiredArgsConstructor
public enum OAuth2ErrorCodesExtension implements StatusCode {
    USER_STATUS("user_status", "Illegal user status"),
    SIGN_OUT("user_sign_out", "User has been signed out"),
    NOT_ALLOW_CLIENT("not_allow_client", "The user is not allowed to access the client");

    private final String code;
    private final String text;
}
