package com.ingot.framework.security.credential.exception;

/**
 * 密码强度不足异常
 *
 * @author jymot
 * @since 2026-01-21
 */
public class PasswordWeakException extends CredentialSecurityException {

    public PasswordWeakException(String message) {
        super(message);
    }

    public PasswordWeakException(String message, Throwable cause) {
        super(message, cause);
    }
}
