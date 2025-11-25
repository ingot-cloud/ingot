package com.ingot.cloud.pms.web.v1.platform.org;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.dto.app.AppEnabledDTO;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.service.biz.BizOrgService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.tenant.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : TenantApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:13 下午.</p>
 */
@Slf4j
@RestController
@Tag(description = "Tenant", name = "系统组织管理模块")
@RequestMapping(value = "/v1/platform/org/tenant")
@RequiredArgsConstructor
public class AdminTenantAPI implements RShortcuts {
    private final BizOrgService bizOrgService;

    @Operation(summary = "组织列表", description = "组织列表")
    @AdminOrHasAnyAuthority({"basic:tenant:search"})
    @GetMapping("/search")
    public R<List<SysTenant>> search(SysTenant filter) {
        return ok(bizOrgService.search(filter));
    }

    @Operation(summary = "组织详情", description = "组织详情")
    @AdminOrHasAnyAuthority({"basic:tenant:detail"})
    @GetMapping("/{id}")
    public R<SysTenant> getTenantInfo(@PathVariable Long id) {
        return ok(bizOrgService.getDetails(id));
    }

    @Operation(summary = "组织列表", description = "组织列表")
    @AdminOrHasAnyAuthority({"basic:tenant:query"})
    @GetMapping("/page")
    public R<IPage<SysTenant>> page(Page<SysTenant> page, SysTenant params) {
        return ok(bizOrgService.conditionPage(page, params));
    }

    @Operation(summary = "组织应用列表", description = "组织应用列表")
    @AdminOrHasAnyAuthority({"basic:tenant:app:query"})
    @GetMapping("/apps")
    public R<List<MetaApp>> getApps() {
        return ok(bizOrgService.getOrgApps(TenantContextHolder.get()));
    }

    @Operation(summary = "更新组织应用状态", description = "更新组织应用状态")
    @AdminOrHasAnyAuthority({"basic:tenant:app:update"})
    @PutMapping("/app/status")
    public R<Void> updateAppStatus(@RequestBody AppEnabledDTO params) {
        bizOrgService.updateOrgAppStatus(params);
        return ok();
    }

    @Operation(summary = "创建组织", description = "创建组织")
    @AdminOrHasAnyAuthority({"basic:tenant:create"})
    @PostMapping
    public R<Void> create(@Valid @RequestBody CreateOrgDTO params) {
        bizOrgService.createOrg(params);
        return ok();
    }

    @Operation(summary = "更新组织", description = "更新组织")
    @AdminOrHasAnyAuthority({"basic:tenant:update"})
    @PutMapping
    public R<Void> update(@Valid @RequestBody SysTenant params) {
        bizOrgService.updateBase(params);
        return ok();
    }

    @Operation(summary = "删除组织", description = "删除组织")
    @AdminOrHasAnyAuthority({"basic:tenant:delete"})
    @DeleteMapping("/{id}")
    public R<Void> removeById(@PathVariable Long id) {
        bizOrgService.removeOrg(id);
        return ok();
    }
}
