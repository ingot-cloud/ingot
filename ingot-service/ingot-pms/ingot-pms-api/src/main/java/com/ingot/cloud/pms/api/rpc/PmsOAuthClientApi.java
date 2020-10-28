package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.client.*;
import com.ingot.cloud.pms.api.model.vo.client.OAuthClientVo;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.core.wrapper.ListData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.ingot.cloud.pms.api.constants.PmsApiConstants.*;

/**
 * <p>Description  : SysOAuthClientApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/2.</p>
 * <p>Time         : 11:51 AM.</p>
 */
@Api(value = "PmsOAuthClientApi")
public interface PmsOAuthClientApi {

    @ApiOperation("获取所有简单client信息")
    @GetMapping(value = "/client/getSimple")
    @ResponseBody
    IngotResponse<ListData> selectAllSimpleClient();

    @ApiOperation("获取所有client信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "client_id", dataType = "String", value = "clientId"),
            @ApiImplicitParam(paramType = "query", name = "client_name", dataType = "String", value = "client名称"),
            @ApiImplicitParam(paramType = "query", name = "type", dataType = "String", value = "类型")
    })
    @GetMapping(value = PATH_CLIENT_GET)
    @ResponseBody
    IngotResponse<ListData> selectAllApplication(@RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                                 @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                                 @RequestParam(value = "client_id", required = false) String clientId,
                                                 @RequestParam(value = "client_name", required = false) String clientName,
                                                 @RequestParam(value = "type", required = false) String type);

    @ApiOperation("查询指定应用允许访问的应用列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "appId", dataType = "String", value = "clientId", required = true),
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "client_id", dataType = "String", value = "clientId"),
            @ApiImplicitParam(paramType = "query", name = "client_name", dataType = "String", value = "client名称")
    })
    @GetMapping(value = PATH_CLIENT_CLIENT_ID_GRANT)
    @ResponseBody
    IngotResponse<ListData> selectGrantApplication(@PathVariable("appId") String appId,
                                                   @RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                                   @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                                   @RequestParam(value = "client_id", required = false) String clientId,
                                                   @RequestParam(value = "client_name", required = false) String clientName);

    @ApiOperation("创建Client")
    @PostMapping(value = PATH_CLIENT_CREATE)
    @ResponseBody
    IngotResponse<OAuthClientVo> create(@RequestBody @Validated ClientCreateParams params);

    @ApiOperation("更新Client")
    @PutMapping(value = PATH_CLIENT_UPDATE)
    @ResponseBody
    IngotResponse update(@RequestBody @Validated ClientUpdateParams params);

    @ApiOperation("删除Client")
    @DeleteMapping(value = PATH_CLIENT_DELETE)
    @ResponseBody
    IngotResponse delete(@RequestBody @Validated ClientDeleteParams params);

    @ApiOperation("Client绑定授权")
    @PutMapping(value = PATH_CLIENT_GRANT_ADD)
    @ResponseBody
    IngotResponse addGrant(@RequestBody @Validated ClientGrantParams params);

    @ApiOperation("Client删除授权")
    @DeleteMapping(value = PATH_CLIENT_GRANT_DELETE)
    @ResponseBody
    IngotResponse deleteGrant(@RequestBody @Validated ClientDeleteGrantParams params);

    @ApiOperation("根据类型获取所有client")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "page_num", dataType = "Integer", value = "页数"),
            @ApiImplicitParam(paramType = "query", name = "page_size", dataType = "Integer", value = "每一页显示数量"),
            @ApiImplicitParam(paramType = "query", name = "type", dataType = "String", value = "类型，多个类型用逗号隔开", required = true)
    })
    @GetMapping(value = PATH_CLIENT_GET_WITH_TYPE)
    @ResponseBody
    IngotResponse<ListData<OAuthClientVo>> selectApplicationWithType(@RequestParam(value = "page_num", required = false, defaultValue = "1") int pageNum,
                                                                     @RequestParam(value = "page_size", required = false, defaultValue = "10") int pageSize,
                                                                     @RequestParam(value = "type") String types);

    @ApiOperation("Client 绑定菜单")
    @PostMapping(value = PATH_CLIENT_BIND_MENU)
    @ResponseBody
    IngotResponse bindMenu(@RequestBody ClientBindMenuDto params);
}
