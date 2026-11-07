package com.ingot.cloud.iam.domain.policy;

/**
 * <p>声明字段策略缓存的稳定键语义，读取用例不依赖配置装配类。</p>
 * @author jy
 * @since 1.0.0
 */
public final class FieldPolicyCacheKeys {
    /** 资源最新默认策略引用的键前缀。 */
    public static final String LATEST_PREFIX = "latest:";

    private FieldPolicyCacheKeys() { }

    /** 构造指定资源的最新默认策略引用键，不改变既有缓存协议。 */
    public static String latest(long resourceId) {
        return LATEST_PREFIX + resourceId;
    }
}
