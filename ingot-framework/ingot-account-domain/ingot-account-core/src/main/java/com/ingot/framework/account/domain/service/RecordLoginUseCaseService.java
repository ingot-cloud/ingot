package com.ingot.framework.account.domain.service;

import java.time.LocalDateTime;

import com.ingot.framework.account.domain.config.AccountDomainProperties;
import com.ingot.framework.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.account.domain.model.enums.LockReason;
import com.ingot.framework.account.domain.port.inbound.LockAccountUseCase;
import com.ingot.framework.account.domain.port.inbound.RecordLoginUseCase;
import com.ingot.framework.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 记录登录用例实现
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordLoginUseCaseService implements RecordLoginUseCase {

    private final UserAccountPort userAccountPort;
    private final LockStatePort lockStatePort;
    private final SecurityEventPort securityEventPort;
    private final LockAccountUseCase lockAccountUseCase;
    private final AccountDomainProperties accountProperties;
    private final CredentialSecurityService credentialSecurityService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSuccess(LoginCommand command) {
        log.info("记录用户 {} 登录成功", command.getUserId());

        // 1. 更新最后登录时间和IP
        userAccountPort.updateLastLogin(
                command.getUserId(),
                command.getUserType(),
                LocalDateTime.now(),
                command.getClientIp()
        );

        // 2. 重置失败计数
        lockStatePort.resetFailCount(command.getUserId(), command.getUserType());

        // 2.1 宽限期扣减：密码已过期但仍在宽限内的成功登录，消费一次宽限次数
        //     未启用过期策略或密码未过期时内部自动跳过
        try {
            credentialSecurityService.consumeGraceLoginOnSuccess(command.getUserId());
        } catch (Exception e) {
            log.warn("[RecordLogin] 宽限期扣减异常，忽略 userId={}", command.getUserId(), e);
        }

        // 3. 发布登录成功事件
        AccountSecurityEvent event = AccountSecurityEvent.loginSuccess(
                command.getUserId(),
                command.getUserType(),
                command.getUsername(),
                command.getClientIp(),
                command.getUserAgent(),
                command.getTenantId()
        );
        securityEventPort.publishEvent(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordFailure(LoginCommand command) {
        log.warn("记录用户 {} 登录失败: {}", command.getUserId(), command.getFailureReason());

        // 1. 原子递增失败计数，直接拿到最新值，无需再查一次
        int newFailCount = lockStatePort.incrementFailCount(command.getUserId(), command.getUserType());

        // 2. 发布登录失败事件
        AccountSecurityEvent event = AccountSecurityEvent.loginFailure(
                command.getUserId(),
                command.getUserType(),
                command.getUsername(),
                command.getClientIp(),
                command.getFailureReason()
        );
        securityEventPort.publishEvent(event);

        // 3. 检查是否需要自动锁定
        if (accountProperties.getLockout().isEnabled()) {
            int maxAttempts = accountProperties.getLockout().getMaxAttempts();
            if (newFailCount >= maxAttempts) {
                log.warn("用户 {} 登录失败次数达到 {}，触发自动锁定", command.getUserId(), newFailCount);

                Integer lockDuration = accountProperties.getLockout().getLockDurationMinutes();
                lockAccountUseCase.lockAutomatically(
                        command.getUserId(),
                        command.getUserType(),
                        LockReason.LOGIN_FAIL_EXCEED,
                        lockDuration == 0 ? null : lockDuration  // 0 表示永久锁定
                );
            }
        }
    }
}
