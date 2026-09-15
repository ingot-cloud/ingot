package com.ingot.cloud.iam.api.rpc;

import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * <p>声明 IAM 应用标识生成 RPC。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@FeignClient(contextId = "RemoteIamIdService", value = ServiceNameConstants.IAM_SERVICE)
public interface RemoteIamIdService {
    /**
     * 生成APP ID
     *
     * @return APP ID
     */
    @GetMapping("/inner/id/appId")
    R<String> genAppId();
}
