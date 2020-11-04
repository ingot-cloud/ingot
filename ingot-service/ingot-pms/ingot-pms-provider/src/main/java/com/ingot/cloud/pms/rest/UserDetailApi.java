package com.ingot.cloud.pms.rest;

import com.ingot.cloud.pms.api.model.dto.user.UserAuthDetailDto;
import com.ingot.framework.core.constants.TenantConstants;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.security.annotation.Permit;
import org.springframework.web.bind.annotation.*;

import static com.ingot.framework.security.model.enums.PermitModel.INNER;

/**
 * <p>Description  : UserApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/4.</p>
 * <p>Time         : 4:41 下午.</p>
 */
@RestController
@RequestMapping(value = "/user")
public class UserDetailApi extends BaseController {

    @Permit(model = INNER)
    @PostMapping(value = "/detail")
    IngotResponse<UserAuthDetailDto> getUserAuthDetail(@RequestParam("username") String username,
                                                       @RequestParam("client_id") String clientId,
                                                       @RequestHeader(TenantConstants.TENANT_HEADER_KEY) String tenantCode){
        // todo
        return ok();
    }
}
