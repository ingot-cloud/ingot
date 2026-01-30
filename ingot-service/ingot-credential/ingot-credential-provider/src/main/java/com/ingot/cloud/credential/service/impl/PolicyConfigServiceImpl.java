package com.ingot.cloud.credential.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.credential.mapper.CredentialPolicyConfigMapper;
import com.ingot.cloud.credential.model.domain.CredentialPolicyConfig;
import com.ingot.cloud.credential.service.PolicyConfigService;
import com.ingot.framework.security.credential.model.CredentialPolicyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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

    private final CredentialPolicyConfigMapper policyConfigMapper;

    private static final String CACHE_NAME = "credential:policy";

    @Override
    @Cacheable(value = CACHE_NAME, key = "#tenantId + ':' + #policyType", unless = "#result == null")
    public CredentialPolicyConfig getPolicyConfig(Long tenantId, String policyType) {
        log.debug("查询策略配置 - 租户ID: {}, 策略类型: {}", tenantId, policyType);

        // 1. 先查询租户级配置
        if (tenantId != null) {
            CredentialPolicyConfig tenantConfig = policyConfigMapper.selectOne(
                    Wrappers.<CredentialPolicyConfig>lambdaQuery()
                            .eq(CredentialPolicyConfig::getTenantId, tenantId)
                            .eq(CredentialPolicyConfig::getPolicyType, policyType)
                            .eq(CredentialPolicyConfig::getEnabled, true)
            );
            if (tenantConfig != null) {
                log.debug("找到租户级策略配置 - ID: {}", tenantConfig.getId());
                return tenantConfig;
            }
        }

        // 2. 查询全局默认配置
        CredentialPolicyConfig globalConfig = policyConfigMapper.selectOne(
                Wrappers.<CredentialPolicyConfig>lambdaQuery()
                        .isNull(CredentialPolicyConfig::getTenantId)
                        .eq(CredentialPolicyConfig::getPolicyType, policyType)
                        .eq(CredentialPolicyConfig::getEnabled, true)
        );

        if (globalConfig != null) {
            log.debug("使用全局默认策略配置 - ID: {}", globalConfig.getId());
        } else {
            log.warn("未找到策略配置 - 租户ID: {}, 策略类型: {}", tenantId, policyType);
        }

        return globalConfig;
    }

    @Override
    public List<CredentialPolicyConfig> getAllPolicyConfigs(Long tenantId) {
        log.debug("查询所有策略配置 - 租户ID: {}", tenantId);

        if (tenantId == null) {
            // 查询全局配置
            return policyConfigMapper.selectList(
                    Wrappers.<CredentialPolicyConfig>lambdaQuery()
                            .isNull(CredentialPolicyConfig::getTenantId)
                            .eq(CredentialPolicyConfig::getEnabled, true)
                            .orderByAsc(CredentialPolicyConfig::getPriority)
            );
        } else {
            // 查询租户配置（包含租户级和全局）
            return policyConfigMapper.selectList(
                    Wrappers.<CredentialPolicyConfig>lambdaQuery()
                            .and(wrapper -> wrapper
                                    .isNull(CredentialPolicyConfig::getTenantId)
                                    .or()
                                    .eq(CredentialPolicyConfig::getTenantId, tenantId)
                            )
                            .eq(CredentialPolicyConfig::getEnabled, true)
                            .orderByAsc(CredentialPolicyConfig::getPriority)
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CACHE_NAME, key = "#config.tenantId + ':' + #config.policyType.value")
    public CredentialPolicyConfig savePolicyConfig(CredentialPolicyConfig config) {
        log.info("保存策略配置 - 租户ID: {}, 策略类型: {}", 
                config.getTenantId(), config.getPolicyType());

        if (config.getId() == null) {
            // 新增
            policyConfigMapper.insert(config);
            log.info("策略配置新增成功 - ID: {}", config.getId());
        } else {
            // 更新
            policyConfigMapper.updateById(config);
            log.info("策略配置更新成功 - ID: {}", config.getId());
        }

        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePolicyConfig(Long id) {
        log.info("删除策略配置 - ID: {}", id);

        CredentialPolicyConfig config = policyConfigMapper.selectById(id);
        if (config != null) {
            policyConfigMapper.deleteById(id);
            // 清除缓存
            refreshCache(config.getTenantId(), config.getPolicyType());
            log.info("策略配置删除成功 - ID: {}", id);
        }
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #policyType.value")
    public void refreshCache(Long tenantId, CredentialPolicyType policyType) {
        log.info("刷新策略缓存 - 租户ID: {}, 策略类型: {}", tenantId, policyType);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearAllCache() {
        log.info("清空所有策略缓存");
    }
}
