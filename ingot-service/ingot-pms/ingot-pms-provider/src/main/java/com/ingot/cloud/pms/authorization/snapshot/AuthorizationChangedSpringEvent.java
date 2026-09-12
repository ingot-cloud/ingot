package com.ingot.cloud.pms.authorization.snapshot;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * <p>授权变更的本地 Spring 事件，事务提交后转为跨节点快照失效广播。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
public class AuthorizationChangedSpringEvent extends ApplicationEvent {

    /**
     * 是否全量失效。
     */
    private final boolean all;

    public AuthorizationChangedSpringEvent(Object source, boolean all) {
        super(source);
        this.all = all;
    }

    /**
     * 构造全量失效事件。
     *
     * @param source 发布方
     * @return 事件
     */
    public static AuthorizationChangedSpringEvent all(Object source) {
        return new AuthorizationChangedSpringEvent(source, true);
    }
}
