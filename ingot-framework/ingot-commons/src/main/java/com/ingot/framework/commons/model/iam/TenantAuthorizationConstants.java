package com.ingot.framework.commons.model.iam;

/**
 * <p>声明组织当前授权的编译期边界常量。</p>
 * @author jy
 * @since 1.0.0
 */
public final class TenantAuthorizationConstants {
    /** 角色编码、名称及分组的最大长度。 */
    public static final int ROLE_TEXT_LIMIT = 128;
    /** 角色说明的最大长度。 */
    public static final int ROLE_DESCRIPTION_LIMIT = 512;
    /** IAM租户基础资源命名空间。 */
    public static final String CORE_APPLICATION = "iam-tenant";
    private TenantAuthorizationConstants() { }
    /** 资源参数键前缀；同资源同类型保持稳定。 */
    public static final String RESOURCE_PARAMETER_PREFIX="resource-";
    /** 资源参数键段分隔符。 */
    public static final String PARAMETER_SEPARATOR="-";
    /** 当前事实指纹算法，不作为授权凭证。 */
    public static final String FINGERPRINT_ALGORITHM="SHA-256";
    /** 为目录和独立资源委派生成同一稳定参数键。 */
    public static String resourceParameterKey(String resourceId,ScopeBindingKind kind) {
        return RESOURCE_PARAMETER_PREFIX+resourceId+PARAMETER_SEPARATOR+kind.getValue().toLowerCase(java.util.Locale.ROOT);
    }

}
