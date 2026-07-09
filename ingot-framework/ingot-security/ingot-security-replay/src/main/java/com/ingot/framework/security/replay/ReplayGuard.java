package com.ingot.framework.security.replay;

/**
 * <p>防重放校验器，以"时间戳窗口 + nonce 去重"两步校验判定请求是否为重放。</p>
 *
 * <p>校验通过则放行，失败抛出 {@link com.ingot.framework.commons.error.BizException}
 * （错误码见 {@link ReplayErrorCode}）。通过 {@code namespace} 隔离不同业务场景（加密传输、验签、业务幂等等）。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see DefaultReplayGuard
 */
public interface ReplayGuard {

    /**
     * 执行防重放校验。
     *
     * @param namespace 场景命名空间，用于隔离不同来源的 nonce
     * @param nonce     请求随机数
     * @param timestamp 请求时间戳（epoch millis）
     */
    void check(String namespace, String nonce, long timestamp);
}
