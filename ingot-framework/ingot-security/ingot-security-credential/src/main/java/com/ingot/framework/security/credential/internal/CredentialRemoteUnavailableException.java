package com.ingot.framework.security.credential.internal;

/**
 * 凭证策略远程数据源不可用异常。
 *
 * <p>由 {@code RemoteCredentialPolicyConfigService} 在远程调用失败（响应为空 / 非成功码 / 连接超时 /
 * 抛出异常）时抛出，用于把「调用失败」与「成功返回空（合法无策略）」区分开：仅前者触发
 * {@code ResilientCredentialPolicyConfigService} 的降级兜底阶梯（LKG → Nacos 地板），后者按合法空直接接受。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class CredentialRemoteUnavailableException extends RuntimeException {

    public CredentialRemoteUnavailableException(String message) {
        super(message);
    }

    public CredentialRemoteUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
