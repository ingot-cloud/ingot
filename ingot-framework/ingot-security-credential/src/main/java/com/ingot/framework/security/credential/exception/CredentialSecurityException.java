package com.ingot.framework.security.credential.exception;

/**
 * 凭证安全异常基类
 *
 * @author jymot
 * @since 2026-01-21
 */
public class CredentialSecurityException extends RuntimeException {

    public CredentialSecurityException(String message) {
        super(message);
    }

    public CredentialSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
