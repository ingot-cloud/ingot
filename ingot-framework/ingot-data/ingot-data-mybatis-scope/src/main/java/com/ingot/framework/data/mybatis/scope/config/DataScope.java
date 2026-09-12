package com.ingot.framework.data.mybatis.scope.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>声明方法按指定资源与具体操作执行行级数据范围，不判定接口能否调用。</p>
 *
 * <p>{@link #resource()} 对应已登记的 {@code platform_resource.code}。
 * {@link #permission()} 是从快照选择规则的键，同一资源上不同操作可以有不同范围。
 * 功能准入由 {@code @AdminOrHasAnyAuthority} / {@code @HasAnyAuthority} 完成。
 * 表字段映射来自可信配置或 {@link com.ingot.framework.data.mybatis.common.annotation.DataScopeTable}，
 * 禁止把请求参数拼进 SQL 标识。无匹配规则时查询变为 {@code 1=2}，不解释为全部。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see com.ingot.framework.security.access.AdminOrHasAnyAuthority
 * @see com.ingot.framework.security.access.HasAnyAuthority
 * @apiNote 受保护查询必须同时加功能注解；本注解漏配功能门禁时读接口得到空结果，不能当成可省略 PreAuthorize。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface DataScope {

    /**
     * 资源编码，必须与平台资源目录中的稳定编码一致。
     *
     * @return 资源编码
     */
    String resource();

    /**
     * 具体操作权限码，用作选择数据规则的键，不是功能准入判定。
     *
     * @return ACTION 编码
     */
    String permission();
}
