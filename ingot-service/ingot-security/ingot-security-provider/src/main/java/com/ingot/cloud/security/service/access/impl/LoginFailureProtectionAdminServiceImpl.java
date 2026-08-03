package com.ingot.cloud.security.service.access.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.model.enums.LoginFailureDimension;
import com.ingot.cloud.security.mapper.LoginFailureProtectionPolicyMapper;
import com.ingot.cloud.security.model.domain.LoginFailureProtectionPolicy;
import com.ingot.cloud.security.service.access.LoginFailureProtectionAdminService;
import com.ingot.cloud.security.service.policy.SecurityPolicyChangedSpringEvent;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 登录失败保护策略管理面 Service 实现。
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class LoginFailureProtectionAdminServiceImpl implements LoginFailureProtectionAdminService {

    private final LoginFailureProtectionPolicyMapper policyMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AssertionChecker assertionChecker;

    @Override
    public List<LoginFailureProtectionPolicy> list() {
        return policyMapper.selectList(Wrappers.<LoginFailureProtectionPolicy>lambdaQuery()
                .orderByAsc(LoginFailureProtectionPolicy::getDimension));
    }

    @Override
    public LoginFailureProtectionPolicy getByDimension(LoginFailureDimension dimension) {
        assertionChecker.checkOperation(dimension != null, "SecurityPolicy.LoginFailureDimensionNotNull");
        return policyMapper.selectOne(Wrappers.<LoginFailureProtectionPolicy>lambdaQuery()
                .eq(LoginFailureProtectionPolicy::getDimension, dimension));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginFailureProtectionPolicy upsert(LoginFailureProtectionPolicy policy) {
        validate(policy);
        LoginFailureProtectionPolicy existing = getByDimension(policy.getDimension());
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
        eventPublisher.publishEvent(new SecurityPolicyChangedSpringEvent(this, SecurityPolicyDomain.LOGIN_FAILURE_PROTECTION));
        return policy;
    }

    private void validate(LoginFailureProtectionPolicy policy) {
        assertionChecker.checkOperation(policy != null, "SecurityPolicy.LoginFailurePolicyNotNull");
        assertionChecker.checkOperation(policy.getDimension() != null, "SecurityPolicy.LoginFailureDimensionNotNull");
        assertionChecker.checkOperation(policy.getMaxAttempts() != null && policy.getMaxAttempts() >= 1,
                "SecurityPolicy.LoginFailureMaxAttemptsInvalid");
        assertionChecker.checkOperation(policy.getWindowMinutes() != null && policy.getWindowMinutes() >= 1,
                "SecurityPolicy.LoginFailureWindowInvalid");
        assertionChecker.checkOperation(policy.getBlockTtlSec() != null && policy.getBlockTtlSec() >= 60,
                "SecurityPolicy.LoginFailureBlockTtlInvalid");
        assertionChecker.checkOperation(policy.getBlockKeyType() != null && !policy.getBlockKeyType().isBlank(),
                "SecurityPolicy.LoginFailureBlockKeyTypeInvalid");
    }
}
