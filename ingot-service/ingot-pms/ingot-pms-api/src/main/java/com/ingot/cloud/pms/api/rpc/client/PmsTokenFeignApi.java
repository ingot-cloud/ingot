package com.ingot.cloud.pms.api.rpc.client;

import com.ingot.cloud.pms.api.rpc.PmsTokenApi;
import com.ingot.framework.core.constants.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>Description  : UcTokenFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/12.</p>
 * <p>Time         : 2:29 PM.</p>
 */
@FeignClient(contextId = "pmsTokenFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsTokenFeignApi extends PmsTokenApi {
}
