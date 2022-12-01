package com.ingot.framework.core.utils.preconditions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Description  : IngotPreconditions.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/25.</p>
 * <p>Time         : 9:12 AM.</p>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface IngotPreconditions {

    /**
     * Preconditions 类
     */
    Class<?> value();
}
