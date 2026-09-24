package com.ingot.framework.security.account.domain;

/**
 * <p>当前账号口令确认失败，不表示登录成功，也不返回令牌。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class ConfirmPasswordFailedException extends RuntimeException {
    /**
     * 口令缺失、错误或账号不可确认。
     */
    public ConfirmPasswordFailedException() {
        super("confirm password failed");
    }
}
