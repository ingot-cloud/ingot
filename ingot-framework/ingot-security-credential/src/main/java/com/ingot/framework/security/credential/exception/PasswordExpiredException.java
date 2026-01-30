package com.ingot.framework.security.credential.exception;

/**
 * 密码已过期异常
 *
 * @author jymot
 * @since 2026-01-21
 */
public class PasswordExpiredException extends CredentialSecurityException {

    public PasswordExpiredException(String message) {
        super(message);
    }

    public PasswordExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
