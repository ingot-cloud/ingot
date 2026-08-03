package com.ingot.framework.security.access.internal;

/**
 * 登录失败策略远程不可用异常。
 *
 * @author jy
 * @since 1.0.0
 */
public class LoginFailurePolicyRemoteUnavailableException extends RuntimeException {

    public LoginFailurePolicyRemoteUnavailableException(String message) {
        super(message);
    }

    public LoginFailurePolicyRemoteUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
