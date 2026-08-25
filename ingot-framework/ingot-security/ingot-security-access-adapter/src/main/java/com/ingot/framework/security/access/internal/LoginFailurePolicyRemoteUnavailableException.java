package com.ingot.framework.security.access.internal;

import com.ingot.framework.cache.spi.RemoteUnavailableException;

/**
 * <p>登录失败策略远端接口不可用（Feign 失败、非 success 响应等），触发分层缓存的降级阶梯。</p>
 *
 * <p>继承框架的 {@link RemoteUnavailableException} 是接入降级链的前提：
 * {@code ResilientCacheLayer} 只捕获该基类。远端返回成功但策略列表为空属于合法空，
 * 不应抛出本异常。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see RemoteLoginFailurePolicyLoader
 */
public class LoginFailurePolicyRemoteUnavailableException extends RemoteUnavailableException {

    public LoginFailurePolicyRemoteUnavailableException(String message) {
        super(message);
    }

    public LoginFailurePolicyRemoteUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
