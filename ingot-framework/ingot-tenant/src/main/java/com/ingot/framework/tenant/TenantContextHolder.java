package com.ingot.framework.tenant;

import cn.hutool.core.util.BooleanUtil;
import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * <p>Description  : TenantContextHolder.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/24.</p>
 * <p>Time         : 10:02 下午.</p>
 */
public class TenantContextHolder {

    private static final ThreadLocal<Long> THREAD_CONTEXT = new TransmittableThreadLocal<>();
    private static final ThreadLocal<Boolean> THREAD_CONTEXT_FLAG = new TransmittableThreadLocal<>();
    private static final ThreadLocal<Boolean> THREAD_SKIP_FLAG = new TransmittableThreadLocal<>();

    /**
     * 设置租户ID
     *
     * @param id 租户ID
     */
    public static void set(Long id) {
        THREAD_CONTEXT.set(id);
        THREAD_CONTEXT_FLAG.set(Boolean.FALSE);
    }

    /**
     * 获取租户ID
     *
     * @return 租户ID
     */
    public static Long get() {
        return THREAD_CONTEXT.get();
    }

    /**
     * 设置默认组合ID
     */
    public static void setDefault(Long id) {
        THREAD_CONTEXT.set(id);
        THREAD_CONTEXT_FLAG.set(Boolean.TRUE);
    }

    /**
     * 当前租户ID是否为默认租户ID
     *
     * @return Boolean
     */
    public static Boolean isUseDefault() {
        return BooleanUtil.isTrue(THREAD_CONTEXT_FLAG.get());
    }

    /**
     * 跳过租户处理
     */
    public static void skip() {
        THREAD_SKIP_FLAG.set(Boolean.TRUE);
    }

    /**
     * 是否跳过租户处理
     *
     * @return {@link Boolean}
     */
    public static Boolean isSkip() {
        return BooleanUtil.isTrue(THREAD_SKIP_FLAG.get());
    }

    /**
     * 清空
     */
    public static void clear() {
        THREAD_CONTEXT.remove();
        THREAD_CONTEXT_FLAG.remove();
        THREAD_SKIP_FLAG.remove();
    }
}
