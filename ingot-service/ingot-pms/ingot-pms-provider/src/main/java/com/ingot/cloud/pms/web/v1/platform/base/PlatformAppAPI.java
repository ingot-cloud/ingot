package com.ingot.cloud.pms.web.v1.platform.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.service.biz.BizPlatformAppService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : PlatformAppAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/15.</p>
 * <p>Time         : 17:17.</p>
 */
@RestController
@Tag(description = "PlatformApp", name = "平台APP管理模块")
@RequestMapping(value = "/v1/platform/base/app")
@RequiredArgsConstructor
public class PlatformAppAPI implements RShortcuts {
    private final BizPlatformAppService platformAppService;

    @AdminOrHasAnyAuthority({"platform:base:app:query"})
    @GetMapping("/page")
    @Operation(summary = "应用分页", description = "应用分页数据")
    public R<IPage<PlatformApp>> page(Page<PlatformApp> page, PlatformApp condition) {
        return ok(platformAppService.conditionPage(page, condition));
    }

    @AdminOrHasAnyAuthority({"platform:base:app:create"})
    @PostMapping
    @Operation(summary = "创建应用", description = "创建应用")
    public R<?> create(@Validated(Group.Create.class) @RequestBody PlatformApp params) {
        platformAppService.create(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:base:app:update"})
    @PutMapping
    @Operation(summary = "更新应用", description = "更新应用")
    public R<?> update(@Validated(Group.Update.class) @RequestBody PlatformApp params) {
        platformAppService.update(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:base:app:delete"})
    @DeleteMapping("/{id}")
    @Operation(summary = "删除应用", description = "删除应用")
    public R<?> removeById(@PathVariable Long id) {
        platformAppService.delete(id);
        return ok();
    }
}
