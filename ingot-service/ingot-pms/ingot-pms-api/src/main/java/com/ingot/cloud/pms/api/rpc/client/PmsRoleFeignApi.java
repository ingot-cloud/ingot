package com.ingot.cloud.pms.api.rpc.client;

import com.ingot.cloud.pms.api.rpc.PmsRoleApi;
import com.ingot.framework.core.constants.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>Description  : UcRoleFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/10.</p>
 * <p>Time         : 下午2:09.</p>
 */
@FeignClient(contextId = "pmsRoleFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsRoleFeignApi extends PmsRoleApi {
}
