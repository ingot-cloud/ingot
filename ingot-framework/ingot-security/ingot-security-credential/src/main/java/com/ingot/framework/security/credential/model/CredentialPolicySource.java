package com.ingot.framework.security.credential.model;

/**
 * 凭证策略当前生效来源。
 *
 * <p>用于降级可观测：标识 {@code ResilientCredentialPolicyConfigService} 最近一次实际返回数据的来源，
 * 便于运维通过日志 / 指标 / 端点判断系统是否处于降级态。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum CredentialPolicySource {

    /**
     * 远程新鲜值（安全中心正常）。
     */
    REMOTE,

    /**
     * 最近成功快照（远程不可用，命中 LKG）。
     */
    LAST_KNOWN_GOOD,

    /**
     * Nacos 本地地板（远程不可用且无 LKG）。
     */
    LOCAL_FLOOR
}
