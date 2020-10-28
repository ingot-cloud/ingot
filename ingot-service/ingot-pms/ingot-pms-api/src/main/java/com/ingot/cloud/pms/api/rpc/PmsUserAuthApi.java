package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.user.HandleLogoutDto;
import com.ingot.cloud.pms.api.model.dto.user.HandleUserLoginDataDto;
import com.ingot.cloud.pms.api.model.dto.user.PmsRefreshTokenDto;
import com.ingot.cloud.pms.api.model.dto.user.UserAuthDetailDto;
import com.ingot.framework.base.model.dto.user.UserLoginResultDto;
import com.ingot.framework.core.constants.TenantConstants;
import com.ingot.framework.core.wrapper.IngotResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import static com.ingot.cloud.pms.api.constants.PmsApiConstants.*;

/**
 * <p>Description  : UserCenterApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/7/10.</p>
 * <p>Time         : 上午9:25.</p>
 */
@Api(value = "PmsUserAuthApi")
public interface PmsUserAuthApi {

    @ApiOperation("获取用户授权信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "username", dataType = "String", value = "用户名"),
            @ApiImplicitParam(paramType = "query", name = "client_id", dataType = "String", value = "登录客户端ID")
    })
    @PostMapping(value = PATH_AUTH_GET_USER_INFO)
    @ResponseBody
    IngotResponse<UserAuthDetailDto> getUserAuthDetail(@RequestParam("username") String username,
                                                       @RequestParam("client_id") String clientId,
                                                       @RequestHeader(TenantConstants.TENANT_HEADER_KEY) String tenantCode);

    @ApiOperation("登录流程，处理用户Unique模式登录数据")
    @PostMapping(value = PATH_AUTH_HANDLE_ADMIN_UNIQUE_LOGIN_DATA)
    @ResponseBody
    IngotResponse<UserLoginResultDto> handleUserUniqueLoginData(@RequestBody HandleUserLoginDataDto params);

    @ApiOperation("登录流程，处理用户Standard模式登录数据")
    @PostMapping(value = PATH_AUTH_HANDLE_ADMIN_STANDARD_LOGIN_DATA)
    @ResponseBody
    IngotResponse<UserLoginResultDto> handleUserStandardLoginData(@RequestBody HandleUserLoginDataDto params);

    @ApiOperation("退出流程，处理用户退出登录")
    @PostMapping(value = PATH_AUTH_HANDLE_LOGOUT)
    @ResponseBody
    IngotResponse handleLogout(@RequestBody HandleLogoutDto params);

    @ApiOperation("用户刷新token")
    @PostMapping(value = PATH_AUTH_REFRESH_TOKEN)
    @ResponseBody
    IngotResponse refreshToken(@RequestBody PmsRefreshTokenDto params);

}
