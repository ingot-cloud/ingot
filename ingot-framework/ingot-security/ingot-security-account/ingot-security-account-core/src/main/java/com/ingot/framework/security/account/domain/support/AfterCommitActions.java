package com.ingot.framework.security.account.domain.support;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p>在当前事务 afterCommit 执行动作；无活动事务时立即执行。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class AfterCommitActions {

    private AfterCommitActions() {
    }

    /**
     * 注册 afterCommit 回调；若当前无事务同步则同步执行。
     *
     * @param action 提交后动作
     */
    public static void run(Runnable action) {
        if (action == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }
}
