package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

/**
 * <p>并发会话策略无法解析的信号异常，触发登录 fail-closed。</p>
 *
 * <p>仅在降级阶梯全部耗尽时抛出：远端不可用、无 LKG 快照、且本地地板被显式关闭。
 * 捕获方应拒绝本次新登录，不得退化为「无限并发」。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionConcurrencyPolicyResolver
 */
public class SessionPolicyUnavailableException extends RuntimeException {

    public SessionPolicyUnavailableException(String message) {
        super(message);
    }

    public SessionPolicyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
