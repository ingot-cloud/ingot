package com.ingot.framework.tenant;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * <p>Description  : TenantContextHolder.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/24.</p>
 * <p>Time         : 10:02 下午.</p>
 */
public class TenantContextHolder {

    private static final ThreadLocal<Integer> THREAD_CONTEXT = new TransmittableThreadLocal<>();

    public static Integer get() {
        return THREAD_CONTEXT.get();
    }

    public static void set(Integer id) {
        THREAD_CONTEXT.set(id);
    }

    public static void clear() {
        THREAD_CONTEXT.remove();
    }
}
