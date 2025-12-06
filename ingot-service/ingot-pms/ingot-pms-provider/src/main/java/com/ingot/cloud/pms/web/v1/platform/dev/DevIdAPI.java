package com.ingot.cloud.pms.web.v1.platform.dev;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.BizLeafAlloc;
import com.ingot.cloud.pms.service.domain.BizLeafAllocService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : AdminIdAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/10/24.</p>
 * <p>Time         : 4:06 PM.</p>
 */
@Slf4j
@RestController
@Tag(description = "DevID", name = "业务ID管理模块")
@RequestMapping(value = "/v1/platform/dev/id")
@RequiredArgsConstructor
public class DevIdAPI implements RShortcuts {
    private final BizLeafAllocService bizLeafAllocService;

    @Operation(summary = "分页查询", description = "分页查询")
    @AdminOrHasAnyAuthority({"platform:develop:id:query"})
    @GetMapping("/page")
    public R<?> page(Page<BizLeafAlloc> page, BizLeafAlloc condition) {
        return ok(bizLeafAllocService.page(page, Wrappers.lambdaQuery(condition)));
    }

    @Operation(summary = "创建ID", description = "创建ID")
    @AdminOrHasAnyAuthority({"platform:develop:id:create"})
    @PostMapping
    public R<?> create(@RequestBody BizLeafAlloc params) {
        params.setUpdateTime(DateUtil.now());
        bizLeafAllocService.save(params);
        return ok();
    }

    @Operation(summary = "更新ID", description = "更新ID")
    @AdminOrHasAnyAuthority({"platform:develop:id:update"})
    @PutMapping
    public R<?> update(@RequestBody BizLeafAlloc params) {
        params.setUpdateTime(DateUtil.now());
        bizLeafAllocService.updateById(params);
        return ok();
    }

    @Operation(summary = "删除ID", description = "删除ID")
    @AdminOrHasAnyAuthority({"platform:develop:id:delete"})
    @DeleteMapping("/{id}")
    public R<?> remove(@PathVariable String id) {
        bizLeafAllocService.removeById(id);
        return ok();
    }
}
