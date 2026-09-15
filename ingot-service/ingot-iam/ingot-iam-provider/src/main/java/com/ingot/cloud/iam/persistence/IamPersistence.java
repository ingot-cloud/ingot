package com.ingot.cloud.iam.persistence;

/**
 * <p>声明 IAM 持久化显式边界的编译期配置值。</p>
 * @author jy
 * @since 1.0.0
 */
public final class IamPersistence {
    /** 新 Mapper 自行绑定域、租户和范围，不叠加旧拦截器语义。 */
    public static final String EXPLICIT_BOUNDARY = "true";
    private IamPersistence() { }
}
