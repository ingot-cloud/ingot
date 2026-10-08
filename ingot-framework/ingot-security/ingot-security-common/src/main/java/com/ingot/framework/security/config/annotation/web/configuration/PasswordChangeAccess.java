package com.ingot.framework.security.config.annotation.web.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>标记必须改密期间可访问的当前账号密码端点，仍须认证及在线会话校验。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordChangeAccess {
}
