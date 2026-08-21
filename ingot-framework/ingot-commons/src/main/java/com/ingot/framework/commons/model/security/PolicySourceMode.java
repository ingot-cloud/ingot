package com.ingot.framework.commons.model.security;

/**
 * <p>安全策略的来源模式：本地 yaml / Nacos 地板，或安全中心远端拉取。</p>
 *
 * <p>与事件投递目标 {@code local}/{@code center} 不是同一语义，不可混用。
 * YAML 字面量为小写 {@code local}/{@code remote}，供 Spring Binder 与
 * {@code @ConditionalOnProperty} 使用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum PolicySourceMode {

    /**
     * 读本机 yaml / Nacos 地板，不访问安全中心。
     */
    LOCAL,

    /**
     * 从安全中心拉取策略；远端不可用时由各域自行决定 LKG / 地板 / fail-closed。
     */
    REMOTE;

    /**
     * YAML 与 Environment 字面量 {@code local}，供 {@code @ConditionalOnProperty} 使用。
     */
    public static final String VALUE_LOCAL = "local";

    /**
     * YAML 与 Environment 字面量 {@code remote}，供 {@code @ConditionalOnProperty} 使用。
     */
    public static final String VALUE_REMOTE = "remote";
}
