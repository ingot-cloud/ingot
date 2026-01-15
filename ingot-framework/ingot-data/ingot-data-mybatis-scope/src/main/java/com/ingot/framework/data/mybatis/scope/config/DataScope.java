package com.ingot.framework.data.mybatis.scope.config;

import java.lang.annotation.*;

/**
 * <p>Description  : DataScope.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/4/1.</p>
 * <p>Time         : 14:23.</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface DataScope {

}
