package com.ingot.framework.security.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * <p>持有当前请求的会话 ID（sid），由资源服务器校验通过后写入。</p>
 *
 * <p>业务代码需要「撤销我自己这条会话」或在审计中标注会话来源时从此处取值，
 * 避免各处重复解析 JWT 声明。取值为 {@code null} 说明当前不在已认证的请求线程内。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension#getSid
 */
public class SessionContextHolder {

    private static final ThreadLocal<String> THREAD_CONTEXT = new TransmittableThreadLocal<>();

    public static String get() {
        return THREAD_CONTEXT.get();
    }

    public static void set(String sid) {
        THREAD_CONTEXT.set(sid);
    }

    public static void clear() {
        THREAD_CONTEXT.remove();
    }
}
