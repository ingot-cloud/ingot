package com.ingot.framework.security.credential.internal;

import com.ingot.framework.cache.spi.RemoteUnavailableException;

/**
 * <p>凭证策略远端接口不可用（Feign 失败、非 success 响应等），触发分层缓存的降级阶梯。</p>
 *
 * <p>继承框架的 {@link RemoteUnavailableException} 是接入降级链的前提。
 * 远端返回成功但策略列表为空属于合法空，不应抛出本异常。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see RemoteCredentialPolicyConfigService
 */
public class CredentialRemoteUnavailableException extends RemoteUnavailableException {

    public CredentialRemoteUnavailableException(String message) {
        super(message);
    }

    public CredentialRemoteUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
