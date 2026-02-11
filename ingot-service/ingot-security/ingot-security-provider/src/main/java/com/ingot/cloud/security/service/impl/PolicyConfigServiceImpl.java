package com.ingot.cloud.security.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.security.mapper.CredentialPolicyConfigMapper;
import com.ingot.cloud.security.model.domain.CredentialPolicyConfig;
import com.ingot.cloud.security.service.PolicyConfigService;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.credential.service.ClearCredentialPolicyConfigCacheService;
import com.ingot.framework.security.credential.service.ClearPasswordPolicyCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 策略配置服务实现
 *
 * @author jymot
 * @since 2026-01-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyConfigServiceImpl implements PolicyConfigService {
    private final ClearCredentialPolicyConfigCacheService clearCredentialPolicyConfigCacheService;
    private final ClearPasswordPolicyCacheService clearPasswordPolicyCacheService;
    private final CredentialPolicyConfigMapper policyConfigMapper;
    private final AssertionChecker assertionChecker;

    @Override
    @Cacheable(value = ClearCredentialPolicyConfigCacheService.CACHE_NAME, key = "'list'", unless = "#result.isEmpty()")
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

        clearCache();
        return config;
    }

    @Override
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

        clearCache();
        return config;
    }

    @Override
    public void deletePolicyConfig(Long id) {
        log.info("删除策略配置 - ID: {}", id);

        CredentialPolicyConfig config = policyConfigMapper.selectById(id);
        if (config != null) {
            policyConfigMapper.deleteById(id);
            log.info("策略配置删除成功 - ID: {}", id);
        }

        clearCache();
    }

    @Override
    public void clearCache() {
        log.info("清空所有策略缓存以及密码策略缓存");
        clearCredentialPolicyConfigCacheService.evict();
        clearPasswordPolicyCacheService.evict();
    }
}
