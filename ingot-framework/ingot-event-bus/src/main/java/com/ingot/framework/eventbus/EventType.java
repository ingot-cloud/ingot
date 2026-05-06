package com.ingot.framework.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注事件类的类型标识，类型即为该事件在总线上的 channel 后缀。
 * <p>
 * 命名约定：{@code <domain>.<action>[.<subaction>]}，全小写、点分隔；同一 domain 内事件类型唯一。
 * 例如：{@code dict.invalidate}、{@code social.wechat.refresh}。
 * </p>
 *
 * @author jy
 * @since 2026/4/27
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventType {

    /**
     * 事件类型字符串。
     */
    String value();
}
