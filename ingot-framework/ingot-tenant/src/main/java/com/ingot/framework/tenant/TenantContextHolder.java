package com.ingot.framework.tenant;

import cn.hutool.core.util.BooleanUtil;
import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * <p>保存当前线程的租户 ID 与是否跳过隔离的标志。</p>
 *
 * <p>未设置时 {@link #get()} 返回 {@code null}，表示当前请求不属于任何租户。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
public class TenantContextHolder {

    private static final ThreadLocal<Long> THREAD_CONTEXT = new TransmittableThreadLocal<>();
    private static final ThreadLocal<Boolean> THREAD_SKIP_FLAG = new TransmittableThreadLocal<>();

    /**
     * 写入当前线程的租户 ID；传入 {@code null} 表示不属于任何租户。
     *
     * @param id 租户 ID，可为 {@code null}
     */
    public static void set(Long id) {
        THREAD_CONTEXT.set(id);
    }

    /**
     * 读取当前线程的租户 ID。
     *
     * @return 租户 ID；未设置时为 {@code null}
     */
    public static Long get() {
        return THREAD_CONTEXT.get();
    }

    /**
     * 标记当前线程跳过租户隔离。
     */
    public static void skip() {
        THREAD_SKIP_FLAG.set(Boolean.TRUE);
    }

    /**
     * 当前线程是否跳过租户隔离。
     *
     * @return 已调用 {@link #skip()} 时为 {@code true}
     */
    public static Boolean isSkip() {
        return BooleanUtil.isTrue(THREAD_SKIP_FLAG.get());
    }

    /**
     * 清空当前线程的租户 ID 与跳过标志。
     */
    public static void clear() {
        THREAD_CONTEXT.remove();
        THREAD_SKIP_FLAG.remove();
    }
}
