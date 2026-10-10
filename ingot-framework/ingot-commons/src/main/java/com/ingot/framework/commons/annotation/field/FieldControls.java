package com.ingot.framework.commons.annotation.field;

import java.lang.annotation.*;

/**
 * <p>同一接口可以独立声明响应、嵌套输入及筛选 DTO 的接入。</p>
 * @author jy
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldControls {
    /** 接口实际处理的独立 DTO 接入点。 */
    FieldControl[] value();
}
