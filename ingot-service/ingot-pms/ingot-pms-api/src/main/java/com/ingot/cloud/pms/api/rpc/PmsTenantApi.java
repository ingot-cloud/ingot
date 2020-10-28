package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.tenant.TenantCreateDto;
import com.ingot.cloud.pms.api.model.dto.tenant.TenantUpdateDto;
import com.ingot.framework.base.model.dto.IdDto;
import com.ingot.framework.core.wrapper.IngotResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : PmsTenantApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/8.</p>
 * <p>Time         : 4:22 PM.</p>
 */
@Api(value = "PmsTenantApi")
public interface PmsTenantApi {

    @ApiOperation("获取所有租户")
    @GetMapping(value = "/tenant/getAll")
    @ResponseBody
    IngotResponse getAll();

    @ApiOperation("获取租户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "name", dataType = "String", value = "租户名称"),
            @ApiImplicitParam(paramType = "query", name = "code", dataType = "String", value = "租户编码"),
            @ApiImplicitParam(paramType = "query", name = "status", dataType = "String", value = "状态")
    })
    @GetMapping(value = "/tenant/get")
    @ResponseBody
    IngotResponse get(@RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                      @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                      @RequestParam(value = "name", required = false) String name,
                      @RequestParam(value = "code", required = false) String code,
                      @RequestParam(value = "status", required = false) String status);

    @ApiOperation("创建租户")
    @PostMapping(value = "/tenant/crt")
    @ResponseBody
    IngotResponse create(@RequestBody TenantCreateDto params);

    @ApiOperation("删除租户")
    @DeleteMapping(value = "/tenant/del")
    @ResponseBody
    IngotResponse delete(@RequestBody IdDto params);

    @ApiOperation("更新租户")
    @PutMapping(value = "/tenant/upd")
    @ResponseBody
    IngotResponse update(@RequestBody TenantUpdateDto params);
}
