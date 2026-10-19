package com.ingot.cloud.iam.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>使用同一IAM数据库事务管理器提交当前角色与资源委派写入。</p>
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class TenantLiveTransaction {
    private final PlatformTransactionManager transactions;

    /** 在REQUIRED事务内执行并传播失败，使快照、指针与审计整体回滚。 */
    public <T> T execute(TransactionCallback<T> callback) {
        return new TransactionTemplate(transactions).execute(callback);
    }
}
