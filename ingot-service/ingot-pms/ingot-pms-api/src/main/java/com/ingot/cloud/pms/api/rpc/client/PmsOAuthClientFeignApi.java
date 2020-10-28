package com.ingot.cloud.pms.api.rpc.client;

import com.ingot.cloud.pms.api.rpc.PmsOAuthClientApi;
import com.ingot.framework.core.constants.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>Description  : SysOAuthClientFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/2.</p>
 * <p>Time         : 11:54 AM.</p>
 */
@FeignClient(contextId = "pmsOAuthClientFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsOAuthClientFeignApi extends PmsOAuthClientApi {
}
