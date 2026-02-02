package com.ingot.cloud.credential.service;

import java.util.List;

import com.ingot.cloud.credential.model.domain.CredentialPolicyConfig;
import com.ingot.framework.security.credential.model.CredentialPolicyType;

/**
 * 策略配置服务
 *
 * @author jymot
 * @since 2026-01-22
 */
public interface PolicyConfigService {

    /**
     * 获取租户的策略配置（如果没有租户级配置，返回全局默认配置）
     *
     * @param policyType 策略类型
     * @return 策略配置
     */
    CredentialPolicyConfig getPolicyConfig(String policyType);

    /**
     * 获取租户的所有策略配置
     *
     * @return 策略配置列表
     */
    List<CredentialPolicyConfig> getAllPolicyConfigs();

    /**
     * 保存或更新策略配置
     *
     * @param config 策略配置
     * @return 保存的配置
     */
    CredentialPolicyConfig savePolicyConfig(CredentialPolicyConfig config);

    /**
     * 更新策略配置
     *
     * @param config 策略配置
     * @return 更新的配置
     */
    CredentialPolicyConfig updatePolicyConfig(CredentialPolicyConfig config);

    /**
     * 删除策略配置
     *
     * @param id 配置ID
     */
    void deletePolicyConfig(Long id);

    /**
     * 刷新策略缓存
     *
     * @param policyType 策略类型
     */
    void refreshCache(CredentialPolicyType policyType);

    /**
     * 清空所有缓存
     */
    void clearAllCache();
}
