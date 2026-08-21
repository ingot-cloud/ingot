package com.ingot.cloud.security.api.rpc;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.SessionConcurrencyPolicyVO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * <p>并发会话策略 Feign 接口，Auth 侧分层缓存的最内层数据源。</p>
 *
 * <p>返回启用中的策略全量列表，由调用方按登录上下文自行选取命中记录。调用失败或安全中心不可用时
 * 由 Auth 的降级阶梯接管（LKG → Nacos 地板），本接口不承担兜底语义。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionConcurrencyPolicyVO
 */
@FeignClient(contextId = "RemoteSessionConcurrencyPolicyService", value = ServiceNameConstants.SECURITY_SERVICE)
public interface RemoteSessionConcurrencyPolicyService {

    /**
     * 获取全量并发会话策略。
     */
    @GetMapping("/inner/security/session/concurrency-policies")
    R<List<SessionConcurrencyPolicyVO>> listPolicies();
}
