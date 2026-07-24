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
        R<List<CredentialPolicyConfigVO>> response;
        try {
            response = remoteCredentialService.getPolicyConfigs();
        } catch (Exception e) {
            // 调用异常（连接失败 / 超时 / 反序列化等）：视为远程不可用，交由上层弹性兜底处理，绝不吞成空。
            throw new CredentialRemoteUnavailableException("Remote getPolicyConfigs error", e);
        }
        if (response == null || !response.isSuccess()) {
            // 非成功码或空响应：同样视为远程不可用，触发降级阶梯。
            throw new CredentialRemoteUnavailableException(
                    "Remote getPolicyConfigs failed, response=" + response);
        }
        // 成功（含空集合）：表达「合法无策略」，直接返回真实数据，不触发兜底。
        List<CredentialPolicyConfigVO> data = response.getData();
        return data != null ? data : List.of();
    }

    @Override
    public void evictAll() {
        // 远端缓存由 ingot-security-provider 自身的 publisher 在事务提交后处理，
        // 本节点除了向上的装饰器层依次清完外，无需再次回调远端。
    }
}
