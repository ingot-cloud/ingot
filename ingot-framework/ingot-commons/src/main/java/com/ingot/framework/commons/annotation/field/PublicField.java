package com.ingot.framework.commons.annotation.field;

import java.lang.annotation.*;

/**
 * <p>显式分类受控 DTO 的固定公开属性，不将其加入动态字段授权。</p>
 * @author jy
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicField {
}
