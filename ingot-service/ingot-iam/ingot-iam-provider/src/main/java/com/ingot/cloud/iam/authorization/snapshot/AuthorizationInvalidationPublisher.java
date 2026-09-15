package com.ingot.cloud.iam.authorization.snapshot;

import com.ingot.cloud.iam.evaluation.AuthorizationCacheConfiguration;
import com.ingot.cloud.iam.evaluation.JdbcAuthorizationEvaluator;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationInvalidationEvent;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotAccess;
import com.ingot.framework.eventbus.InvalidationBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * <p>授权写操作事务提交后清理本节点快照与 IAM 热缓存，并广播跨节点失效。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class AuthorizationInvalidationPublisher {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationInvalidationPublisher.class);

    private final ObjectProvider<AuthorizationSnapshotAccess> snapshotAccessProvider;
    private final ObjectProvider<InvalidationBus> invalidationBusProvider;
    private final ObjectProvider<LayeredCache<String, JdbcAuthorizationEvaluator.AuthorizationView>> viewCache;

    /**
     * 绑定旧快照、失效总线与 IAM 授权视图缓存。
     *
     * @param snapshotAccessProvider 数据范围快照
     * @param invalidationBusProvider 跨节点总线
     * @param viewCache IAM 授权视图缓存
     */
    public AuthorizationInvalidationPublisher(
            ObjectProvider<AuthorizationSnapshotAccess> snapshotAccessProvider,
            ObjectProvider<InvalidationBus> invalidationBusProvider,
            @Qualifier(AuthorizationCacheConfiguration.CACHE_BEAN_NAME)
            ObjectProvider<LayeredCache<String, JdbcAuthorizationEvaluator.AuthorizationView>> viewCache) {
        this.snapshotAccessProvider = snapshotAccessProvider;
        this.invalidationBusProvider = invalidationBusProvider;
        this.viewCache = viewCache;
    }

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
        if (access != null) {
            try {
                access.evictAll();
                log.info("[AuthorizationSnapshot] origin local L1+L2 evicted");
            } catch (Exception ex) {
                log.warn("[AuthorizationSnapshot] origin local cache evict failed", ex);
            }
        }
        LayeredCache<String, JdbcAuthorizationEvaluator.AuthorizationView> cache = viewCache.getIfAvailable();
        if (cache != null) {
            try {
                cache.evictAll();
                log.info("[IamAuthorization] origin local L1+L2 evicted");
            } catch (Exception ex) {
                log.warn("[IamAuthorization] origin local cache evict failed", ex);
            }
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
