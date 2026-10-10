package com.ingot.framework.commons.model.iam.extension;

import java.time.Instant;

/**
 * <p>字段配置来源的绝对有效期；派生缓存不得重新延长来源的授权窗口。</p>
 * @author jy
 * @since 1.0.0
 */
public final class FieldPolicyLifetime {
    /** 读取配置和派生授权的最大有效秒数。 */
    public static final long MAX_SECONDS = 30;

    private FieldPolicyLifetime() { }

    /** 当前来源读取所允许的最晚到期时刻。 */
    public static Instant deadline() { return Instant.now().plusSeconds(MAX_SECONDS); }

    /** 派生结果继承两个来源中较早的到期时刻。 */
    public static Instant earliest(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }
}
