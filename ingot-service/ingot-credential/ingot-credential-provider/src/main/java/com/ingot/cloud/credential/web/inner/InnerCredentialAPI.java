package com.ingot.cloud.credential.web.inner;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.ingot.cloud.credential.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.cloud.credential.model.domain.CredentialPolicyConfig;
import com.ingot.cloud.credential.service.PolicyConfigService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 凭证安全内部接口
 *
 * @author jymot
 * @since 2026-01-22
 */
@Slf4j
@RestController
@RequestMapping("/inner/credential")
@RequiredArgsConstructor
public class InnerCredentialAPI implements RShortcuts {
    private final PolicyConfigService policyConfigService;

    /**
     * 获取密码策略列表
     */
    @GetMapping("/policy-configs")
    public R<List<CredentialPolicyConfigVO>> getPolicyConfigs() {
        List<CredentialPolicyConfig> configs = policyConfigService.getAllPolicyConfigs();
        return ok(CollUtil.emptyIfNull(configs)
                .stream()
                .map(config -> {
                    CredentialPolicyConfigVO vo = new CredentialPolicyConfigVO();
                    vo.setId(config.getId());
                    vo.setPolicyType(config.getPolicyType().getValue());
                    vo.setPolicyConfig(config.getPolicyConfig());
                    vo.setPriority(config.getPriority());
                    vo.setEnabled(config.getEnabled());
                    return vo;
                })
                .toList());
    }
}
