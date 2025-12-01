package com.ingot.cloud.member.api.model.enums;

import com.ingot.framework.commons.model.status.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : MemberErrorCode.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 10:53.</p>
 */
@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    ExistUsername("PUE01", "用户名已存在"),
    ExistPhone("PUE02", "手机号已存在"),
    ExistEmail("PUE03", "Email已存在");

    private final String code;
    private final String text;
}
