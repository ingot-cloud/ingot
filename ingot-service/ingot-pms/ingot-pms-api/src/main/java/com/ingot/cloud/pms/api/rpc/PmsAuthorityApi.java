package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.authority.AuthorityCreateDto;
import com.ingot.cloud.pms.api.model.dto.authority.AuthorityDeleteDto;
import com.ingot.cloud.pms.api.model.dto.authority.AuthorityUpdateDto;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.core.wrapper.ListData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : UcAuthorityApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/11.</p>
 * <p>Time         : 3:17 PM.</p>
 */
@Api(value = "PmsAuthorityApi")
public interface PmsAuthorityApi {

    @ApiOperation("获取所有权限")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量")
    })
    @GetMapping(value = "/authority/get")
    @ResponseBody
    IngotResponse<ListData> getAllAuthority(@RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                            @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize);

    @ApiOperation("获取所有权限组")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "authority_name", dataType = "String", value = "权限名称"),
            @ApiImplicitParam(paramType = "query", name = "authority_code", dataType = "String", value = "权限编码"),
            @ApiImplicitParam(paramType = "query", name = "status", dataType = "String", value = "状态")
    })
    @GetMapping(value = "/authority/group/get")
    @ResponseBody
    IngotResponse<ListData> getAllAuthorityGroup(@RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                                 @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                                 @RequestParam(value = "authority_name", required = false) String authorityName,
                                                 @RequestParam(value = "authority_code", required = false) String authorityCode,
                                                 @RequestParam(value = "status", required = false) String status);

    @ApiOperation("获取指定权限组中所有权限")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "pid", dataType = "String", value = "权限组id"),
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "authority_name", dataType = "String", value = "权限名称"),
            @ApiImplicitParam(paramType = "query", name = "authority_code", dataType = "String", value = "权限编码"),
            @ApiImplicitParam(paramType = "query", name = "status", dataType = "String", value = "状态")
    })
    @GetMapping(value = "/authority/group/{pid}/get")
    @ResponseBody
    IngotResponse<ListData> getAllAuthorityGroupChild(@PathVariable("pid") String pid,
                                                      @RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                                      @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                                      @RequestParam(value = "authority_name", required = false) String authorityName,
                                                      @RequestParam(value = "authority_code", required = false) String authorityCode,
                                                      @RequestParam(value = "status", required = false) String status);

    @ApiOperation("创建权限")
    @PostMapping(value = "/authority/crt")
    @ResponseBody
    IngotResponse create(@RequestBody @Validated AuthorityCreateDto params);

    @ApiOperation("更新权限")
    @PutMapping(value = "/authority/upd")
    @ResponseBody
    IngotResponse update(@RequestBody @Validated AuthorityUpdateDto params);

    @ApiOperation("删除权限")
    @DeleteMapping(value = "/authority/del")
    @ResponseBody
    IngotResponse delete(@RequestBody @Validated AuthorityDeleteDto params);
}
