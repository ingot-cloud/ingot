package com.ingot.cloud.pms.api.rpc.client;

import com.ingot.cloud.pms.api.rpc.PmsUserApi;
import com.ingot.framework.core.constants.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>Description  : UcAdminFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/26.</p>
 * <p>Time         : 下午5:30.</p>
 */
@FeignClient(contextId = "pmsUserFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsUserFeignApi extends PmsUserApi {
}
