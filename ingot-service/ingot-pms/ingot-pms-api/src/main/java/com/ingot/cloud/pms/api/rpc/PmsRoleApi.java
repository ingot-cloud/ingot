package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.role.*;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.core.wrapper.ListData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : UcRoleApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/10.</p>
 * <p>Time         : 下午1:32.</p>
 */
@Api("PmsRoleApi")
public interface PmsRoleApi {

    @ApiOperation("获取所有可用角色，不分页")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "type", dataType = "String", value = "角色类型，多个类型用逗号隔开")
    })
    @GetMapping(value = "/role/getAll")
    @ResponseBody
    IngotResponse getAllRole(@RequestParam(value = "type", required = false) String type);

    @ApiOperation("获取角色权限列表")
    @PostMapping(value = "/role/getAuthority")
    @ResponseBody
    IngotResponse<RoleAuthorityDto> getRoleAuthority(@RequestBody RoleListDto params);

    @ApiOperation("获取所有角色信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "type", dataType = "String", value = "角色类型"),
            @ApiImplicitParam(paramType = "query", name = "role_name", dataType = "String", value = "角色名称")
    })
    @GetMapping(value = "/role/get")
    @ResponseBody
    IngotResponse<ListData> getRoleList(@RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                        @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                        @RequestParam(value = "type", required = false) String type,
                                        @RequestParam(value = "role_name", required = false) String roleName);

    @ApiOperation("创建角色")
    @PostMapping(value = "/role/crt")
    @ResponseBody
    IngotResponse createRole(@RequestBody @Validated RoleCreateDto params);

    @ApiOperation("删除角色")
    @DeleteMapping(value = "/role/del")
    @ResponseBody
    IngotResponse deleteRole(@RequestBody @Validated RoleDeleteDto params);

    @ApiOperation("更新角色")
    @PutMapping(value = "/role/upd")
    @ResponseBody
    IngotResponse updateRole(@RequestBody @Validated RoleUpdateDto params);

    @ApiOperation("角色绑定用户")
    @PostMapping(value = "/role/bind/roleBindUser")
    @ResponseBody
    IngotResponse roleBindUser(@RequestBody @Validated RoleBindUserDto params);

    @ApiOperation("用户绑定角色")
    @PostMapping(value = "/role/bind/userBindRole")
    @ResponseBody
    IngotResponse userBindRole(@RequestBody @Validated RoleBindRoleDto params);

    @ApiOperation("获取指定角色的用户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "roleId", dataType = "String", value = "角色id"),
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "username", dataType = "String", value = "用户名"),
            @ApiImplicitParam(paramType = "query", name = "real_name", dataType = "String", value = "真实姓名")
    })
    @GetMapping(value = "/role/user/{roleId}")
    @ResponseBody
    IngotResponse<ListData> getRoleUserList(@PathVariable("roleId") String roleId,
                                            @RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                            @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                            @RequestParam(value = "username", required = false) String username,
                                            @RequestParam(value = "real_name", required = false) String realName);

    @ApiOperation("获取用户并且携带角色信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "username", dataType = "String", value = "用户名"),
            @ApiImplicitParam(paramType = "query", name = "real_name", dataType = "String", value = "真实姓名")
    })
    @GetMapping(value = "/role/user")
    @ResponseBody
    IngotResponse<ListData> getUserWithRoleInfo(@RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                                @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                                @RequestParam(value = "username", required = false) String username,
                                                @RequestParam(value = "real_name", required = false) String realName);

    @ApiOperation("获取指定用户绑定的所有角色")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "userId", dataType = "String", value = "用户id"),
            @ApiImplicitParam(paramType = "query", name = "role_code", dataType = "String", value = "角色编码"),
            @ApiImplicitParam(paramType = "query", name = "role_name", dataType = "String", value = "角色名称")
    })
    @GetMapping(value = "/role/getByUserId/{userId}")
    @ResponseBody
    IngotResponse<ListData> getRoleByUserId(@PathVariable("userId") String userId,
                                            @RequestParam(value = "role_code", required = false) String role_code,
                                            @RequestParam(value = "role_name", required = false) String role_name);

    @ApiOperation("获取所有角色信息，并且携带指定用户是否有该权限字段")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "user_id", dataType = "String", value = "用户id", required = true),
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "role_code", dataType = "String", value = "角色编码"),
            @ApiImplicitParam(paramType = "query", name = "role_name", dataType = "String", value = "角色名称")
    })
    @GetMapping(value = "/role/getWithUserIdIfPresent")
    @ResponseBody
    IngotResponse<ListData> getRoleWithUserIdIfPresent(@RequestParam(value = "user_id") String userId,
                                                       @RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                                       @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                                       @RequestParam(value = "role_code", required = false) String role_code,
                                                       @RequestParam(value = "role_name", required = false) String role_name);

    @ApiOperation("获取指定角色绑定的所有权限")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "roleId", dataType = "String", value = "角色id", required = true),
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "authority_code", dataType = "String", value = "权限编码"),
            @ApiImplicitParam(paramType = "query", name = "authority_name", dataType = "String", value = "权限名称")
    })
    @GetMapping(value = "/role/auth/bind/{roleId}/get")
    @ResponseBody
    IngotResponse<ListData> getBindAuthorityOfRole(@PathVariable("roleId") String roleId,
                                                   @RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                                   @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                                   @RequestParam(value = "authority_code", required = false) String authorityCode,
                                                   @RequestParam(value = "authority_name", required = false) String authorityName);

    @ApiOperation("查询权限组，若绑定了指定角色，那么一同返回角色信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "role_id", dataType = "String", value = "角色id", required = true),
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "authority_code", dataType = "String", value = "权限编码"),
            @ApiImplicitParam(paramType = "query", name = "authority_name", dataType = "String", value = "权限名称")
    })
    @GetMapping(value = "/role/auth/group/get")
    @ResponseBody
    IngotResponse<ListData> getAllAuthorityGroupWithRole(@RequestParam(value = "role_id") String roleId,
                                                         @RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                                         @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                                         @RequestParam(value = "authority_code", required = false) String authorityCode,
                                                         @RequestParam(value = "authority_name", required = false) String authorityName);

    @ApiOperation("查询权限组子列表，若绑定了指定角色，那么一同返回角色信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "pid", dataType = "String", value = "权限组id", required = true),
            @ApiImplicitParam(paramType = "query", name = "role_id", dataType = "String", value = "角色id", required = true),
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "authority_code", dataType = "String", value = "权限编码"),
            @ApiImplicitParam(paramType = "query", name = "authority_name", dataType = "String", value = "权限名称")
    })
    @GetMapping(value = "/role/auth/group/{pid}/get")
    @ResponseBody
    IngotResponse<ListData> getAllAuthorityGroupChildWithRole(@PathVariable("pid") String pid,
                                                              @RequestParam(value = "role_id") String roleId,
                                                              @RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                                              @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                                              @RequestParam(value = "authority_code", required = false) String authorityCode,
                                                              @RequestParam(value = "authority_name", required = false) String authorityName);

    @ApiOperation("角色绑定权限")
    @PostMapping(value = "/role/bind/auth")
    @ResponseBody
    IngotResponse roleBindAuthority(@RequestBody @Validated RoleBindAuthorityDto params);

    @ApiOperation("角色绑定菜单")
    @PostMapping(value = "/role/bind/menu")
    @ResponseBody
    IngotResponse roleBindMenu(@RequestBody @Validated RoleBindMenuDto params);

    @ApiOperation("获取所有系统列表，附带角色信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "client_id", dataType = "String", value = "ClientId"),
            @ApiImplicitParam(paramType = "query", name = "description", dataType = "String", value = "描述")
    })
    @GetMapping(value = "/role/client")
    @ResponseBody
    IngotResponse<ListData> getClientWithRoleInfo(@RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                                  @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                                  @RequestParam(value = "client_id", required = false) String clientId,
                                                  @RequestParam(value = "description", required = false) String description);


    @ApiOperation("获取已绑定指定角色的系统列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "roleId", dataType = "String", value = "角色id", required = true),
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "client_id", dataType = "String", value = "ClientId"),
            @ApiImplicitParam(paramType = "query", name = "description", dataType = "String", value = "描述")
    })
    @GetMapping(value = "/role/client/{roleId}")
    @ResponseBody
    IngotResponse<ListData> getClientByRole(@PathVariable("roleId") String roleId,
                                            @RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                            @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                            @RequestParam(value = "client_id", required = false) String clientId,
                                            @RequestParam(value = "description", required = false) String description);

    @ApiOperation("角色绑定Client")
    @PostMapping(value = "/role/bind/roleBindClient")
    @ResponseBody
    IngotResponse roleBindClient(@RequestBody @Validated RoleBindClientDto params);

}
