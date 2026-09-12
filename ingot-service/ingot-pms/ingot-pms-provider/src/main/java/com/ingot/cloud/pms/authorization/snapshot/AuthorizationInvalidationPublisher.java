package com.ingot.cloud.pms.authorization.snapshot;

import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationInvalidationEvent;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotAccess;
import com.ingot.framework.eventbus.InvalidationBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * <p>授权写操作事务提交后清理本节点快照热缓存并广播跨节点失效。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class AuthorizationInvalidationPublisher {

    private final ObjectProvider<AuthorizationSnapshotAccess> snapshotAccessProvider;
    private final ObjectProvider<InvalidationBus> invalidationBusProvider;

    /**
     * 事务提交后执行失效；无事务时立即执行。
     *
     * @param event 本地授权变更事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAuthorizationChanged(AuthorizationChangedSpringEvent event) {
        evictLocal();
        publishInvalidation();
    }

    private void evictLocal() {
        AuthorizationSnapshotAccess access = snapshotAccessProvider.getIfAvailable();
        if (access == null) {
            return;
        }
        try {
            access.evictAll();
            log.info("[AuthorizationSnapshot] origin local L1+L2 evicted");
        } catch (Exception ex) {
            log.warn("[AuthorizationSnapshot] origin local cache evict failed", ex);
        }
    }

    private void publishInvalidation() {
        InvalidationBus bus = invalidationBusProvider.getIfAvailable();
        if (bus == null) {
            log.debug("[AuthorizationSnapshot] InvalidationBus not present, skip broadcast");
            return;
        }
        try {
            bus.publish(AuthorizationInvalidationEvent.all());
            log.info("[AuthorizationSnapshot] published invalidation");
        } catch (Exception ex) {
            log.warn("[AuthorizationSnapshot] publish invalidation failed", ex);
        }
    }
}
