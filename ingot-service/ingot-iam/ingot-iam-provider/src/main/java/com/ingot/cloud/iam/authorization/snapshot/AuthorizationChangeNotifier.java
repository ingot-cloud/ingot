package com.ingot.cloud.iam.authorization.snapshot;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * <p>授权写路径在事务内登记全量快照失效，提交后再清理缓存并广播。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthorizationChangeNotifier {

    private final ApplicationEventPublisher publisher;



    /**
     * 标记授权事实已变更，事务提交后失效全部用户快照热缓存。
     */
    public void markAll() {
        publisher.publishEvent(AuthorizationChangedSpringEvent.all(this));
    }
}
