package com.ingot.cloud.iam.api.rpc;

import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotRequest;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>声明 IAM 内部授权快照 RPC，服务身份与目标上下文分别校验。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@FeignClient(contextId = "iamAuthorizationService", value = ServiceNameConstants.IAM_SERVICE)
public interface RemoteIamAuthorizationService {

    /**
     * 读取当前认证用户在当前租户下的授权快照。
     *
     * @param request 可选身份；与认证上下文不一致时服务端返回 403
     * @return 授权快照
     */
    @PostMapping("/inner/authorization/snapshot")
    R<AuthorizationSnapshotDTO> snapshot(@RequestBody(required = false) AuthorizationSnapshotRequest request);

    /**
     * 求值当前认证中的 IAM 身份，不接受请求替代成员或管理域。
     * @param request 已注册资源和精确操作
     * @return 完整操作、对象范围与字段执行策略
     */
    @PostMapping("/inner/authorization/v2/evaluate")
    R<com.ingot.framework.commons.model.iam.extension.AuthorizationDecision> evaluate(
            @RequestBody com.ingot.framework.commons.model.iam.extension.AuthorizationRequest request);
}
