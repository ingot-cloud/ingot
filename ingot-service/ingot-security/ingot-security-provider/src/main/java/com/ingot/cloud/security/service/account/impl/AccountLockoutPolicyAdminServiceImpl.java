package com.ingot.cloud.security.service.account.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.mapper.AccountLockoutPolicyConfigMapper;
import com.ingot.cloud.security.model.domain.AccountLockoutPolicyConfig;
import com.ingot.cloud.security.service.account.AccountLockoutPolicyAdminService;
import com.ingot.cloud.security.service.policy.SecurityPolicyChangedSpringEvent;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>账号锁定策略管理面实现：校验、按用户类型 upsert、提交后发失效事件。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AccountLockoutPolicyAdminServiceImpl implements AccountLockoutPolicyAdminService {

    private final AccountLockoutPolicyConfigMapper policyMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AssertionChecker assertionChecker;

    @Override
    public List<AccountLockoutPolicyConfig> list() {
        return policyMapper.selectList(Wrappers.<AccountLockoutPolicyConfig>lambdaQuery()
                .orderByAsc(AccountLockoutPolicyConfig::getUserType));
    }

    @Override
    public AccountLockoutPolicyConfig getByUserType(UserTypeEnum userType) {
        assertionChecker.checkOperation(userType != null, "SecurityPolicy.AccountLockoutUserTypeInvalid");
        return policyMapper.selectOne(Wrappers.<AccountLockoutPolicyConfig>lambdaQuery()
                .eq(AccountLockoutPolicyConfig::getUserType, userType));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountLockoutPolicyConfig upsert(AccountLockoutPolicyConfig policy) {
        validate(policy);
        AccountLockoutPolicyConfig existing = getByUserType(policy.getUserType());
        if (existing == null) {
            policy.setCreatedAt(DateUtil.now());
            policy.setUpdatedAt(DateUtil.now());
            policyMapper.insert(policy);
        } else {
            policy.setId(existing.getId());
            policy.setCreatedAt(existing.getCreatedAt());
            policy.setUpdatedAt(DateUtil.now());
            policyMapper.updateById(policy);
        }
        eventPublisher.publishEvent(
                new SecurityPolicyChangedSpringEvent(this, SecurityPolicyDomain.ACCOUNT_LOCKOUT));
        return policy;
    }

    private void validate(AccountLockoutPolicyConfig policy) {
        assertionChecker.checkOperation(policy != null, "SecurityPolicy.AccountLockoutPolicyNotNull");
        assertionChecker.checkOperation(policy.getUserType() != null,
                "SecurityPolicy.AccountLockoutUserTypeInvalid");
        assertionChecker.checkOperation(policy.getMaxAttempts() != null && policy.getMaxAttempts() >= 1,
                "SecurityPolicy.AccountLockoutMaxAttemptsInvalid");
        assertionChecker.checkOperation(
                policy.getAttemptWindowMinutes() != null && policy.getAttemptWindowMinutes() >= 1,
                "SecurityPolicy.AccountLockoutWindowInvalid");
        assertionChecker.checkOperation(policy.getHintAfterAttempts() != null
                        && policy.getHintAfterAttempts() >= 1
                        && policy.getHintAfterAttempts() <= policy.getMaxAttempts(),
                "SecurityPolicy.AccountLockoutHintInvalid");
        assertionChecker.checkOperation(policy.getLockDurationMinutes() != null
                        && policy.getLockDurationMinutes() >= 0,
                "SecurityPolicy.AccountLockoutDurationInvalid");
        assertionChecker.checkOperation(
                policy.getUserType() != UserTypeEnum.APP || policy.getLockDurationMinutes() >= 1,
                "SecurityPolicy.AccountLockoutAppPermanentForbidden");
        if (policy.getEnabled() == null) {
            policy.setEnabled(Boolean.TRUE);
        }
    }
}
