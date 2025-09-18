package com.ingot.framework.core.utils.preconditions;

import java.lang.annotation.*;

/**
 * <p>Description  : InPreconditions.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/25.</p>
 * <p>Time         : 9:12 AM.</p>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface InPreconditions {

    /**
     * Preconditions 类
     */
    Class<?> value();
}
