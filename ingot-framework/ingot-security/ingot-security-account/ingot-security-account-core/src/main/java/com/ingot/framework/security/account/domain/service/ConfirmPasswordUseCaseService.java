package com.ingot.framework.security.account.domain.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.security.api.model.enums.SecurityEventType;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.ConfirmPasswordFailedException;
import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.account.domain.model.LockState;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.model.enums.LockReason;
import com.ingot.framework.security.account.domain.port.inbound.ConfirmPasswordUseCase;
import com.ingot.framework.security.account.domain.port.inbound.LockAccountUseCase;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 确认当前账号口令，失败计入登录失败锁定，成功只清失败计数。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmPasswordUseCaseService implements ConfirmPasswordUseCase {
    static final String STEP_UP_FAILURE = "step-up";

    private final UserAccountPort userAccountPort;
    private final LockStatePort lockStatePort;
    private final SecurityEventPort securityEventPort;
    private final LockAccountUseCase lockAccountUseCase;
    private final AccountLockoutPolicyLoader lockoutPolicyLoader;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(ConfirmPasswordCommand command) {
        if (command.getUserId() == null || command.getUserType() == null || StrUtil.isBlank(command.getPassword())) {
            throw new ConfirmPasswordFailedException();
        }
        UserAccount account = userAccountPort.findById(command.getUserId(), command.getUserType())
                .orElseThrow(ConfirmPasswordFailedException::new);
        if (!passwordEncoder.matches(command.getPassword(), account.getPassword())) {
            recordFailure(account);
            throw new ConfirmPasswordFailedException();
        }
        lockStatePort.resetFailCount(command.getUserId(), command.getUserType());
    }

    private void recordFailure(UserAccount account) {
        Long userId = account.getId();
        UserTypeEnum userType = account.getUserType();
        if (isCurrentlyLocked(userId, userType)) {
            publishFailure(account);
            return;
        }
        LockoutPolicy lockout = lockoutPolicyLoader.getLockoutPolicy(userType);
        if (lockout.enabled()) {
            int windowMinutes = lockout.attemptWindowMinutes();
            lockStatePort.findByUser(userId, userType).ifPresent(state -> {
                if (state.getLastFailedAt() != null
                        && !state.getLastFailedAt().plusMinutes(windowMinutes).isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
                    lockStatePort.resetFailCount(userId, userType);
                }
            });
        }
        int newFailCount = lockStatePort.incrementFailCount(userId, userType);
        publishFailure(account);
        if (lockout.enabled() && newFailCount >= lockout.maxAttempts()) {
            int lockDuration = lockout.lockDurationMinutes();
            lockAccountUseCase.lockAutomatically(userId, userType, LockReason.LOGIN_FAIL_EXCEED,
                    lockDuration == 0 ? null : lockDuration);
        }
    }

    private void publishFailure(UserAccount account) {
        securityEventPort.publishEvent(AccountSecurityEvent.builder()
                .userId(account.getId())
                .userType(account.getUserType())
                .eventType(SecurityEventType.LOGIN_FAILURE)
                .result(false)
                .reasonDetail(STEP_UP_FAILURE)
                .source(EventSource.IAM)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());
    }

    private boolean isCurrentlyLocked(Long userId, UserTypeEnum userType) {
        return lockStatePort.findByUser(userId, userType)
                .map(LockState::isLocked)
                .orElse(false);
    }
}
