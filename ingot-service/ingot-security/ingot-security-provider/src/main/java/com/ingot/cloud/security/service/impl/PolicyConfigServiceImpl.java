package com.ingot.cloud.security.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.security.mapper.CredentialPolicyConfigMapper;
import com.ingot.cloud.security.model.domain.CredentialPolicyConfig;
import com.ingot.cloud.security.service.PolicyConfigService;
import com.ingot.cloud.security.service.credential.CredentialPolicyChangedSpringEvent;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 策略配置服务实现。
 *
 * @author jymot
 * @since 2026-01-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyConfigServiceImpl implements PolicyConfigService {
    private final CredentialPolicyConfigMapper policyConfigMapper;
    private final AssertionChecker assertionChecker;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<CredentialPolicyConfig> getAllPolicyConfigs() {
        return policyConfigMapper.selectList(
                Wrappers.<CredentialPolicyConfig>lambdaQuery()
                        .orderByAsc(CredentialPolicyConfig::getPriority)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialPolicyConfig savePolicyConfig(CredentialPolicyConfig config) {
        assertionChecker.checkOperation(config.getPolicyType() != null, "PolicyConfigServiceImpl.TypeNotNull");

        config.setCreatedAt(DateUtil.now());
        config.setUpdatedAt(config.getCreatedAt());
        policyConfigMapper.insert(config);

        publishChanged();
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialPolicyConfig updatePolicyConfig(CredentialPolicyConfig config) {
        assertionChecker.checkOperation(config.getId() != null, "PolicyConfigServiceImpl.IdNotNull");
        assertionChecker.checkOperation(config.getPolicyType() != null, "PolicyConfigServiceImpl.TypeNotNull");

        CredentialPolicyConfig current = policyConfigMapper.selectById(config.getId());
        assertionChecker.checkOperation(current != null, "PolicyConfigServiceImpl.ConfigNotFound");
        assert current != null;
        assertionChecker.checkOperation(current.getPolicyType() == config.getPolicyType(),
                "PolicyConfigServiceImpl.TypeCantModified");

        config.setUpdatedAt(DateUtil.now());
        policyConfigMapper.updateById(config);

        publishChanged();
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePolicyConfig(Long id) {
        log.info("删除策略配置 - ID: {}", id);

        CredentialPolicyConfig config = policyConfigMapper.selectById(id);
        if (config != null) {
            policyConfigMapper.deleteById(id);
            log.info("策略配置删除成功 - ID: {}", id);
        }

        publishChanged();
    }

    private void publishChanged() {
        eventPublisher.publishEvent(new CredentialPolicyChangedSpringEvent(this));
    }
}
