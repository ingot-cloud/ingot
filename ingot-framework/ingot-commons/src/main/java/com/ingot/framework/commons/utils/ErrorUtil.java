package com.ingot.framework.commons.utils;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>Description  : ErrorUtil.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/10/16.</p>
 * <p>Time         : 12:56.</p>
 */
public final class ErrorUtil {

    /**
     * try or throw
     *
     * @param supplier        {@link Supplier}
     * @param exceptionMapper {@link Function}
     * @param <T>             T
     * @return T
     */
    public static <T> T tryOrThrow(Supplier<T> supplier, Function<Exception, RuntimeException> exceptionMapper) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw exceptionMapper.apply(e);
        }
    }

    /**
     * try or throw
     *
     * @param exec            {@link Runnable}
     * @param exceptionMapper {@link Function}
     */
    public static void tryOrThrow(Runnable exec, Function<Exception, RuntimeException> exceptionMapper) {
        try {
            exec.run();
        } catch (Exception e) {
            throw exceptionMapper.apply(e);
        }
    }
}
