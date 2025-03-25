package com.ingot.cloud.pms.web.v1.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.BizLeafAlloc;
import com.ingot.cloud.pms.service.domain.BizLeafAllocService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.security.access.HasAnyAuthority;
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
@Tag(description = "id", name = "ID管理模块")
@RequestMapping(value = "/v1/admin/id")
@RequiredArgsConstructor
public class AdminIdAPI implements RShortcuts {
    private final BizLeafAllocService bizLeafAllocService;

    @HasAnyAuthority({"develop:id:w", "develop:id:r"})
    @GetMapping("/page")
    public R<?> page(Page<BizLeafAlloc> page, BizLeafAlloc condition) {
        return ok(bizLeafAllocService.page(page, Wrappers.lambdaQuery(condition)));
    }

    @HasAnyAuthority({"develop:id:w"})
    @PostMapping
    public R<?> create(@RequestBody BizLeafAlloc params) {
        params.setUpdateTime(DateUtils.now());
        bizLeafAllocService.save(params);
        return ok();
    }

    @HasAnyAuthority({"develop:id:w"})
    @PutMapping
    public R<?> update(@RequestBody BizLeafAlloc params) {
        params.setUpdateTime(DateUtils.now());
        bizLeafAllocService.updateById(params);
        return ok();
    }

    @HasAnyAuthority({"develop:id:w"})
    @DeleteMapping("/{id}")
    public R<?> remove(@PathVariable String id) {
        bizLeafAllocService.removeById(id);
        return ok();
    }
}
