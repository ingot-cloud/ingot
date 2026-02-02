package com.ingot.cloud.credential.api.rpc;

import java.util.List;

import com.ingot.cloud.credential.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 凭证安全 Feign 接口
 *
 * @author jymot
 * @since 2026-01-22
 */
@FeignClient(contextId = "RemoteCredentialService", value = ServiceNameConstants.CREDENTIAL_SERVICE)
public interface RemoteCredentialService {

    /**
     * 获取密码策略配置列表
     *
     * @param tenantId 租户ID
     * @return 密码策略配置列表
     */
    @GetMapping("/inner/credential/policy-configs")
    R<List<CredentialPolicyConfigVO>> getPolicyConfigs();
}
