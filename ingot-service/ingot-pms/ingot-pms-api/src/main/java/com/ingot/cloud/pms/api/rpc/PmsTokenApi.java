package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.token.OfflineTokenDto;
import com.ingot.cloud.pms.api.model.dto.token.TokenDeleteDto;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.core.wrapper.ListData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : UcTokenApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/12.</p>
 * <p>Time         : 2:24 PM.</p>
 */
@Api(value = "PmsTokenApi")
public interface PmsTokenApi {

    @ApiOperation("获取所有token信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "user_id", dataType = "String", value = "用户名"),
            @ApiImplicitParam(paramType = "query", name = "username", dataType = "String", value = "用户名"),
            @ApiImplicitParam(paramType = "query", name = "real_name", dataType = "String", value = "真实姓名"),
            @ApiImplicitParam(paramType = "query", name = "status", dataType = "String", value = "状态")
    })
    @GetMapping(value = "/token/get")
    @ResponseBody
    IngotResponse<ListData> getAllToken(@RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                        @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                        @RequestParam(value = "user_id", required = false) String userId,
                                        @RequestParam(value = "username", required = false) String systemCode,
                                        @RequestParam(value = "real_name", required = false) String systemName,
                                        @RequestParam(value = "status", required = false, defaultValue = "-1") int status);

    @ApiOperation("离线token")
    @PutMapping(value = "/token/offline")
    @ResponseBody
    IngotResponse offlineToken(@RequestBody @Validated OfflineTokenDto params);

    @ApiOperation("删除token")
    @DeleteMapping(value = "/token/del")
    @ResponseBody
    IngotResponse deleteToken(@RequestBody @Validated TokenDeleteDto params);
}
