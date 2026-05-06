package com.ingot.framework.eventbus;

/**
 * 订阅句柄，调用 {@link #close()} 取消订阅。
 *
 * @author jy
 * @since 2026/4/27
 */
@FunctionalInterface
public interface Subscription extends AutoCloseable {

    @Override
    void close();
}
