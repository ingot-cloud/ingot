package com.ingot.cloud.pms.api.rpc.client;

import com.ingot.cloud.pms.api.rpc.PmsLogApi;
import com.ingot.framework.core.constants.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>Description  : PmsLogFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-28.</p>
 * <p>Time         : 14:42.</p>
 */
@FeignClient(contextId = "pmsLogFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsLogFeignApi extends PmsLogApi {
}
