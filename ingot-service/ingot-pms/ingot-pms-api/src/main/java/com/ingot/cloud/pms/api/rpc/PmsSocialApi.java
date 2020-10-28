package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.user.UserAuthDetailDto;
import com.ingot.framework.core.constants.TenantConstants;
import com.ingot.framework.core.wrapper.IngotResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>Description  : PmsSocialApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-27.</p>
 * <p>Time         : 09:41.</p>
 */
@Api(value = "PmsSocialApi")
public interface PmsSocialApi {

    @ApiOperation("获取用户授权信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "type", dataType = "String", value = "社交类型"),
            @ApiImplicitParam(paramType = "query", name = "code", dataType = "String", value = "授权码"),
            @ApiImplicitParam(paramType = "query", name = "client_id", dataType = "String", value = "登录客户端ID")
    })
    @PostMapping(value = "/social/getUserAuthDetail")
    @ResponseBody
    IngotResponse<UserAuthDetailDto> getUserAuthDetail(@RequestParam("type") String type,
                                                       @RequestParam("code") String code,
                                                       @RequestParam("client_id") String clientId,
                                                       @RequestHeader(TenantConstants.TENANT_HEADER_KEY) String tenantCode);
}
