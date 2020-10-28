package com.ingot.cloud.pms.api.rpc.client;

import com.ingot.cloud.pms.api.rpc.PmsDeptApi;
import com.ingot.framework.core.constants.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>Description  : UcGroupFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/11.</p>
 * <p>Time         : 3:43 PM.</p>
 */
@FeignClient(contextId = "pmsGroupFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsDeptFeignApi extends PmsDeptApi {
}
