package com.ingot.framework.dict.client.model;

/**
 * 字典作用域。
 *
 * @author jy
 * @since 2026/4/25
 */
public enum DictScope {
    /**
     * 平台级共享
     */
    PLATFORM,
    /**
     * 租户隔离
     */
    TENANT,
    /**
     * 应用隔离
     */
    APP
}
