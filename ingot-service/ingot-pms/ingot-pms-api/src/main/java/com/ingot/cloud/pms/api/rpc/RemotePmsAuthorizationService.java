package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationSnapshotRequest;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>PMS 有效授权快照内部接口，供资源服务在请求期内补全业务权限与数据范围。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@FeignClient(contextId = "pmsAuthorizationService", value = ServiceNameConstants.PMS_SERVICE)
public interface RemotePmsAuthorizationService {

    /**
     * 读取当前认证用户在当前租户下的授权快照。
     *
     * @param request 可选身份；与认证上下文不一致时服务端返回 403
     * @return 授权快照
     */
    @PostMapping("/inner/authorization/snapshot")
    R<AuthorizationSnapshotDTO> snapshot(@RequestBody(required = false) AuthorizationSnapshotRequest request);
}
