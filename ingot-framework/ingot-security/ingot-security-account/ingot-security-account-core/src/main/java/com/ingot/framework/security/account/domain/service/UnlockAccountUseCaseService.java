package com.ingot.framework.security.account.domain.service;

import java.time.LocalDateTime;
import java.util.List;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.config.AccountMessageSource;
import com.ingot.framework.security.account.domain.model.AccountLockSignal;
import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.account.domain.model.LockState;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.port.inbound.UnlockAccountUseCase;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.security.account.domain.support.AfterCommitActions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 解锁账号用例实现
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnlockAccountUseCaseService implements UnlockAccountUseCase {

    /** 每批次处理的最大过期锁定记录数 */
    private static final int UNLOCK_BATCH_SIZE = 100;

    private final UserAccountPort userAccountPort;
    private final LockStatePort lockStatePort;
    private final SecurityEventPort securityEventPort;
    private final PlatformTransactionManager transactionManager;
    private final AccountLockSignalPort accountLockSignalPort;

    private final MessageSourceAccessor message = AccountMessageSource.getAccessor();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockManually(UnlockCommand command) {
        log.info("管理员 {} 手动解锁用户 {}", command.getOperatorId(), command.getUserId());

        if (!isCurrentlyLocked(command.getUserId(), command.getUserType())) {
            log.info("用户 {} 已处于未锁定状态，跳过重复手动解锁", command.getUserId());
            return;
        }

        // 1. 更新锁定状态为解锁
        lockStatePort.updateLockStatus(
                command.getUserId(),
                command.getUserType(),
                false,
                null,
                null,
                null,
                command.getOperatorId(),
                command.getOperatorName()
        );

        // 2. 重置失败计数
        lockStatePort.resetFailCount(command.getUserId(), command.getUserType());

        // 3. 同步更新冗余字段
        userAccountPort.updateLockStatus(command.getUserId(), command.getUserType(), false);

        // 4. 发布解锁事件
        AccountSecurityEvent event = AccountSecurityEvent.accountUnlocked(
                command.getUserId(),
                command.getUserType(),
                command.getReason(),
                command.getSource(),
                command.getOperatorId(),
                command.getOperatorName()
        );
        securityEventPort.publishEvent(event);

        scheduleClearLockSignal(command.getUserId(), command.getUserType());

        log.info("用户 {} 已解锁", command.getUserId());
    }

    /**
     * 自动解锁所有过期的临时锁定。
     * <p>
     * 设计要点：
     * <ul>
     *   <li>本方法不开启外层事务，查询与写入完全解耦</li>
     *   <li>使用游标分页（{@code id > lastId}），每批 {@value UNLOCK_BATCH_SIZE} 条，
     *       避免一次性加载全量数据和 OFFSET 大页问题</li>
     *   <li>每条记录的解锁操作通过 {@link TransactionTemplate} 在独立事务中执行，
     *       单条失败不影响后续记录的处理</li>
     * </ul>
     * </p>
     */
    @Override
    public void unlockExpired() {
        log.info("开始自动解锁过期的临时锁定");

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        LocalDateTime now = LocalDateTime.now();
        long lastId = 0L;
        int totalUnlocked = 0;
        int totalFailed = 0;

        while (true) {
            // 查询在事务外执行，避免长事务持有读锁
            List<LockState> batch = lockStatePort.findExpiredLocksByPage(now, lastId, UNLOCK_BATCH_SIZE);
            if (batch.isEmpty()) {
                break;
            }

            for (LockState lockState : batch) {
                try {
                    txTemplate.executeWithoutResult(status -> doUnlockExpired(lockState));
                    totalUnlocked++;
                } catch (Exception e) {
                    log.error("自动解锁用户 {} 失败，跳过继续处理",
                            lockState.getUserId(), e);
                    totalFailed++;
                }
                lastId = lockState.getId();
            }

            // 当返回记录数小于批次大小时，说明已是最后一页
            if (batch.size() < UNLOCK_BATCH_SIZE) {
                break;
            }
        }

        log.info("自动解锁完成：成功 {} 个，失败 {} 个", totalUnlocked, totalFailed);
    }

    /**
     * 对单条过期锁定记录执行解锁操作，由调用方通过 {@link TransactionTemplate} 保证事务边界。
     */
    private void doUnlockExpired(LockState lockState) {
        log.info("自动解锁用户 {}，锁定已过期", lockState.getUserId());

        if (!lockState.isLocked()) {
            log.info("用户 {} 已处于未锁定状态，跳过过期自动解锁", lockState.getUserId());
            return;
        }

        // 1. 更新 lock_state 表：解锁
        lockStatePort.updateLockStatus(
                lockState.getUserId(),
                lockState.getUserType(),
                false,
                null,
                null,
                null,
                null,
                null
        );

        // 2. 重置登录失败计数
        lockStatePort.resetFailCount(lockState.getUserId(), lockState.getUserType());

        // 3. 同步更新 user 表冗余字段
        userAccountPort.updateLockStatus(lockState.getUserId(), lockState.getUserType(), false);

        // 4. 发布系统自动解锁事件
        AccountSecurityEvent event = AccountSecurityEvent.accountUnlocked(
                lockState.getUserId(),
                lockState.getUserType(),
                message.getMessage("Account.LockExpired"),
                EventSource.SYSTEM,
                null,
                null
        );
        securityEventPort.publishEvent(event);

        scheduleClearLockSignal(lockState.getUserId(), lockState.getUserType());
    }

    private void scheduleClearLockSignal(Long userId, UserTypeEnum userType) {
        String username = userAccountPort.findById(userId, userType)
                .map(account -> account.getUsername())
                .orElse(null);
        AccountLockSignal signal = new AccountLockSignal(userId, userType, username, null);
        AfterCommitActions.run(() -> accountLockSignalPort.clearLocked(signal));
    }

    private boolean isCurrentlyLocked(Long userId, UserTypeEnum userType) {
        return lockStatePort.findByUser(userId, userType)
                .map(LockState::isLocked)
                .orElse(false);
    }
}
