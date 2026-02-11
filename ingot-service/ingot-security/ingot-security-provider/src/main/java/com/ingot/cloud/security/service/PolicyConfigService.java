package com.ingot.cloud.security.service;

import java.util.List;

import com.ingot.cloud.security.model.domain.CredentialPolicyConfig;

/**
 * 策略配置服务
 *
 * @author jymot
 * @since 2026-01-22
 */
public interface PolicyConfigService {

    /**
     * 获取所有策略配置
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
     * 清空所有缓存
     */
    void clearCache();
}
