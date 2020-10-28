package com.ingot.cloud.pms.api.rpc.client;

import com.ingot.cloud.pms.api.rpc.PmsAuthorityApi;
import com.ingot.framework.core.constants.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>Description  : UcAuthorityFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/11.</p>
 * <p>Time         : 3:40 PM.</p>
 */
@FeignClient(contextId = "pmsAuthorityFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsAuthorityFeignApi extends PmsAuthorityApi {
}
