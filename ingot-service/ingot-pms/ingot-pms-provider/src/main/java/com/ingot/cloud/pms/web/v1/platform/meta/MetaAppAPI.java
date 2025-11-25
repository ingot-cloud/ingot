package com.ingot.cloud.pms.web.v1.platform.meta;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.service.biz.BizMetaAppService;
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
 * <p>Description  : MetaAppAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/15.</p>
 * <p>Time         : 17:17.</p>
 */
@RestController
@Tag(description = "MetaApp", name = "元数据菜单管理模块")
@RequestMapping(value = "/v1/platform/meta/app")
@RequiredArgsConstructor
public class MetaAppAPI implements RShortcuts {
    private final BizMetaAppService metaAppService;

    @AdminOrHasAnyAuthority({"meta:app:query"})
    @GetMapping("/page")
    @Operation(summary = "应用分页", description = "应用分页数据")
    public R<IPage<MetaApp>> page(Page<MetaApp> page, MetaApp condition) {
        return ok(metaAppService.conditionPage(page, condition));
    }

    @AdminOrHasAnyAuthority({"meta:app:create"})
    @PostMapping
    @Operation(summary = "创建应用", description = "创建应用")
    public R<?> create(@Validated(Group.Create.class) @RequestBody MetaApp params) {
        metaAppService.create(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"meta:app:update"})
    @PutMapping
    @Operation(summary = "更新应用", description = "更新应用")
    public R<?> update(@Validated(Group.Update.class) @RequestBody MetaApp params) {
        metaAppService.update(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"meta:app:delete"})
    @DeleteMapping("/{id}")
    @Operation(summary = "删除应用", description = "删除应用")
    public R<?> removeById(@PathVariable Long id) {
        metaAppService.delete(id);
        return ok();
    }
}
