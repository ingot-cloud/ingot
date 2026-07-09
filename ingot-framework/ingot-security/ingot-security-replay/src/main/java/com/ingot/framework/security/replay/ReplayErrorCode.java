package com.ingot.framework.security.replay;

import com.ingot.framework.commons.model.status.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>防重放能力的统一错误码。</p>
 *
 * <p>由 {@link ReplayGuard} 与幂等切面抛出，经全局异常处理转换为明文响应，
 * 供调用方（加密传输、验签、业务幂等等）识别重放、过期或存储不可用等情形。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum ReplayErrorCode implements ErrorCode {
    /**
     * 请求时间戳超出允许窗口
     */
    REPLAY_TIMESTAMP_EXPIRED("replay_ts_expired", "请求已过期"),
    /**
     * 请求随机数重复（重放或重复提交）
     */
    REPLAY_NONCE_DUPLICATE("replay_nonce_dup", "请求重复提交"),
    /**
     * 防重放存储不可用且策略为 fail-close
     */
    REPLAY_STORE_UNAVAILABLE("replay_store_unavailable", "防重放校验暂不可用");

    private final String code;
    private final String text;
}
