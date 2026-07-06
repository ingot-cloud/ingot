package com.ingot.framework.security.oauth2.server.resource.authentication;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * <p>Description  : JwtContextHolder.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/20.</p>
 * <p>Time         : 13:53.</p>
 */
public class JwtContextHolder {
    private static final ThreadLocal<String> THREAD_CONTEXT = new TransmittableThreadLocal<>();

    public static String get() {
        return THREAD_CONTEXT.get();
    }

    public static void set(String jti) {
        THREAD_CONTEXT.set(jti);
    }

    public static void clear() {
        THREAD_CONTEXT.remove();
    }
}
