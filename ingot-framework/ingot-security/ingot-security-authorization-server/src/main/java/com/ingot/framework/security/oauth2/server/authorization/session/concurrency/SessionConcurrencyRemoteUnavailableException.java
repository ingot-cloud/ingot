package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import com.ingot.framework.cache.spi.RemoteUnavailableException;

/**
 * <p>安全中心并发策略接口不可用的信号异常，触发分层缓存的降级阶梯。</p>
 *
 * <p>只有连接失败、超时或业务失败码才应抛出；调用成功但策略表为空是合法空，
 * 必须正常返回，否则「当前没有配置任何策略」会被误判成故障并污染 LKG。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see FeignSessionConcurrencyPolicyLoader
 */
public class SessionConcurrencyRemoteUnavailableException extends RemoteUnavailableException {

    public SessionConcurrencyRemoteUnavailableException(String message) {
        super(message);
    }

    public SessionConcurrencyRemoteUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
