package com.ingot.framework.core.constants;

/**
 * <p>Description  : TenantConstants.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-12-24.</p>
 * <p>Time         : 16:54.</p>
 */
public interface TenantConstants {

    /**
     * header 中保存租户编码的key
     */
    String TENANT_HEADER_KEY = "Tenant";

    /**
     * 默认租户ID
     */
    Integer DEFAULT_TENANT_ID = 1;
}
