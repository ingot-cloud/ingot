package com.ingot.cloud.iam.domain.authorization;

/**
 * <p>声明授权视图的内部装配标识，缓存基础设施不依赖具体配置类。</p>
 * @author jy
 * @since 1.0.0
 */
public final class AuthorizationCacheNames {
    /** 授权视图缓存Bean名，保留已有限定注入语义。 */
    public static final String VIEW_CACHE_BEAN = "iamAuthorizationViewCache";

    private AuthorizationCacheNames() { }
}
