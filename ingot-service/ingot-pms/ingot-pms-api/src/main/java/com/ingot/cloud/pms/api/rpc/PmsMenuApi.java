package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.menu.MenuCreateDto;
import com.ingot.cloud.pms.api.model.dto.menu.MenuDeleteDto;
import com.ingot.cloud.pms.api.model.dto.menu.MenuUpdateDto;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.core.wrapper.ListData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : UcMenuApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/11.</p>
 * <p>Time         : 3:55 PM.</p>
 */
@Api("PmsMenuApi")
public interface PmsMenuApi {

    @ApiOperation("获取菜单树")
    @GetMapping(value = "/menu/tree/get")
    @ResponseBody
    IngotResponse<ListData> selectAllMenuTree();

    @ApiOperation("获取菜单树如果存在携带指定系统信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "system_id", dataType = "String", value = "系统id", required = true)
    })
    @GetMapping(value = "/menu/tree/withClientIfPresent")
    @ResponseBody
    IngotResponse<ListData> selectMenuTreeWithClientInfoIfPresent(@RequestParam("client_id") String clientId);

    @ApiOperation("获取指定系统的菜单树")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "systemId", dataType = "String", value = "系统id", required = true)
    })
    @GetMapping(value = "/menu/client/{clientId}/tree/get")
    @ResponseBody
    IngotResponse<ListData> selectClientRootMenuTree(@PathVariable("clientId") String clientId);

    @ApiOperation("获取指定系统所有根菜单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "systemId", dataType = "String", value = "系统id", required = true)
    })
    @GetMapping(value = "/menu/client/{clientId}/root/get")
    @ResponseBody
    IngotResponse<ListData> selectClientRootMenu(@PathVariable("clientId") String clientId);

    @ApiOperation("查询菜单树，并且携带指定角色信息（如果存在）")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "role_id", dataType = "String", value = "角色id", required = true)
    })
    @GetMapping(value = "/menu/tree/withRoleIfPresent")
    @ResponseBody
    IngotResponse<ListData> selectMenuTreeWithRoleInfoIfPresent(@RequestParam("role_id") String roleId);

    @ApiOperation("获取指定角色菜单树")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "roleId", dataType = "String", value = "角色id", required = true)
    })
    @GetMapping(value = "/menu/role/{roleId}/tree/get")
    @ResponseBody
    IngotResponse<ListData> selectRoleMenuTree(@PathVariable("roleId") String roleId);

    @ApiOperation("获取指定角色根菜单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "roleId", dataType = "String", value = "角色id", required = true)
    })
    @GetMapping(value = "/menu/role/{roleId}/root/get")
    @ResponseBody
    IngotResponse<ListData> selectRoleMenu(@PathVariable("roleId") String roleId);

    @ApiOperation("创建菜单")
    @PostMapping(value = "/menu/crt")
    @ResponseBody
    IngotResponse create(@RequestBody @Validated MenuCreateDto params);

    @ApiOperation("更新菜单")
    @PutMapping(value = "/menu/upd")
    @ResponseBody
    IngotResponse update(@RequestBody @Validated MenuUpdateDto params);

    @ApiOperation("删除菜单")
    @DeleteMapping(value = "/menu/del")
    @ResponseBody
    IngotResponse delete(@RequestBody @Validated MenuDeleteDto params);
}
