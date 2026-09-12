package com.ingot.framework.data.mybatis.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>标记实体对应表需要行级数据范围，并可声明资源与列映射。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface DataScopeTable {

    /**
     * 绑定的资源编码；空表示沿用当前 {@code @DataScope} 上下文，不校验资源隔离。
     *
     * @return 资源编码
     */
    String resource() default "";

    /**
     * 部门范围列名；空表示使用 {@code ingot.mybatis.scope.scope-field-name}。
     *
     * @return 列名
     */
    String scopeColumn() default "";

    /**
     * 归属用户列名；空表示使用 {@code ingot.mybatis.scope.user-field-name}。
     *
     * @return 列名
     */
    String userColumn() default "";
}
