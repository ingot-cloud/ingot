package com.ingot.framework.security.credential.internal;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.cloud.security.api.rpc.RemoteCredentialService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * L0 远端 delegate：通过 Feign 调用 ingot-security 服务获取策略配置。
 * <p>
 * 在 ingot-security-provider 进程内不会注册（被 {@code LocalCredentialPolicyConfigService}
 * 通过同名 bean 覆盖）。其它消费方在引入 ingot-security-credential 后默认装载本实现。
 * </p>
 *
 * @author jy
 * @since 2026/5/16
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteCredentialPolicyConfigService implements CredentialPolicyConfigService {

    private final RemoteCredentialService remoteCredentialService;

    @Override
    public List<CredentialPolicyConfigVO> getAll() {
        try {
            R<List<CredentialPolicyConfigVO>> response = remoteCredentialService.getPolicyConfigs();
            if (response == null || !response.isSuccess()) {
                log.warn("[Credential] Remote getPolicyConfigs failed, response={}", response);
                return List.of();
            }
            List<CredentialPolicyConfigVO> data = response.getData();
            return data != null ? data : List.of();
        } catch (Exception e) {
            log.warn("[Credential] Remote getPolicyConfigs error", e);
            return List.of();
        }
    }

    @Override
    public void evictAll() {
        // 远端缓存由 ingot-security-provider 自身的 publisher 在事务提交后处理，
        // 本节点除了向上的装饰器层依次清完外，无需再次回调远端。
    }
}
