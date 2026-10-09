package com.ingot.framework.authorization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;

/**
 * <p>精确IAM操作准入，仅检查功能，目标及字段必须由服务执行。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireIamAction {

    /**
     * 管理域。
     * @return 管理域
     */
    AuthorizationDomain domain();

    /**
     * 应用编码。
     * @return 应用编码
     */
    String application();

    /**
     * 资源编码。
     * @return 资源编码
     */
    String resource();

    /**
     * 精确操作编码。
     * @return 操作码
     */
    String value();

}
