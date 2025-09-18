package com.ingot.framework.security.access;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ingot.framework.security.oauth2.server.resource.access.expression.InSecurityExpression;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <p>Description  : {@link InSecurityExpression#adminOrHasAnyAuthority(String...)}.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/9/2.</p>
 * <p>Time         : 10:32.</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@ingot.adminOrHasAnyAuthority('{value}'.split(','))")
public @interface AdminOrHasAnyAuthority {
    /**
     * 权限
     *
     * @return {@link String[] }
     */
    String[] value();
}
