package com.ingot.framework.security.credential.exception;

/**
 * 密码重复使用异常
 *
 * @author jymot
 * @since 2026-01-21
 */
public class PasswordReusedException extends CredentialSecurityException {

    public PasswordReusedException(String message) {
        super(message);
    }

    public PasswordReusedException(String message, Throwable cause) {
        super(message, cause);
    }
}
