package com.ingot.cloud.pms.authorization.snapshot;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * <p>授权写路径在事务内登记全量快照失效，提交后再清理缓存并广播。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
public class AuthorizationChangeNotifier {

    private final ApplicationEventPublisher publisher;

    /**
     * @param publisher Spring 事件发布器
     */
    public AuthorizationChangeNotifier(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * 标记授权事实已变更，事务提交后失效全部用户快照热缓存。
     */
    public void markAll() {
        publisher.publishEvent(AuthorizationChangedSpringEvent.all(this));
    }
}
