package com.ingot.framework.data.mybatis.scope.context;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * <p>Description  : DataScopeContextHolder.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/4/1.</p>
 * <p>Time         : 14:32.</p>
 */
public class DataScopeContextHolder {

    private static final ThreadLocal<List<Long>> THREAD_CONTEXT_SCOPE = new TransmittableThreadLocal<>();
    private static final ThreadLocal<Long> THREAD_CONTEXT_USER_SCOPE = new TransmittableThreadLocal<>();
    private static final ThreadLocal<Boolean> THREAD_CONTEXT_SKIP_FLAG = new TransmittableThreadLocal<>();

    public static void skip() {
        THREAD_CONTEXT_SKIP_FLAG.set(Boolean.TRUE);
    }

    public static boolean isSkip() {
        return BooleanUtil.isTrue(THREAD_CONTEXT_SKIP_FLAG.get());
    }

    public static void setScopes(List<Long> scopes) {
        THREAD_CONTEXT_SCOPE.set(scopes);
    }

    public static List<Long> getScopes() {
        return THREAD_CONTEXT_SCOPE.get();
    }

    public static void setUserScope(Long userScope) {
        THREAD_CONTEXT_USER_SCOPE.set(userScope);
    }

    public static Long getUserScope() {
        return THREAD_CONTEXT_USER_SCOPE.get();
    }

    public static boolean isNotEmpty() {
        return CollUtil.isNotEmpty(getScopes()) || getUserScope() != null;
    }

    public static boolean isEmpty() {
        return !isNotEmpty();
    }

    public static void clear() {
        THREAD_CONTEXT_SCOPE.remove();
        THREAD_CONTEXT_USER_SCOPE.remove();
        THREAD_CONTEXT_SKIP_FLAG.remove();
    }
}
