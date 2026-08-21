package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.SessionConcurrencyPolicyVO;
import com.ingot.cloud.security.api.rpc.RemoteSessionConcurrencyPolicyService;
import com.ingot.framework.cache.spi.CacheValueLoader;
import com.ingot.framework.commons.model.support.R;
import lombok.RequiredArgsConstructor;

/**
 * <p>分层缓存链最内层的加载器，经 Feign 向安全中心拉取并发策略全量列表。</p>
 *
 * <p>调用异常或非 success 响应都包装成 {@link SessionConcurrencyRemoteUnavailableException} 上抛，
 * 交由降级阶梯处理；响应成功但列表为空属于合法空，原样返回空列表 ——
 * 表示「安全中心当前没有配置策略」，此时执行面按无限并发处理，与远端故障是两件事。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionConcurrencyRemoteUnavailableException
 */
@RequiredArgsConstructor
public class FeignSessionConcurrencyPolicyLoader
        implements CacheValueLoader<String, List<SessionConcurrencyPolicyVO>> {

    private final RemoteSessionConcurrencyPolicyService remoteService;

    @Override
    public List<SessionConcurrencyPolicyVO> load(String key) {
        R<List<SessionConcurrencyPolicyVO>> response;
        try {
            response = remoteService.listPolicies();
        } catch (Exception e) {
            throw new SessionConcurrencyRemoteUnavailableException(
                    "[SessionConcurrency] remote listPolicies error", e);
        }
        if (response == null || !response.isSuccess()) {
            throw new SessionConcurrencyRemoteUnavailableException(
                    "[SessionConcurrency] remote listPolicies failed, response=" + response);
        }
        List<SessionConcurrencyPolicyVO> data = response.getData();
        return data != null ? data : List.of();
    }
}
