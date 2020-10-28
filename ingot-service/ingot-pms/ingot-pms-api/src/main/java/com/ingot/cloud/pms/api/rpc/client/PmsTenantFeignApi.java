package com.ingot.cloud.pms.api.rpc.client;

import com.ingot.cloud.pms.api.rpc.PmsTenantApi;
import com.ingot.framework.core.constants.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>Description  : PmsTenantFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/8.</p>
 * <p>Time         : 4:32 PM.</p>
 */
@FeignClient(contextId = "pmsTenantFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsTenantFeignApi extends PmsTenantApi {
}
