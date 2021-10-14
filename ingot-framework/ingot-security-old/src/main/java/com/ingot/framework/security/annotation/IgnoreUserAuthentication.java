package com.ingot.framework.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Description  : IgnoreUserAuthentication.</p>
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
