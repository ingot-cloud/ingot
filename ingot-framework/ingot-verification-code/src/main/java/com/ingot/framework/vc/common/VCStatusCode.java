package com.ingot.framework.vc.common;

import com.ingot.framework.core.model.status.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : VCStatusCode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/4/27.</p>
 * <p>Time         : 11:03 AM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum VCStatusCode implements StatusCode {
    Illegal("illegal_vc", "验证码异常");

    private final String code;
    private final String text;
}
