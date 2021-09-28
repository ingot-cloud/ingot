package com.ingot.framework.security.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * <p>Description  : ClientContextHolder.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/27.</p>
 * <p>Time         : 9:07 上午.</p>
 */
public class ClientContextHolder {

    private static final ThreadLocal<String> THREAD_CONTEXT = new TransmittableThreadLocal<>();

    public static String get() {
        return THREAD_CONTEXT.get();
    }

    public static void set(String clientId) {
        THREAD_CONTEXT.set(clientId);
    }

    public static void clear() {
        THREAD_CONTEXT.remove();
    }
}
