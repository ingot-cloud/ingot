package com.ingot.cloud.security.service;

import java.util.List;

import com.ingot.cloud.security.model.domain.CredentialPolicyConfig;

/**
 * 策略配置服务（管理面）。
 * <p>
 * 读路径不再承担缓存职责（统一由
 * {@link com.ingot.framework.security.credential.service.CredentialPolicyConfigService}
 * 装饰器链负责）；写路径在事务提交后通过
 * {@link com.ingot.cloud.security.service.credential.CredentialPolicyChangedSpringEvent}
 * 触发本节点 L1+L2 清理与跨节点失效广播。
 *
 * @author jymot
 * @since 2026-01-22
 */
public interface PolicyConfigService {

    /**
     * 获取所有策略配置（管理面用，直查 DB 不走缓存）。
     */
    List<CredentialPolicyConfig> getAllPolicyConfigs();

    /**
     * 保存策略配置。
     */
    CredentialPolicyConfig savePolicyConfig(CredentialPolicyConfig config);

    /**
     * 更新策略配置。
     */
    CredentialPolicyConfig updatePolicyConfig(CredentialPolicyConfig config);

    /**
     * 删除策略配置。
     */
    void deletePolicyConfig(Long id);
}
