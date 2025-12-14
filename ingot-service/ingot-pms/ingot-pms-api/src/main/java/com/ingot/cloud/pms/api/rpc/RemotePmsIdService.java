package com.ingot.cloud.pms.api.rpc;

import java.util.List;

import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>Description  : RemotePmsIdService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/14.</p>
 * <p>Time         : 15:26.</p>
 */
@FeignClient(contextId = "RemotePmsIdService", value = ServiceNameConstants.PMS_SERVICE)
public interface RemotePmsIdService {
    /**
     * 生成APP ID
     *
     * @return APP ID
     */
    @GetMapping("/inner/id/appId")
    R<String> genAppId();
}
