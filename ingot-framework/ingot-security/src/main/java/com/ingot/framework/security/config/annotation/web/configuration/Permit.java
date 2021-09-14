package com.ingot.framework.security.config.annotation.web.configuration;

import java.lang.annotation.*;

/**
 * <p>Description  : IgnoreUserAuthentication.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/13.</p>
 * <p>Time         : 下午5:18.</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Permit {

    /**
     * 允许模式，默认为 {@link PermitModel#PUBLIC}
     * @return {@link PermitModel}
     */
    PermitModel model() default PermitModel.PUBLIC;
}
