package com.ingot.cloud.pms.api.rpc;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>Description  : PmsSocialDetailsService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 16:01.</p>
 */
@FeignClient(contextId = "pmsSocialDetailsService", value = ServiceNameConstants.PMS_SERVICE)
public interface RemotePmsSocialDetailsService {

    @GetMapping("/inner/social/detailList/{type}")
    R<List<SysSocialDetails>> getSocialDetailsByType(@PathVariable String type);

    @GetMapping("/inner/social/appId/{appId}")
    R<SysSocialDetails> getDetailsByAppId(@PathVariable String appId);
}
