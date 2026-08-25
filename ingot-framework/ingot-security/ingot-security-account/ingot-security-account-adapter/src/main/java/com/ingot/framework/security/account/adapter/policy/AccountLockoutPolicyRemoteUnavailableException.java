package com.ingot.framework.security.account.adapter.policy;

import com.ingot.framework.cache.spi.RemoteUnavailableException;

/**
 * <p>账号锁定策略远端接口不可用（Feign 失败、非 success、空列表），触发分层缓存降级阶梯。</p>
 *
 * <p>锁定策略不允许合法空：空列表与调用失败同样视为不可用，避免 fail-open 成「未启用锁定」。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class AccountLockoutPolicyRemoteUnavailableException extends RemoteUnavailableException {

    /**
     * @param message 失败说明
     */
    public AccountLockoutPolicyRemoteUnavailableException(String message) {
        super(message);
    }

    /**
     * @param message 失败说明
     * @param cause   远端调用异常
     */
    public AccountLockoutPolicyRemoteUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
