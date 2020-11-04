package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.role.RoleAuthorityDto;
import com.ingot.cloud.pms.api.model.dto.role.RoleListDto;
import com.ingot.framework.core.constants.ServiceNameConstants;
import com.ingot.framework.core.wrapper.IngotResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>Description  : UcRoleFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/10.</p>
 * <p>Time         : 下午2:09.</p>
 */
@FeignClient(contextId = "pmsRoleFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsRoleFeignApi {

    @PostMapping(value = "/role/getAuthority")
    IngotResponse<RoleAuthorityDto> getRoleAuthority(@RequestBody RoleListDto params);
}
