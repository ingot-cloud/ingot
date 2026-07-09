package com.ingot.framework.security.replay.idempotent;

import java.lang.annotation.*;

import com.ingot.framework.security.replay.ReplayErrorCode;

/**
 * <p>方法级幂等注解，在时间窗内拒绝重复请求。</p>
 *
 * <p>基于 {@link #key()}（支持 SpEL，可引用方法入参）在 {@link #ttl()} 时间窗内做去重，
 * 语义为"拒绝重复提交"，底层复用防重放的 nonce 存储。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Idempotent(key = "#dto.orderNo", namespace = "order", ttl = 60)
 * public R<Void> create(OrderDTO dto) { ... }
 * }</pre>
 *
 * @author jy
 * @since 1.0.0
 * @see IdempotentAspect
 * @apiNote 命中重复时抛出 {@link ReplayErrorCode#REPLAY_NONCE_DUPLICATE}；仅"拒绝重复"，不返回上次结果。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等键，支持 SpEL 表达式（如 {@code #dto.orderNo}、{@code #userId}）。
     */
    String key();

    /**
     * 场景命名空间，用于隔离不同业务的幂等键。
     */
    String namespace() default "idempotent";

    /**
     * 幂等窗口秒数；小于等于 0 时使用 {@code ingot.replay.window} 的配置。
     */
    long ttl() default 0;

    /**
     * 命中重复时的自定义提示，为空则使用默认错误文案。
     */
    String message() default "";
}
