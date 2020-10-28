package com.ingot.framework.security.annotation;

import java.lang.annotation.*;

/**
 * <p>Description  : IngotGateway.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/14.</p>
 * <p>Time         : 1:05 PM.</p>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface IgnoreUserAuthentication {
}
