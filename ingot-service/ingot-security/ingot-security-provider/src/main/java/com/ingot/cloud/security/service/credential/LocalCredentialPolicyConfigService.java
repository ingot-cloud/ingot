package com.ingot.cloud.security.service.credential;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.cloud.security.mapper.CredentialPolicyConfigMapper;
import com.ingot.cloud.security.model.domain.CredentialPolicyConfig;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ingot-security-provider 进程内的本地 delegate 实现，直接通过 Mapper 访问 MySQL。
 * <p>
 * 通过 {@code CredentialSecurityAutoConfiguration#CREDENTIAL_POLICY_CONFIG_DELEGATE} 同名 bean
 * 覆盖框架默认的 Remote 实现，避免 ingot-security 自身又走 Feign 形成循环。
 * </p>
 *
 * @author jy
 * @since 2026/5/16
 */
@Slf4j
@RequiredArgsConstructor
public class LocalCredentialPolicyConfigService implements CredentialPolicyConfigService {

    private final CredentialPolicyConfigMapper policyConfigMapper;

    @Override
    public List<CredentialPolicyConfigVO> getAll() {
        List<CredentialPolicyConfig> entities = policyConfigMapper.selectList(
                Wrappers.<CredentialPolicyConfig>lambdaQuery()
                        .orderByAsc(CredentialPolicyConfig::getPriority)
        );
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(LocalCredentialPolicyConfigService::toVO).toList();
    }

    private static CredentialPolicyConfigVO toVO(CredentialPolicyConfig entity) {
        CredentialPolicyConfigVO vo = new CredentialPolicyConfigVO();
        vo.setId(entity.getId());
        vo.setPolicyType(entity.getPolicyType() == null ? null : entity.getPolicyType().getValue());
        vo.setPolicyConfig(entity.getPolicyConfig());
        vo.setPriority(entity.getPriority());
        vo.setEnabled(entity.getEnabled());
        return vo;
    }
}
