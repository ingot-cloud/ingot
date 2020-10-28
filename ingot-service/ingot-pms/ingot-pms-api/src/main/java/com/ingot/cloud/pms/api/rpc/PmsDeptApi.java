package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.dept.DeptCreateDto;
import com.ingot.cloud.pms.api.model.dto.dept.DeptUpdateDto;
import com.ingot.framework.base.model.dto.IdDto;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.core.wrapper.ListData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : PmsDeptApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/11.</p>
 * <p>Time         : 3:35 PM.</p>
 */
@Api("PmsDeptApi")
public interface PmsDeptApi {

    @ApiOperation("获取所有部门树形结构")
    @GetMapping(value = "/dept/tree/get")
    @ResponseBody
    IngotResponse<ListData> selectRootTree();

    @ApiOperation("创建部门")
    @PostMapping(value = "/dept/crt")
    @ResponseBody
    IngotResponse createDept(@RequestBody @Validated DeptCreateDto params);

    @ApiOperation("更新部门")
    @PutMapping(value = "/dept/upd")
    @ResponseBody
    IngotResponse updateDept(@RequestBody @Validated DeptUpdateDto params);

    @ApiOperation("删除部门")
    @DeleteMapping(value = "/dept/del")
    @ResponseBody
    IngotResponse deleteDept(@RequestBody @Validated IdDto params);

}
