package com.ingot.framework.eventbus;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 失效广播事件基类。子类需通过 {@link EventType} 注解声明事件类型；
 * 子类可自由扩展 payload 字段。
 *
 * @author jy
 * @since 2026/4/27
 */
@Data
@NoArgsConstructor
public abstract class InvalidationEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点标识，由 bus 在发布前自动注入；接收方据此跳过自身回环。
     */
    private String origin;

    /**
     * 发布时间戳（毫秒），由 bus 在发布前自动注入。
     */
    private long timestamp;
}
