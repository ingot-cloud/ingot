package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.log.SysLogDto;
import com.ingot.framework.core.wrapper.IngotResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>Description  : PmsLogApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-28.</p>
 * <p>Time         : 14:28.</p>
 */
@Api("PmsLogApi")
public interface PmsLogApi {

    /**
     * 删除日志
     * @param id ID
     * @return {@link IngotResponse}
     */
    @ApiOperation("删除日志")
    @DeleteMapping("/log/{id}")
    @ResponseBody
    IngotResponse removeById(@PathVariable(value = "id") Long id);

    /**
     * 插入日志
     * @param sysLog 日志实体
     * @return {@link IngotResponse}
     */
    @ApiOperation("保存日志")
    @PostMapping("/log/save")
    @ResponseBody
    IngotResponse save(@Valid @RequestBody SysLogDto sysLog);
}
