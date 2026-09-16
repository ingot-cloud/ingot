package com.ingot.cloud.iam.adapter;

import java.time.LocalDateTime;

import com.ingot.cloud.iam.identity.AccountCredentialRepository;
import com.ingot.cloud.iam.persistence.AccountWriteRepository;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.port.outbound.UserCredentialPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>将凭证端口接到新模型 {@code iam_account}，更新受版本条件约束，不回退旧用户表。</p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Component
@RequiredArgsConstructor
public class IamUserCredentialPortAdapter implements UserCredentialPort {
    private final AccountCredentialRepository accounts;
    private final AccountWriteRepository accountWrites;

    /** {@inheritDoc} */
    @Override
    public String getPasswordHash(Long userId, UserTypeEnum userType) {
        return accounts.findById(userId).map(AccountCredentialRepository.AccountCredentials::passwordHash)
                .orElse(null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePassword(Long userId, UserTypeEnum userType,
                                  String newPasswordHash, LocalDateTime changedAt,
                                  Long expectedVersion, boolean mustChangePwd) {
        return accountWrites.updatePassword(userId, newPasswordHash, changedAt, expectedVersion, mustChangePwd) > 0;
    }

    /** {@inheritDoc} */
    @Override
    public LocalDateTime getPasswordChangedAt(Long userId, UserTypeEnum userType) {
        return accounts.findById(userId)
                .map(AccountCredentialRepository.AccountCredentials::passwordChangedAt)
                .orElse(null);
    }
}
