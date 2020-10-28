package com.ingot.cloud.pms.api.rpc.client;

import com.ingot.cloud.pms.api.rpc.PmsMenuApi;
import com.ingot.framework.core.constants.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>Description  : UcMenuFeignAPi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/11.</p>
 * <p>Time         : 3:59 PM.</p>
 */
@FeignClient(contextId = "pmsMenuFeignAPi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsMenuFeignAPi extends PmsMenuApi {
}
