package com.ingot.framework.security.config.annotation.web.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Description  : 用户鉴权放行注解，用于细化用户权限，对于不需要鉴权的接口可以不进行拦截.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/13.</p>
 * <p>Time         : 下午5:18.</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Permit {

    /**
     * 允许模式，默认为 {@link PermitMode#PUBLIC}
     * @return {@link PermitMode}
     */
    PermitMode mode() default PermitMode.PUBLIC;
}
