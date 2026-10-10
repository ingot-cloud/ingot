package com.ingot.framework.commons.annotation.field;

import java.lang.annotation.*;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;

/**
 * <p>声明业务接口的完整资源和精确操作，收集输入或响应 DTO 的实际绑定。</p>
 * @author jy
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FieldControls.class)
public @interface FieldControl {
    /** 资源管理域。 */
    AuthorizationDomain domain();
    /** 完整资源的应用编码。 */
    String applicationCode();
    /** 完整资源的资源编码。 */
    String resourceCode();
    /** 服务器声明的精确操作编码。 */
    String action();
    /** 当前接口处理用途。 */
    FieldUse use() default FieldUse.READ;
    /** 显式 DTO 类型，避免从分页或统一响应包装猜测类型。 */
    Class<?> valueType();
}
