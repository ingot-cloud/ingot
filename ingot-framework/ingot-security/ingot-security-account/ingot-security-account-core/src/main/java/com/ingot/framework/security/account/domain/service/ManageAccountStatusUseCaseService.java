package com.ingot.framework.security.account.domain.service;

import java.time.LocalDateTime;

import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.cloud.security.api.model.enums.SecurityEventType;
import com.ingot.framework.security.account.domain.port.inbound.ManageAccountStatusUseCase;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.account.domain.port.outbound.SessionRevocationPort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.security.account.domain.support.AfterCommitActions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号状态管理用例实现
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManageAccountStatusUseCaseService implements ManageAccountStatusUseCase {

    private final UserAccountPort userAccountPort;
    private final SecurityEventPort securityEventPort;
    private final SessionRevocationPort sessionRevocationPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableAccount(StatusCommand command) {
        log.info("启用账号 {}", command.getUserId());

        if (isAlreadyInTargetStatus(command, true)) {
            log.info("账号 {} 已处于启用状态，跳过重复启用", command.getUserId());
            return;
        }

        // 1. 更新启用状态
        userAccountPort.updateStatus(command.getUserId(), command.getUserType(), true);

        // 2. 发布账号启用事件
        AccountSecurityEvent event = AccountSecurityEvent.builder()
                .userId(command.getUserId())
                .userType(command.getUserType())
                .eventType(SecurityEventType.ACCOUNT_ENABLED)
                .reasonDetail(command.getReason())
                .source(command.getSource())
                .operatorId(command.getOperatorId())
                .operatorName(command.getOperatorName())
                .createdAt(LocalDateTime.now())
                .build();
        securityEventPort.publishEvent(event);

        log.info("账号 {} 已启用", command.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableAccount(StatusCommand command) {
        log.info("禁用账号 {}", command.getUserId());

        if (isAlreadyInTargetStatus(command, false)) {
            log.info("账号 {} 已处于禁用状态，跳过重复禁用", command.getUserId());
            return;
        }

        // 1. 更新禁用状态
        userAccountPort.updateStatus(command.getUserId(), command.getUserType(), false);

        // 2. 发布账号禁用事件
        AccountSecurityEvent event = AccountSecurityEvent.builder()
                .userId(command.getUserId())
                .userType(command.getUserType())
                .eventType(SecurityEventType.ACCOUNT_DISABLED)
                .reasonDetail(command.getReason())
                .source(command.getSource())
                .operatorId(command.getOperatorId())
                .operatorName(command.getOperatorName())
                .createdAt(LocalDateTime.now())
                .build();
        securityEventPort.publishEvent(event);

        // 3. 禁用只影响后续认证，已签发会话需联动下线
        AfterCommitActions.run(() -> sessionRevocationPort.revokeUserSessions(
                command.getUserId(), SessionRevokeReason.ACCOUNT_DISABLED, command.getOperatorId()));

        log.info("账号 {} 已禁用", command.getUserId());
    }

    private boolean isAlreadyInTargetStatus(StatusCommand command, boolean targetEnabled) {
        return userAccountPort.findById(command.getUserId(), command.getUserType())
                .map(UserAccount::isEnabled)
                .map(enabled -> enabled == targetEnabled)
                .orElse(false);
    }
}
