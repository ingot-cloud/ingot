package com.ingot.framework.commons.annotation.field;

import java.lang.annotation.*;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;

/**
 * <p>将实际 DTO 属性绑定到资源逻辑字段；空资源编码从可信接口上下文继承。</p>
 * @author jy
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
@com.fasterxml.jackson.annotation.JacksonAnnotationsInside
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldBinding {
    /** 资源内的稳定逻辑字段键，可以与 Java 和 JSON 属性名称不同。 */
    String key();
    /** 实际实现的用途，默认仅响应读取。 */
    FieldUse[] uses() default {FieldUse.READ};
    /** 显式资源的管理域，仅提供完整 applicationCode/resourceCode 时使用。 */
    AuthorizationDomain domain() default AuthorizationDomain.PLATFORM;
    /** 显式资源应用编码；为空时继承接口上下文。 */
    String applicationCode() default "";
    /** 显式资源编码；必须与 applicationCode 同时提供。 */
    String resourceCode() default "";
}
