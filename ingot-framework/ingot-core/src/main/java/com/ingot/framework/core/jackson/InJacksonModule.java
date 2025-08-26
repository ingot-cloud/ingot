package com.ingot.framework.core.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * <p>Description  : InJacksonModule.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/8/25.</p>
 * <p>Time         : 09:31.</p>
 */
public abstract class InJacksonModule extends SimpleModule {
    /**
     * 排序
     *
     * @return Int
     */
    public abstract int getOrder();
}
