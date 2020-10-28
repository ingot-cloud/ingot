package com.ingot.cloud.pms.api.rpc.client;

import com.ingot.cloud.pms.api.rpc.PmsSocialApi;
import com.ingot.framework.core.constants.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>Description  : PmsSocialFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-27.</p>
 * <p>Time         : 09:42.</p>
 */
@FeignClient(contextId = "pmsSocialFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsSocialFeignApi extends PmsSocialApi {
}
