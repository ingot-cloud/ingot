package com.ingot.framework.security.credential.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.model.status.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * CredentialErrorCode
 *
 * @author jy
 * @since 2026/2/2
 */
@Getter
@RequiredArgsConstructor
public enum CredentialErrorCode implements ErrorCode {
    EMPTY("pwd_empty", "密码不能为空"),
    EXPIRED("pwd_expired", "密码已过期，请立即修改密码"),
    EXPIRED_WITH_GRACE("pwd_expired_with_grace", "密码已过期，但仍在宽限期内"),
    EXPIRING_SOON("pwd_expiring_soon", "密码即将过期，请及时修改密码"),
    HISTORY_REUSE("pwd_history_reuse", "密码不能与最近使用密码相同"),
    STRENGTH("pwd_strength", "密码强度不足");

    @JsonValue
    private final String code;
    private final String text;
}
