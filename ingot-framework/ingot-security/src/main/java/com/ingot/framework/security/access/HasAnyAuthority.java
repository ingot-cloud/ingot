package com.ingot.framework.security.access;

import com.ingot.framework.security.oauth2.server.resource.access.expression.InSecurityExpression;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Description  : {@link InSecurityExpression#hasAnyAuthority(String...)}方法.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/9/2.</p>
 * <p>Time         : 10:27.</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@ingot.hasAnyAuthority('{value}'.split(','))")
public @interface HasAnyAuthority {
    /**
     * 权限
     *
     * @return Array {@link String }
     */
    String[] value();
}
