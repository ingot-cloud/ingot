package com.ingot.core.annotation;

import java.lang.annotation.*;

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
    Class value();
}
