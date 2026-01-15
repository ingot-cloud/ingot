package com.ingot.framework.data.mybatis.common.annotation;

import java.lang.annotation.*;

/**
 * <p>Description  : 租户表注解.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/9/3.</p>
 * <p>Time         : 13:53.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface TenantTable {
}
