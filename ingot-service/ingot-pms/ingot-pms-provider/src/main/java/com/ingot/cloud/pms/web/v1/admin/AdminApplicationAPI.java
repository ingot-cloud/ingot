package com.ingot.cloud.pms.web.v1.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysApplication;
import com.ingot.cloud.pms.api.model.domain.SysApplicationTenant;
import com.ingot.cloud.pms.api.model.dto.application.ApplicationFilterDTO;
import com.ingot.cloud.pms.service.biz.BizApplicationService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.security.access.HasAnyAuthority;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : AdminApplicationAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/26.</p>
 * <p>Time         : 11:13.</p>
 */
@Slf4j
@Tag(description = "application", name = "系统应用管理模块")
@RestController
@RequestMapping(value = "/v1/admin/application")
@RequiredArgsConstructor
public class AdminApplicationAPI implements RShortcuts {
    private final BizApplicationService bizApplicationService;

    @GetMapping("/page")
    @HasAnyAuthority({"orgm:application"})
    public R<?> page(Page<SysApplication> page, ApplicationFilterDTO filter) {
        return ok(bizApplicationService.page(page, filter));
    }

    @GetMapping("/page/{orgId}")
    @HasAnyAuthority({"orgm:application"})
    public R<?> orgApp(@PathVariable Long orgId) {
        return ok(bizApplicationService.orgApplicationList(orgId));
    }

    @PutMapping("/sync/{id}")
    @HasAnyAuthority({"orgm:application"})
    public R<?> syncApp(@PathVariable Long id) {
        bizApplicationService.syncApplication(id);
        return ok();
    }

    @PostMapping
    @HasAnyAuthority({"orgm:application"})
    public R<?> createApp(@RequestBody SysApplication params) {
        bizApplicationService.createApplication(params);
        return ok();
    }

    @PutMapping("/status")
    @HasAnyAuthority({"orgm:application"})
    public R<?> updateStatus(@RequestBody SysApplication params) {
        bizApplicationService.updateApplicationStatus(params);
        return ok();
    }

    @PutMapping("/default")
    @HasAnyAuthority({"orgm:application"})
    public R<?> updateDefault(@RequestBody SysApplication params) {
        bizApplicationService.updateApplicationDefault(params);
        return ok();
    }

    @DeleteMapping("/{id}")
    @HasAnyAuthority({"orgm:application"})
    public R<?> removeApp(@PathVariable Long id) {
        bizApplicationService.removeApplication(id);
        return ok();
    }

    @PutMapping("/status/org/{orgId}")
    @HasAnyAuthority({"orgm:application"})
    public R<?> updateOrgStatus(@PathVariable Long orgId, @RequestBody SysApplicationTenant params) {
        bizApplicationService.updateStatusOfTargetOrg(orgId, params);
        return ok();
    }
}
