package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.user.*;
import com.ingot.cloud.pms.api.model.vo.user.UserAfterLoginInfoVo;
import com.ingot.cloud.pms.api.model.vo.user.UserVo;
import com.ingot.framework.base.model.dto.IdDto;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.core.wrapper.ListData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.ingot.cloud.pms.api.constants.PmsApiConstants.PATH_MODIFY_PASSWORD_BY_MOBILE;

/**
 * <p>Description  : UcAdminApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/26.</p>
 * <p>Time         : 下午5:28.</p>
 */
@Api(value = "PmsUserApi")
public interface PmsUserApi {

    @ApiOperation("用户注册")
    @PostMapping(value = "/user/register")
    @ResponseBody
    IngotResponse register(@RequestBody UserRegisterParamsDto params);

    @ApiOperation("后管用户注册用户")
    @PostMapping(value = "/user/registerByUser")
    @ResponseBody
    IngotResponse registerByUser(@RequestBody @Validated UserRegisterParamsDto params);

    @ApiOperation("删除用户")
    @DeleteMapping(value = "/user/del")
    @ResponseBody
    IngotResponse deleteUser(@RequestBody @Validated IdDto params);

    @ApiOperation("根据用户名查询用户信息")
    @GetMapping(value = "/user/byUsername")
    @ResponseBody
    IngotResponse<UserVo> findUserByName(@RequestParam(value = "username") String username,
                                         @RequestParam(value = "tenantCode") String tenantCode);

    @ApiOperation("通过系统编码获取用户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "client_id", dataType = "String", value = "系统编码")
    })
    @GetMapping(value = "/user/info")
    @ResponseBody
    IngotResponse<UserAfterLoginInfoVo> getUserInfo(@RequestParam("client_id") String clientId);

    @ApiOperation("获取用户详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "userId", dataType = "String", value = "用户Id", required = true)
    })
    @GetMapping(value = "/user/detail/{userId}")
    @ResponseBody
    IngotResponse getUserDetailInfo(@PathVariable("userId") String userId);

    @ApiOperation("获取所有用户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "username", dataType = "String", value = "用户名"),
            @ApiImplicitParam(paramType = "query", name = "real_name", dataType = "String", value = "真实姓名"),
            @ApiImplicitParam(paramType = "query", name = "mobile", dataType = "String", value = "手机号"),
            @ApiImplicitParam(paramType = "query", name = "status", dataType = "String", value = "状态")
    })
    @GetMapping(value = "/user/get")
    @ResponseBody
    IngotResponse<ListData> getUserList(@RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                        @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                        @RequestParam(value = "username", required = false) String username,
                                        @RequestParam(value = "real_name", required = false) String realName,
                                        @RequestParam(value = "mobile", required = false) String mobile,
                                        @RequestParam(value = "status", required = false) String status);

    @ApiOperation("获取所有用户详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "username", dataType = "String", value = "用户名"),
            @ApiImplicitParam(paramType = "query", name = "real_name", dataType = "String", value = "真实姓名"),
            @ApiImplicitParam(paramType = "query", name = "mobile", dataType = "String", value = "手机号"),
            @ApiImplicitParam(paramType = "query", name = "status", dataType = "String", value = "状态"),
            @ApiImplicitParam(paramType = "query", name = "tenant_id", dataType = "String", value = "租户Id"),
            @ApiImplicitParam(paramType = "query", name = "dept_id", dataType = "String", value = "部门Id"),
            @ApiImplicitParam(paramType = "query", name = "client_id", dataType = "String", value = "客户端Id"),
            @ApiImplicitParam(paramType = "query", name = "role_id", dataType = "String", value = "角色Id"),
    })
    @GetMapping(value = "/user/detail/get")
    @ResponseBody
    IngotResponse<ListData> getUserDetailList(@RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                              @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                              @RequestParam(value = "username", required = false) String username,
                                              @RequestParam(value = "real_name", required = false) String realName,
                                              @RequestParam(value = "mobile", required = false) String mobile,
                                              @RequestParam(value = "status", required = false) String status,
                                              @RequestParam(value = "tenant_id", required = false) String tenantId,
                                              @RequestParam(value = "dept_id", required = false) String deptId,
                                              @RequestParam(value = "client_id", required = false) String clientId,
                                              @RequestParam(value = "role_id", required = false) String roleId);

    @ApiOperation("更新用户信息")
    @PutMapping(value = "/user/upd")
    @ResponseBody
    IngotResponse updateUser(@RequestBody @Validated UserUpdateDto params);

    @ApiOperation("修改用户状态")
    @PutMapping(value = "/user/modifyStatus")
    @ResponseBody
    IngotResponse modifyStatus(@RequestBody @Validated ModifyStatusDto params);

    @ApiOperation("修改用户密码")
    @PutMapping(value = "/user/modifyPassword")
    @ResponseBody
    IngotResponse modifyPassword(@RequestBody @Validated ModifyPasswordDto params);

    @ApiOperation("重置用户密码")
    @PutMapping(value = "/user/resetPassword")
    @ResponseBody
    IngotResponse resetPassword(@RequestBody @Validated ResetPasswordDto params);

    @ApiOperation("根据手机号找回密码")
    @PostMapping(value = PATH_MODIFY_PASSWORD_BY_MOBILE)
    @ResponseBody
    IngotResponse modifyPasswordByMobile(@RequestBody @Validated ModifyPasswordByMobileDto params);

    @ApiOperation("用户绑定部门")
    @PutMapping(value = "/user/bindDept")
    @ResponseBody
    IngotResponse bindDept(@RequestBody @Validated UserBindDeptDto params);
}
